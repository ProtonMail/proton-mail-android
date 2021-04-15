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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.settings.pin.viewmodel.PinFragmentViewModel
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.views.SecureEditText

// region constants
const val EXTRA_NEW_PIN_SET = "extra_new_pin_set"
const val EXTRA_NEW_PIN = "extra_pin"
// endregion

/*
 * Created by dkadrikj on 3/27/16.
 */

class ChangePinActivity : BaseActivity(),
    PinFragmentViewModel.IPinCreationListener,
    SecureEditText.ISecurePINListener {

    private val fragmentContainer by lazy { findViewById<ViewGroup>(R.id.fragmentContainer) }

    override fun shouldCheckForAutoLogout(): Boolean = false

    override fun getLayoutId(): Int = R.layout.activity_fragment_container

    override fun isPreventingScreenshots(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        if (fragmentContainer != null) {
            if (savedInstanceState != null) {
                return
            }
            val validatePinFragment = PinFragment.newInstance(R.string.enter_current_pin, PinAction.VALIDATE, null, false, false)
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, validatePinFragment, validatePinFragment.fragmentKey)
                    .commitAllowingStateLoss()
        }
    }

    override fun onPinCreated(pin: String) {
        val confirmPinFragment = PinFragment.newInstance(R.string.pin_subtitle_confirm_new, PinAction.CONFIRM, pin, false, false)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, confirmPinFragment)
        fragmentTransaction.addToBackStack(confirmPinFragment.fragmentKey)
        fragmentTransaction.commitAllowingStateLoss()
    }

    override fun showCreatePin() {
        supportFragmentManager.popBackStack()
        val validatePinFragment = PinFragment.newInstance(R.string.enter_current_pin, PinAction.VALIDATE, null, false, false)
        supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, validatePinFragment, validatePinFragment.fragmentKey)
                .commitAllowingStateLoss()
    }

    override fun onPinConfirmed(confirmPin: String?) {
        val intent = Intent()
        intent.putExtra(EXTRA_NEW_PIN_SET, true)
        intent.putExtra(EXTRA_NEW_PIN, confirmPin)
        setResult(RESULT_OK, intent)
        saveLastInteraction()
        finish()
    }

    override fun onForgotPin() {
        // noop
    }

    override fun onPinSuccess() {
        val createPinFragment = PinFragment.newInstance(R.string.pin_subtitle_create_new, PinAction.CREATE, null, false, false)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, createPinFragment)
        fragmentTransaction.addToBackStack(createPinFragment.fragmentKey)
        fragmentTransaction.commitAllowingStateLoss()
    }

    private fun logout() {
        mUserManager.requireCurrentLegacyUserBlocking().apply {
            isUsePin = false
            isUseFingerprint = false
            save()
        }
        mUserManager.apply {
            savePin("")
            resetPinAttempts()
        }
        accountViewModel.logout(mUserManager.requireCurrentUserId()).invokeOnCompletion {
            val intent = Intent()
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onPinError() {
        if (mUserManager.incorrectPinAttempts >= Constants.MAX_INCORRECT_PIN_ATTEMPTS) {
            logout()
        }
        showToast(R.string.pin_not_match, Toast.LENGTH_SHORT)
    }

    override fun onPinMaxDigitReached() {
        // noop
    }
}
