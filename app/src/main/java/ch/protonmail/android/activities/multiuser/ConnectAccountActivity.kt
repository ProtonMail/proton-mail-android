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

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ToggleButton
import androidx.activity.viewModels
import ch.protonmail.android.R
import ch.protonmail.android.activities.multiuser.viewModel.ConnectAccountViewModel
import ch.protonmail.android.api.models.LoginInfoResponse
import ch.protonmail.android.api.models.LoginResponse
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.core.LOGIN_STATE_LOGIN_FINISHED
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.events.AuthStatus
import ch.protonmail.android.events.ConnectAccountLoginEvent
import ch.protonmail.android.events.ConnectAccountMailboxLoginEvent
import ch.protonmail.android.events.ForceUpgradeEvent
import ch.protonmail.android.events.Login2FAEvent
import ch.protonmail.android.events.LoginInfoEvent
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.extensions.getColorCompat
import ch.protonmail.android.utils.extensions.removeWhitespaces
import ch.protonmail.android.utils.extensions.setcolor
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.moveToMailbox
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showInfoDialog
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_connect_account.*
import kotlinx.android.synthetic.main.connect_account_progress.*

/**
 * This activity handles the first step towards connecting an account,
 * where username and password should be provided.
 */
@AndroidEntryPoint
class ConnectAccountActivity : ConnectAccountBaseActivity() {

    override val togglePasswordView: ToggleButton
        get() = toggleViewPassword
    override val passwordEditView: EditText
        get() = password

    private var disableBack = false
    private var twoFactorDialog: AlertDialog? = null

    private val viewModel: ConnectAccountViewModel by viewModels()

    override fun removeAccount(username: String) {
        viewModel.removeAccount(username)
    }

    override fun resetState() {
        // noop
    }

    override fun getLayoutId(): Int = R.layout.activity_connect_account

    override fun unsubscribeFromEvents() {
        app.bus.unregister(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AlarmReceiver().cancelAlarm(this)
        val accountUserName = intent?.extras?.getString(EXTRA_USERNAME, null)
        accountUserName?.also {
            username.setText(it)
            username.isEnabled = false
        }

        forgotPassword.setOnClickListener { onForgotPasswordClicked() }
        connect.setOnClickListener { onConnectClicked() }

        progressCircular.setcolor(getColorCompat(R.color.new_purple_dark))
    }

    override fun onStart() {
        super.onStart()
        app.bus.register(this)
    }

    @Subscribe
    fun onLogin2FAEvent(event: Login2FAEvent) {
        app.resetLogin2FAEvent()
        hideProgress()
        enableInput(true)
        showTwoFactorDialog(
            event.userId,
            event.password,
            event.infoResponse,
            event.loginResponse,
            event.fallbackAuthVersion
        )
    }

    @Subscribe
    fun onLoginInfoEvent(event: LoginInfoEvent) {
        when (event.status) {
            AuthStatus.SUCCESS -> {
                app.resetLoginInfoEvent()
                viewModel.login(
                    username = event.username,
                    password = event.password,
                    response = event.response,
                    fallbackAuthVersion = event.fallbackAuthVersion
                )
            }
            AuthStatus.NO_NETWORK -> {
                showToast(R.string.no_network)
            }
            AuthStatus.FAILED -> {
                hideProgress()
                enableInput(true)
                showToast(R.string.login_failure)
            }
            else -> {
            }
        }
    }

    @Subscribe
    fun onConnectAccountEvent(event: ConnectAccountLoginEvent?) {
        if (event == null) {
            return
        }
        app.resetLoginEvent()
        enableInput(true)
        when (event.status) {
            AuthStatus.SUCCESS -> {
                if (event.isRedirectToSetup) {
                    // TODO: show dialog and navigate to browser
                    return
                }
                hideProgress()
                mUserManager.loginState = LOGIN_STATE_LOGIN_FINISHED
                val mailboxLoginIntent = Intent(this, ConnectAccountMailboxLoginActivity::class.java)
                    .putExtra(EXTRA_KEY_SALT, event.keySalt)
                    .putExtra(EXTRA_CURRENT_PRIMARY_USER_ID, viewModel.currentPrimary?.s)
                startActivity(AppUtil.decorInAppIntent(mailboxLoginIntent))
                saveLastInteraction()
                finish()
            }
            AuthStatus.NO_NETWORK -> {
                hideProgress()
                showToast(R.string.no_network)
            }
            AuthStatus.UPDATE -> {
                hideProgress()
                AppUtil.postEventOnUi(ForceUpgradeEvent(event.error))
            }
            AuthStatus.INVALID_CREDENTIAL -> {
                hideProgress()
            }
            AuthStatus.INVALID_SERVER_PROOF -> {
                hideProgress()
                showToast(R.string.invalid_server_proof)
            }
            AuthStatus.FAILED -> {
                if (event.isRedirectToSetup) {
                    showInfoDialog(
                        this,
                        getString(R.string.login_failure),
                        getString(R.string.login_failure_missing_keys),
                        null
                    )
                }
                hideProgress()
            }
            AuthStatus.CANT_CONNECT -> {
                hideProgress()
                showInfoDialog(
                    this@ConnectAccountActivity,
                    getString(R.string.connect_account_limit_title),
                    getString(R.string.connect_account_limit_subtitle)
                ) {
                    saveLastInteraction()
                    moveToMailbox()
                    finish()
                }
            }
            else -> {
                hideProgress()
                showToast(R.string.login_failure)
            }
        }
    }

    @Subscribe
    override fun onConnectAccountMailboxLoginEvent(event: ConnectAccountMailboxLoginEvent) {
        super.onConnectAccountMailboxLoginEvent(event)
    }

    private fun onConnectClicked() {
        username.setText(username.text?.removeWhitespaces())
        if (username.text.toString().isNotEmpty() && password.text.toString().isNotEmpty()) {
            UiUtil.hideKeyboard(this)
            disableBack = true
            enableInput(false)
            progress.visibility = View.VISIBLE
            val username = username.text.toString()
            val password = password.text.toString().toByteArray() /*TODO passphrase*/
            viewModel.info(username, password)
        }
    }

    private fun onForgotPasswordClicked() {
        val browserIntent = Intent(Intent.ACTION_VIEW)
        browserIntent.data = Uri.parse(getString(R.string.link_forgot_pass))
        if (browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
        } else {
            showToast(R.string.no_browser_found)
        }
    }

    private fun showTwoFactorDialog(
        userId: Id,
        password: ByteArray,
        response: LoginInfoResponse?,
        loginResponse: LoginResponse?,
        fallbackAuthVersion: Int
    ) {
        twoFactorDialog?.let {
            if (it.isShowing) {
                return
            }
        }

        twoFactorDialog = DialogUtils.show2FADialog(this, {
            UiUtil.hideKeyboard(this)
            progress.visibility = View.VISIBLE
            twoFA(userId, password, it, response, loginResponse, fallbackAuthVersion)
            app.resetLoginInfoEvent()
        }, {
            UiUtil.hideKeyboard(this@ConnectAccountActivity)
            app.resetLoginInfoEvent()
            mUserManager.logoutBlocking(userId)
        })
    }

    private fun twoFA(
            userId: Id,
            password: ByteArray,
            twoFactor: String,
            infoResponse: LoginInfoResponse?,
            loginResponse: LoginResponse?,
            fallbackAuthVersion: Int) {
        mUserManager.twoFA(
            userId,
            password,
            twoFactor,
            infoResponse,
            loginResponse,
            signUp = false,
            isConnecting = true,
            fallbackAuthVersion = fallbackAuthVersion
        )
    }

    private fun hideProgress() {
        progress.visibility = View.GONE
    }

    private fun enableInput(enable: Boolean) {
        connect.isClickable = enable
        username.isFocusable = enable
        password.isFocusable = enable
        username.isFocusableInTouchMode = enable
        password.isFocusableInTouchMode = enable
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                moveToMailbox()
                saveLastInteraction()
                finish()
            }
        }
        return true
    }
}
