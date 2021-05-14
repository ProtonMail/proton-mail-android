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
import ch.protonmail.android.feature.account.AccountStateManager
import ch.protonmail.android.utils.startMailboxActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var accountStateManager: AccountStateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(accountStateManager) {
            register(this@SplashActivity)

            // Start Login or MailboxActivity.
            state
                .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                .onEach {
                    when (it) {
                        AccountStateManager.State.Processing -> Unit
                        AccountStateManager.State.AccountNeeded ->
                            login()
                        AccountStateManager.State.PrimaryExist -> {
                            unregister()
                            startMailboxActivity()
                            finish()
                        }
                    }
                }.launchIn(lifecycleScope)

            // Finish if Login closed.
            onLoginClosed {
                unregister()
                finish()
            }
        }
    }
}
