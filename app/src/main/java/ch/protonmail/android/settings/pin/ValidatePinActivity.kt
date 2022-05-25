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
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.pinlock.presentation.PinLockManager
import ch.protonmail.android.settings.pin.viewmodel.PinFragmentViewModel
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import ch.protonmail.android.views.ISecurePINListener
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class ValidatePinActivity :
    BaseActivity(),
    PinFragmentViewModel.IPinCreationListener,
    ISecurePINListener,
    PinFragmentViewModel.ReopenFingerprintDialogListener {

    @Inject
    lateinit var pinLockManager: PinLockManager

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun getLayoutId(): Int = R.layout.activity_fragment_container

    override fun isPreventingScreenshots(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        if (savedInstanceState != null) {
            return
        }
        val user = mUserManager.requireCurrentLegacyUser()
        val titleRes = R.string.settings_enter_pin_code_title
        val validatePinFragment =
            PinFragment.newInstance(titleRes, PinAction.VALIDATE, null, useFingerprint = user.isUseFingerprint)
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.zoom_in, 0, 0, R.anim.zoom_out)
            .add(R.id.fragmentContainer, validatePinFragment, validatePinFragment.fragmentKey)
            .commitAllowingStateLoss()

        if (user.isUseFingerprint) {
            initBiometricPrompt()
        }
    }

    override fun onPinCreated(pin: String) {
        // NOOP
    }

    override fun showCreatePin() {
        // NOOP
    }

    override fun onPinConfirmed(confirmPin: String?) {
        // NOOP
    }

    override fun onFingerprintReopen() {
        if (!::biometricPrompt.isInitialized) {
            initBiometricPrompt()
        }
        biometricPrompt.authenticate(promptInfo)
    }

    override fun onForgotPin() {
        DialogUtils.showDeleteConfirmationDialog(this, "", getString(R.string.sign_out_question)) {
            logout()
        }
    }

    override fun onPinSuccess() {
        pinLockManager.unlock()
        setResult(Activity.RESULT_OK)
        finish()
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

    private fun initBiometricPrompt() {
        val executor = Executors.newSingleThreadExecutor()
        biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (!this@ValidatePinActivity.isFinishing) {
                        biometricPrompt.cancelAuthentication()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onPinSuccess()
                }
            }
        )
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_locked))
            .setDescription(getString(R.string.log_in_using_biometric_credential))
            .setNegativeButtonText(getString(R.string.use_pin_instead))
            .build()
        biometricPrompt.authenticate(promptInfo)
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

    override fun onBackPressed() {}

}
