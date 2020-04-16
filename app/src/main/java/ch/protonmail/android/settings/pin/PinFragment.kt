/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonMail.
 * 
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.settings.pin

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.OnClick
import ch.protonmail.android.R
import ch.protonmail.android.activities.fragments.BaseFragment
import ch.protonmail.android.settings.pin.viewmodel.PinFragmentViewModel
import ch.protonmail.android.settings.pin.viewmodel.PinFragmentViewModelFactory
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.views.RoundButton
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_pin.*
import javax.inject.Inject

// region constants
private const val ARGUMENT_SUBTITLE = "extra_pin_subtitle"
private const val ARGUMENT_ACTION_TYPE = "extra_pin_action"
private const val ARGUMENT_WANTED_PIN = "extra_wanted_pin"
private const val ARGUMENT_SIGN_OUT = "extra_signout_possible"
private const val ARGUMENT_FINGERPRINT = "extra_use_fingerprint"
// endregion

/**
 * Created by dkadrikj on 3/28/16.
 */

class PinFragment : BaseFragment() {

    @Inject
    lateinit var pinFragmentViewModelFactory: PinFragmentViewModelFactory
    private lateinit var pinFragmentViewModel: PinFragmentViewModel

    @StringRes
    private var subtitleRes: Int = 0

    override fun getLayoutResourceId(): Int = R.layout.fragment_pin

    override fun getFragmentKey(): String = "ProtonMail.PinFragment"

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        pinFragmentViewModel = ViewModelProviders.of(this, pinFragmentViewModelFactory).get(PinFragmentViewModel::class.java)
        if (context is PinFragmentViewModel.IPinCreationListener) {
            pinFragmentViewModel.setListener(context)
        } else {
            throw ClassCastException("Activity must implement IPinCreationListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            subtitleRes = getInt(ARGUMENT_SUBTITLE)
            pinFragmentViewModel.setup(getSerializable(ARGUMENT_ACTION_TYPE) as PinAction, getBoolean(ARGUMENT_SIGN_OUT),
                    getString(ARGUMENT_WANTED_PIN), getBoolean(ARGUMENT_FINGERPRINT))
        }
        val context = context
        if (context is PinFragmentViewModel.ReopenFingerprintDialogListener) {
            pinFragmentViewModel.setListener(context)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (subtitleRes != 0) {
            mPinEdit.setTitle(getString(subtitleRes))
        }
        pinFragmentViewModel.setupDoneObservable.observe(this, setupObserver)
    }

    private val forwardObserver = Observer<PinFragmentViewModel.ValidationResult> {
        if (it.actionType == PinAction.CREATE && !it.valid) {
            context?.showToast(R.string.pin_invalid)
        } else if (it.actionType == PinAction.CONFIRM && !it.valid) {
            context?.showToast(R.string.pin_confirmation_not_match)
        }
    }

    private val setupObserver = Observer<PinFragmentViewModel.PinSetup> {
        mPinEdit.setActionType(it.actionType)
        mBtnForward.visibility = View.INVISIBLE
        when (it.actionType) {
            PinAction.CREATE -> {
                mBtnForward.visibility = View.VISIBLE
                mBtnForward.text = getString(R.string.create)
            }
            PinAction.CONFIRM -> {
                mBtnForward.visibility = View.VISIBLE
                mBtnForward.text = getString(R.string.confirm)
                mBtnBackward.visibility = View.VISIBLE
            }
            else -> {
                mForgotPin.visibility = if (it.signOutPossible) View.VISIBLE else View.GONE
                if (it.useFingerprint) {
                    openFingerprintPrompt.visibility = View.VISIBLE
                }
            }
        }
    }

    // region click listeners
    @OnClick(R.id.mBtnForward)
    fun onNextClicked() {
        pinFragmentViewModel.nextClicked(mPinEdit.pin, mPinEdit.isValid, mPinEdit.isValid(pinFragmentViewModel.wantedPin()))
                .observe(this, forwardObserver)
    }

    @OnClick(R.id.btn_pin_0, R.id.btn_pin_1, R.id.btn_pin_2, R.id.btn_pin_3, R.id.btn_pin_4, R.id.btn_pin_5, R.id.btn_pin_6, R.id.btn_pin_7, R.id.btn_pin_8, R.id.btn_pin_9)
    fun onKeyClicked(button: RoundButton) {
        val keyValue = button.keyValue
        mPinEdit.enterKey(keyValue)
    }

    @OnClick(R.id.mBtnBackward)
    fun onBackClicked() {
        pinFragmentViewModel.onBackClicked()
    }

    @OnClick(R.id.openFingerprintPrompt)
    fun onFingerprintClick() {
        pinFragmentViewModel.onFingerprintReopen()
    }

    @OnClick(R.id.mForgotPin)
    fun onForgotPinClicked() {
        pinFragmentViewModel.onForgotPin()
    }
    // endregion

    companion object {

        @JvmOverloads
        fun newInstance(@StringRes subtitleRes: Int, actionType: PinAction, wantedPin: String?, signOutPossible: Boolean = true, useFingerprint: Boolean): PinFragment {
            val fragment = PinFragment()
            val extras = Bundle()
            extras.putInt(ARGUMENT_SUBTITLE, subtitleRes)
            extras.putSerializable(ARGUMENT_ACTION_TYPE, actionType)
            extras.putString(ARGUMENT_WANTED_PIN, wantedPin)
            extras.putBoolean(ARGUMENT_SIGN_OUT, signOutPossible)
            extras.putBoolean(ARGUMENT_FINGERPRINT, useFingerprint)
            fragment.arguments = extras
            return fragment
        }
    }
}
