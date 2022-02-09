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

package ch.protonmail.android.security.presentation

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.navigation.presentation.NavigationActivity
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.auth.presentation.ui.AddAccountActivity
import me.proton.core.auth.presentation.ui.AuthActivity
import javax.inject.Inject

internal class LogoutHandler @Inject constructor(
    private val accountManager: AccountManager,
    private val userManager: UserManager
) {

    fun register(activity: ComponentActivity) {
        if (activity.isAuthActivity() || activity.isRootActivity()) return

        val currentUserId = userManager.currentUserId

        accountManager.getPrimaryUserId()
            .flowWithLifecycle(activity.lifecycle)
            .filter { it != currentUserId }
            .onEach { activity.finish() }
            .launchIn(activity.lifecycleScope)
    }

    private fun Activity.isAuthActivity() = when (this) {
        is AuthActivity<*>, is AddAccountActivity -> true
        else -> false
    }

    private fun Activity.isRootActivity() = this is NavigationActivity

}
