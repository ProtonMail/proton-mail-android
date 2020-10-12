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
package ch.protonmail.android.activities.multiuser

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.view.Gravity
import android.widget.EditText
import android.widget.ToggleButton
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseConnectivityActivity
import ch.protonmail.android.core.LOGIN_STATE_TO_INBOX
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.AuthStatus
import ch.protonmail.android.events.ConnectAccountMailboxLoginEvent
import ch.protonmail.android.fcm.FcmUtil
import ch.protonmail.android.utils.extensions.setBarColors
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.moveToMailbox
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import kotlinx.android.synthetic.main.toolbar_white.*

// region constants
const val EXTRA_USERNAME = "connect_account_username"
// endregion

/**
 * The base activity for handling connecting an account.
 */
abstract class ConnectAccountBaseActivity : BaseConnectivityActivity() {

    protected abstract val togglePasswordView: ToggleButton
    protected abstract val passwordEditView: EditText

    abstract fun unsubscribeFromEvents()
    abstract fun resetState()
    abstract fun removeAccount(username: String)

    private var eventsUnregistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbar.setNavigationIcon(R.drawable.ic_close)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        window?.setBarColors(resources.getColor(R.color.new_purple_dark))

        togglePasswordView.setOnClickListener { onTogglePasswordClick(it as ToggleButton) }
    }

    override fun onStop() {
        super.onStop()
        if (!eventsUnregistered) {
            unsubscribeFromEvents()
        }
    }

    private fun onTogglePasswordClick(togglePasswordView: ToggleButton) {
        if (togglePasswordView.isChecked) {
            passwordEditView.transformationMethod = SingleLineTransformationMethod.getInstance()
        } else {
            passwordEditView.transformationMethod = PasswordTransformationMethod.getInstance()
        }
        passwordEditView.setSelection(passwordEditView.text.length)
    }

    open fun onConnectAccountMailboxLoginEvent(event: ConnectAccountMailboxLoginEvent) {
        ProtonMailApplication.getApplication().resetMailboxLoginEvent()
        when (event.status) {
            AuthStatus.SUCCESS -> {
                eventsUnregistered = true
                ProtonMailApplication.getApplication().bus.unregister(this)
                FcmUtil.setTokenSent(mUserManager.username, false) // force FCM to register new user
                mUserManager.loginState = LOGIN_STATE_TO_INBOX
                moveToMailbox()
                saveLastInteraction()
                finish()
            }
            AuthStatus.NO_NETWORK -> {
                resetState()
                showToast(R.string.no_network, Gravity.CENTER)
            }
            AuthStatus.UPDATE -> {
                resetState()
                showToast(R.string.update_app)
            }
            AuthStatus.INVALID_CREDENTIAL -> {
                resetState()
                showToast(R.string.invalid_mailbox_password, Gravity.CENTER)
            }
            AuthStatus.INCORRECT_KEY_PARAMETERS -> {
                resetState()
                showToast(R.string.incorrect_key_parameters, Gravity.CENTER)
            }
            AuthStatus.NOT_SIGNED_UP -> {
                resetState()
                showToast(R.string.not_signed_up, Gravity.CENTER)
            }
            AuthStatus.CANT_CONNECT -> {
                resetState()
                DialogUtils.showInfoDialog(this@ConnectAccountBaseActivity,
                        getString(R.string.connect_account_limit_title),
                        getString(R.string.connect_account_limit_subtitle)) {
                    saveLastInteraction()
                    moveToMailbox()
                    finish()
                }
            }
            else -> {
                resetState()
                showToast(R.string.mailbox_login_failure, Gravity.CENTER)
            }
        }
    }

    override fun isPreventingScreenshots(): Boolean = true
}
