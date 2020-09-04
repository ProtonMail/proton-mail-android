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
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ToggleButton
import androidx.lifecycle.ViewModelProviders
import ch.protonmail.android.R
import ch.protonmail.android.activities.multiuser.viewModel.ConnectAccountMailboxLoginViewModel
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.ConnectAccountMailboxLoginEvent
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.moveToMailbox
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_connect_account_mailbox_login.*
import kotlinx.android.synthetic.main.connect_account_progress.*
import javax.inject.Inject

// region constants
const val EXTRA_KEY_SALT = "key_salt"
const val EXTRA_CURRENT_PRIMARY = "connect_current_primary"
// constants

/**
 * This activity handles the second step towards connecting an account,
 * where mailbox password should be provided.
 */
@AndroidEntryPoint
class ConnectAccountMailboxLoginActivity : ConnectAccountBaseActivity() {
    override fun removeAccount(username: String) {
        viewModel.removeAccount(username)
    }

    override val togglePasswordView: ToggleButton
        get() = toggleViewPassword
    override val passwordEditView: EditText
        get() = mailboxPassword

    private var setupComplete: Boolean = false
    /** [ConnectAccountMailboxLoginViewModel.Factory] for [ConnectAccountMailboxLoginViewModel] */
    @Inject
    lateinit var viewModelFactory: ConnectAccountMailboxLoginViewModel.Factory

    /** A Lazy instance of [ConnectAccountMailboxLoginViewModel] */
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)
                .get(ConnectAccountMailboxLoginViewModel::class.java)
    }

    override fun getLayoutId(): Int = R.layout.activity_connect_account_mailbox_login

    override fun resetState() {
        progress.visibility = View.GONE
        mailboxPassword.isFocusable = true
        mailboxPassword.isFocusableInTouchMode = true
        mailboxSignIn.isClickable = true
    }

    override fun unsubscribeFromEvents() {
        ProtonMailApplication.getApplication().bus.unregister(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.username = intent?.extras?.getString(EXTRA_USERNAME, null)
        viewModel.currentPrimary = intent?.extras?.getString(EXTRA_CURRENT_PRIMARY, null)
        mailboxSignIn.setOnClickListener { onMailboxLoginClicked() }
    }

    override fun onStart() {
        super.onStart()
        ProtonMailApplication.getApplication().bus.register(this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (!setupComplete) {
            viewModel.username?.let {
                viewModel.logoutAccount(it)
                moveToMailbox()
            }
        }
    }

    private fun onMailboxLoginClicked() {
        setupComplete = true
        mailboxSignIn.isClickable = false
        mailboxPassword.isFocusable = false
        progress.visibility = View.VISIBLE
        val mailboxPasswordValue = mailboxPassword.text.toString()
        UiUtil.hideKeyboard(this, mailboxPassword)
        Handler().postDelayed({
            setupComplete = false
            viewModel.mailboxLogin(mailboxPasswordValue, intent.getStringExtra(EXTRA_KEY_SALT))
        }, 1500)
    }

    @Subscribe
    override fun onConnectAccountMailboxLoginEvent(event: ConnectAccountMailboxLoginEvent) {
        super.onConnectAccountMailboxLoginEvent(event)
    }
}
