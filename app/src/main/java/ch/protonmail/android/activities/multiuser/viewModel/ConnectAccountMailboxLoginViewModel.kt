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
package ch.protonmail.android.activities.multiuser.viewModel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.activities.multiuser.EXTRA_CURRENT_PRIMARY_USER_ID
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.Id
import kotlinx.coroutines.launch

class ConnectAccountMailboxLoginViewModel @ViewModelInject constructor(
    private val userManager: UserManager,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val currentPrimary = savedStateHandle.get<String>(EXTRA_CURRENT_PRIMARY_USER_ID)?.let(::Id)

    fun mailboxLogin(mailboxPassword: String, keySalt: String) {
        viewModelScope.launch {
            userManager.connectAccountMailboxLogin(userManager.requireCurrentUserId(),
                currentPrimary,
                mailboxPassword,
                keySalt)
        }
    }

}
