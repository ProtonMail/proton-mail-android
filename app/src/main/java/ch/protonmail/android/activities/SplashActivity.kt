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

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.feature.account.AccountViewModel
import ch.protonmail.android.utils.startMailboxActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SplashActivity : BaseActivity() {

    private fun startMailbox() {
        fetchMailSettingsWorkerEnqueuer.enqueue()
        startMailboxActivity()
        finish()
    }

    override fun getLayoutId(): Int = R.layout.activity_splash

    override fun shouldCheckForAutoLogout(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accountViewModel.state
            .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
            .onEach {
                when (it) {
                    is AccountViewModel.State.Processing -> Unit
                    is AccountViewModel.State.LoginNeeded -> accountViewModel.login()
                    is AccountViewModel.State.LoginClosed -> finish()
                    is AccountViewModel.State.AccountList -> startMailbox()
                }
            }.launchIn(lifecycleScope)
    }
}
