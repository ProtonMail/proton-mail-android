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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.feature.account.AccountStateManager
import ch.protonmail.android.utils.startMailboxActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.auth.presentation.AuthOrchestrator
import javax.inject.Inject

@AndroidEntryPoint
internal class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var accountStateManager: AccountStateManager

    @Inject
    lateinit var authOrchestrator: AuthOrchestrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authOrchestrator.register(this)

        with(accountStateManager) {
            setAuthOrchestrator(authOrchestrator)
            observeAccountStateWithExternalLifecycle(lifecycle, isSplashActivity = true)
            // Start Login or MailboxActivity.
            state
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .onEach {
                    when (it) {
                        AccountStateManager.State.Processing ->
                            Unit
                        AccountStateManager.State.AccountNeeded ->
                            addAccount()
                        AccountStateManager.State.PrimaryExist -> {
                            delay(resources.getInteger(R.integer.splash_transition_millis).toLong())
                            startMailboxActivity()
                            overridePendingTransition(0, 0)
                            finishAndRemoveTask()
                        }
                    }
                }.launchIn(lifecycleScope)

            // Finish if AddAccount closed.
            onAddAccountClosed {
                finishAndRemoveTask()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accountStateManager.setAuthOrchestrator(authOrchestrator)
    }

    override fun onDestroy() {
        authOrchestrator.unregister()
        super.onDestroy()
    }
}
