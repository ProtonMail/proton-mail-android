/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.settings.pin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.settings.pin.viewmodel.PinFragmentViewModel
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.views.ISecurePINListener

// region constants
const val EXTRA_NEW_PIN_SET = "extra_new_pin_set"
const val EXTRA_NEW_PIN = "extra_pin"
// endregion

class ChangePinActivity :
    BaseActivity(),
    PinFragmentViewModel.IPinCreationListener,
    ISecurePINListener {

    private val fragmentContainer by lazy { findViewById<ViewGroup>(R.id.fragmentContainer) }

    override fun getLayoutId(): Int = R.layout.activity_fragment_container

    override fun isPreventingScreenshots(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        if (fragmentContainer != null) {
            if (savedInstanceState != null) {
                return
            }
            val validatePinFragment =
                PinFragment.newInstance(R.string.settings_enter_pin_code_title, PinAction.VALIDATE, null, false, false)
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.zoom_in, 0, 0, R.anim.zoom_out)
                .add(R.id.fragmentContainer, validatePinFragment, validatePinFragment.fragmentKey)
                .commitAllowingStateLoss()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onPinCreated(pin: String) {
        val confirmPinFragment =
            PinFragment.newInstance(R.string.settings_confirm_pin_code_title, PinAction.CONFIRM, pin, false, false)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, confirmPinFragment)
        fragmentTransaction.addToBackStack(confirmPinFragment.fragmentKey)
        fragmentTransaction.commitAllowingStateLoss()
    }

    override fun showCreatePin() {
        supportFragmentManager.popBackStack()
        val validatePinFragment =
            PinFragment.newInstance(R.string.settings_create_pin_code_title, PinAction.VALIDATE, null, false, false)
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, validatePinFragment, validatePinFragment.fragmentKey)
            .commitAllowingStateLoss()
    }

    override fun onPinConfirmed(confirmPin: String?) {
        val intent = Intent()
        intent.putExtra(EXTRA_NEW_PIN_SET, true)
        intent.putExtra(EXTRA_NEW_PIN, confirmPin)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onForgotPin() {
        // noop
    }

    override fun onPinSuccess() {
        val createPinFragment =
            PinFragment.newInstance(R.string.settings_create_pin_code_title, PinAction.CREATE, null, false, false)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, createPinFragment)
        fragmentTransaction.addToBackStack(createPinFragment.fragmentKey)
        fragmentTransaction.commitAllowingStateLoss()
    }

    private fun logout() {
        mUserManager.requireCurrentLegacyUser().apply {
            isUsePin = false
            isUseFingerprint = false
        }
        mUserManager.apply {
            savePin("")
            resetPinAttempts()
        }
        accountStateManager.signOutAll().invokeOnCompletion {
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
