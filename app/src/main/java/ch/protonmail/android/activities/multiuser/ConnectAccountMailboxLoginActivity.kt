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
import androidx.activity.viewModels
import ch.protonmail.android.R
import ch.protonmail.android.activities.multiuser.viewModel.ConnectAccountMailboxLoginViewModel
import ch.protonmail.android.events.ConnectAccountMailboxLoginEvent
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.app
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_connect_account_mailbox_login.*
import kotlinx.android.synthetic.main.connect_account_progress.*

// region constants
const val EXTRA_KEY_SALT = "key_salt"
const val EXTRA_CURRENT_PRIMARY_USER_ID = "extra.connect.current.primary.user.id"
// constants

/**
 * This activity handles the second step towards connecting an account,
 * where mailbox password should be provided.
 */
@AndroidEntryPoint
class ConnectAccountMailboxLoginActivity : ConnectAccountBaseActivity() {

    override val togglePasswordView: ToggleButton
        get() = toggleViewPassword
    override val passwordEditView: EditText
        get() = mailboxPassword

    private var setupComplete: Boolean = false

    private val viewModel: ConnectAccountMailboxLoginViewModel by viewModels()
    
    override fun getLayoutId(): Int = R.layout.activity_connect_account_mailbox_login

    override fun resetState() {
        progress.visibility = View.GONE
        mailboxPassword.isFocusable = true
        mailboxPassword.isFocusableInTouchMode = true
        mailboxSignIn.isClickable = true
    }

    override fun unsubscribeFromEvents() {
        app.bus.unregister(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mailboxSignIn.setOnClickListener { onMailboxLoginClicked() }
    }

    override fun onStart() {
        super.onStart()
        app.bus.register(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return true
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
            viewModel.mailboxLogin(mailboxPasswordValue, intent.getStringExtra(EXTRA_KEY_SALT)!!)
        }, 1500)
    }

    @Subscribe
    override fun onConnectAccountMailboxLoginEvent(event: ConnectAccountMailboxLoginEvent) {
        super.onConnectAccountMailboxLoginEvent(event)
    }
}
