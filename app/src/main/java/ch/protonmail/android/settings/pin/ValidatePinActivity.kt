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
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.FetchDraftDetailEvent
import ch.protonmail.android.events.FetchMessageDetailEvent
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.MessageCountsEvent
import ch.protonmail.android.events.PostImportAttachmentEvent
import ch.protonmail.android.events.user.MailSettingsEvent
import ch.protonmail.android.settings.pin.viewmodel.PinFragmentViewModel
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.moveToLogin
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import ch.protonmail.android.views.SecureEditText
import com.squareup.otto.Subscribe
import java.util.concurrent.Executors

// region constants
const val EXTRA_PIN_VALID = "extra_pin_valid"
const val EXTRA_LOGOUT = "extra_logout"
const val EXTRA_FRAGMENT_TITLE = "extra_title"
const val EXTRA_ATTACHMENT_IMPORT_EVENT = "extra_attachment_import_event"
const val EXTRA_TOTAL_COUNT_EVENT = "extra_total_count_event"
const val EXTRA_MESSAGE_DETAIL_EVENT = "extra_message_details_event"
const val EXTRA_DRAFT_DETAILS_EVENT = "extra_draft_details_event"
// endregion

/*
 * Created by dkadrikj on 3/27/16.
 */

class ValidatePinActivity : BaseActivity(),
    PinFragmentViewModel.IPinCreationListener,
    SecureEditText.ISecurePINListener,
    PinFragmentViewModel.ReopenFingerprintDialogListener {

    private var importAttachmentEvent: PostImportAttachmentEvent? = null
    private var messageCountsEvent: MessageCountsEvent? = null
    private var messageDetailEvent: FetchMessageDetailEvent? = null
    private var draftDetailEvent: FetchDraftDetailEvent? = null
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun shouldCheckForAutoLogout(): Boolean = false

    override fun getLayoutId(): Int = R.layout.activity_fragment_container

    override fun isPreventingScreenshots(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        if (savedInstanceState != null) {
            return
        }
        val titleRes = intent.getIntExtra(EXTRA_FRAGMENT_TITLE, 0)
        val validatePinFragment = PinFragment.newInstance(titleRes, PinAction.VALIDATE,
                null, useFingerprint =  mUserManager.user.isUseFingerprint)
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, validatePinFragment, validatePinFragment.fragmentKey).commitAllowingStateLoss()

        if (mUserManager.user.isUseFingerprint) {
            initBiometricPrompt()
        }
    }

    override fun onStart() {
        super.onStart()
        ProtonMailApplication.getApplication().bus.register(this)
    }

    override fun onStop() {
        super.onStop()
        ProtonMailApplication.getApplication().bus.unregister(this)
    }

    // region subscription events
    @Subscribe
    fun onPostImportAttachmentEvent(event: PostImportAttachmentEvent) {
        importAttachmentEvent = event
    }

    @Subscribe
    fun onMessageCountsEvent(event: MessageCountsEvent) {
        messageCountsEvent = event
    }

    @Subscribe
    fun onFetchMessageDetailEvent(event: FetchMessageDetailEvent) {
        messageDetailEvent = event
    }

    @Subscribe
    fun onFetchDraftDetailEvent(event: FetchDraftDetailEvent) {
        draftDetailEvent = event
    }
    // endregion

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
        mUserManager.user.setManuallyLocked(false)
        mPinValid = true
        setResult(Activity.RESULT_OK, buildIntent())
        saveLastInteraction()
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

    @Subscribe
    fun onLogoutEvent(event: LogoutEvent) {
        moveToLogin()
    }

    private fun initBiometricPrompt() {
        val executor = Executors.newSingleThreadExecutor()
        biometricPrompt = BiometricPrompt(this, executor,
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

                })
        promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_locked))
                .setDescription(getString(R.string.log_in_using_biometric_credential))
                .setNegativeButtonText(getString(R.string.use_pin_instead))
                .build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun logout() {
        mUserManager.user.apply {
            isUsePin = false
            isUseFingerprint = false
            save()
        }
        mUserManager.apply {
            savePin("")
            resetPinAttempts()
            this.logoutAccount(username)
        }
        val intent = Intent()
        intent.putExtra(EXTRA_LOGOUT, true)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onBackPressed() {
        if (!AppUtil.isLockTaskModeRunning(this)) {
            setResult(Activity.RESULT_CANCELED, Intent().apply { putExtra(EXTRA_PIN_VALID, false) })
            finish()
        }
    }

    private fun buildIntent() : Intent {
        return Intent().apply {
            putExtra(EXTRA_PIN_VALID, true)
            if (importAttachmentEvent != null) {
                putExtra(EXTRA_ATTACHMENT_IMPORT_EVENT, importAttachmentEvent)
            }
            if (messageCountsEvent != null) {
                putExtra(EXTRA_TOTAL_COUNT_EVENT, messageCountsEvent)
            }
            if (messageDetailEvent != null) {
                putExtra(EXTRA_MESSAGE_DETAIL_EVENT, messageDetailEvent)
            }
        }
    }

    @Subscribe
    fun onMailSettingsEvent(event: MailSettingsEvent) {
        loadMailSettings()
    }
}
