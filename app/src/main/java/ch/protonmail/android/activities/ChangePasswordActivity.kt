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
package ch.protonmail.android.activities

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.ToggleButton
import butterknife.OnClick
import ch.protonmail.android.R
import ch.protonmail.android.api.models.User
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.AuthStatus
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.PasswordChangeEvent
import ch.protonmail.android.jobs.ChangePasswordJob
import ch.protonmail.android.jobs.user.FetchUserSettingsJob
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.moveToLogin
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.activity_change_password.*

class ChangePasswordActivity : BaseActivity() {

    private var user: User? = null
    private var passwordType: Int = 0
    private var hasTwoFactor: Boolean = false
    private var singlePassMode: Boolean = false

    private var currentPassword: ByteArray? = null
    private var currentMailboxLoginPassword: ByteArray? = null

    override fun isPreventingScreenshots(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        user = mUserManager.user

        if (user != null) {
            hasTwoFactor = mUserManager.userSettings?.twoFactor == 1
            singlePassMode = mUserManager.userSettings?.passwordMode == Constants.PasswordMode.SINGLE
        }

        if (singlePassMode) {
            mailboxPasswordContainer.visibility = View.GONE
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

    override fun getLayoutId(): Int = R.layout.activity_change_password

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            saveLastInteraction()
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @OnClick(R.id.save)
    fun onSaveClicked() {
        if (
            !TextUtils.isEmpty(currentPasswordEditText.text.toString()) &&
            !TextUtils.isEmpty(newPassword.text.toString()) &&
            !TextUtils.isEmpty(newPasswordConfirm.text.toString())
        ) {
            progressContainer.visibility = View.VISIBLE
            currentPassword = currentPasswordEditText.text.toString().toByteArray() /*TODO passphrase*/
            if (hasTwoFactor) {
                passwordType = Constants.PASSWORD_TYPE_LOGIN
                showTwoFactorDialog()
            } else {
                checkAndExecutePasswordChange(null)
            }
        }
    }

    @OnClick(R.id.mailbox_save)
    fun onMailboxSaveClicked() {
        progressContainer.visibility = View.VISIBLE
        currentMailboxLoginPassword = mailboxLoginPassword.text.toString().toByteArray() /*TODO passphrase*/
        passwordType = Constants.PASSWORD_TYPE_MAILBOX
        if (hasTwoFactor) {
            showTwoFactorDialog()
        } else {
            checkAndExecuteMailboxPasswordChange(null)
        }
    }

    @Subscribe
    fun onLogoutEvent(event: LogoutEvent?) {
        moveToLogin()
    }

    @Subscribe
    fun onPasswordChangeEvent(event: PasswordChangeEvent) {
        progressContainer.visibility = View.GONE
        if (event.status == AuthStatus.SUCCESS) {
            val username = mUserManager.username
            mJobManager.addJobInBackground(FetchUserSettingsJob(username))
            saveLastInteraction()
            finish()
        }
    }

    private fun checkAndExecutePasswordChange(twoFactorCode: String?) {
        val newPassString = newPassword.text.toString()
        val newPassConfirmString = newPasswordConfirm.text.toString()
        val incompleteInput = currentPassword?.isEmpty() == true || newPassString.isEmpty() ||
            newPassConfirmString.isEmpty()
        val incompleteTwoFactorCode = hasTwoFactor && twoFactorCode.isNullOrEmpty()

        if (incompleteInput || incompleteTwoFactorCode) {
            showToast(R.string.new_passwords_not_valid, Toast.LENGTH_SHORT)
            return
        }
        if (newPassString != newPassConfirmString) {
            showToast(R.string.new_passwords_do_not_match, Toast.LENGTH_SHORT)
            progressContainer.visibility = View.GONE
        } else {
            val job = ChangePasswordJob(
                passwordType,
                mUserManager.userSettings!!.passwordMode,
                currentPassword,
                twoFactorCode,
                newPassString.toByteArray() /*TODO passphrase*/
            )
            mJobManager.addJobInBackground(job)
        }
    }

    private fun checkAndExecuteMailboxPasswordChange(twoFactorCode: String?) {
        val newPassString = mailboxNewPassword.text.toString()
        val newPassConfirmString = mailboxNewPasswordConfirm.text.toString()
        val incompleteInput = currentPassword?.isEmpty() == true || newPassString.isEmpty() ||
            newPassConfirmString.isEmpty()
        val incompleteTwoFactorCode = hasTwoFactor && twoFactorCode.isNullOrEmpty()
        if (incompleteInput || incompleteTwoFactorCode) {
            showToast(R.string.new_passwords_not_valid, Toast.LENGTH_SHORT)
            return
        }
        if (newPassString != newPassConfirmString) {
            showToast(R.string.new_passwords_do_not_match, Toast.LENGTH_SHORT)
            progressContainer.visibility = View.GONE
        } else {
            if (passwordType == Constants.PASSWORD_TYPE_MAILBOX) {
                val job = ChangePasswordJob(
                    passwordType,
                    Constants.PasswordMode.DUAL,
                    currentMailboxLoginPassword,
                    twoFactorCode,
                    newPassString.toByteArray() /*TODO passphrase*/
                )
                mJobManager.addJobInBackground(job)
            }
        }
    }

    private fun showTwoFactorDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.layout_2fa, null)
        val twoFactorCode = dialogView.findViewById<EditText>(R.id.two_factor_code)
        val toggleInputText = dialogView.findViewById<ToggleButton>(R.id.toggle_input_text)
        toggleInputText.setOnClickListener { v ->
            if ((v as ToggleButton).isChecked) {
                twoFactorCode.inputType = InputType.TYPE_CLASS_TEXT
            } else {
                twoFactorCode.inputType = InputType.TYPE_CLASS_NUMBER
            }
        }
        builder.setView(dialogView)
        builder.setPositiveButton(R.string.enter) { dialog, _ ->
            UiUtil.hideKeyboard(this@ChangePasswordActivity, twoFactorCode)
            val twoFactorString = twoFactorCode.text.toString()
            if (!TextUtils.isEmpty(twoFactorString)) {
                if (passwordType == Constants.PASSWORD_TYPE_LOGIN) {
                    checkAndExecutePasswordChange(twoFactorString)
                } else {
                    checkAndExecuteMailboxPasswordChange(twoFactorString)
                }
            }
            dialog.cancel()
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            progressContainer.visibility = View.GONE
            dialog.cancel()
        }
        val alert = builder.create()
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.iron_gray))
            val positiveButton = alert.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setTextColor(resources.getColor(R.color.new_purple_dark))
            positiveButton.text = getString(R.string.enter)
        }
        alert.setCanceledOnTouchOutside(false)
        if (!isFinishing) {
            alert.show()
        }
    }

    @OnClick(R.id.toggleViewCurrentPassword)
    fun onShowCurrentPassword(showPassword: ToggleButton) {
        currentPasswordEditText.setVisibilityMode(showPassword.isChecked)
    }

    @OnClick(R.id.toggleViewNewPassword)
    fun onShowNewPassword(showPassword: ToggleButton) {
        newPassword.setVisibilityMode(showPassword.isChecked)
    }

    @OnClick(R.id.toggleViewNewPasswordConfirm)
    fun onShowNewConfirmPassword(showPassword: ToggleButton) {
        newPasswordConfirm.setVisibilityMode(showPassword.isChecked)
    }

    @OnClick(R.id.toggleViewMailboxLoginPassword)
    fun onShowMailboxCurrentLoginPassword(showPassword: ToggleButton) {
        mailboxLoginPassword.setVisibilityMode(showPassword.isChecked)
    }

    @OnClick(R.id.toggleViewMailboxNewPassword)
    fun onShowMailboxNewPassword(showPassword: ToggleButton) {
        mailboxNewPassword.setVisibilityMode(showPassword.isChecked)
    }

    @OnClick(R.id.toggleViewMailboxNewConfirmPassword)
    fun onShowMailboxNewConfirmPassword(showPassword: ToggleButton) {
        mailboxNewPasswordConfirm.setVisibilityMode(showPassword.isChecked)
    }
}
