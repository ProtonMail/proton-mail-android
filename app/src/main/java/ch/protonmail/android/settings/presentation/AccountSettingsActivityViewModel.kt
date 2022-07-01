/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.Right
import arrow.core.left
import ch.protonmail.android.R
import ch.protonmail.android.feature.NotLoggedIn
import ch.protonmail.android.settings.domain.GetMailSettings
import ch.protonmail.android.settings.domain.UpdateViewMode
import ch.protonmail.android.settings.domain.usecase.ObserveUserSettings
import ch.protonmail.android.usecase.delete.ClearUserMessagesData
import ch.protonmail.android.utils.resources.StringResourceResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.mailsettings.domain.entity.ViewMode
import javax.inject.Inject

@HiltViewModel
class AccountSettingsActivityViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private var clearUserMessagesData: ClearUserMessagesData,
    private var updateViewMode: UpdateViewMode,
    private val getMailSettings: GetMailSettings,
    private val stringResourceResolver: StringResourceResolver,
    private val observeUserSettings: ObserveUserSettings
) : ViewModel() {

    suspend fun getMailSettings(): Flow<Either<NotLoggedIn, GetMailSettings.Result>> =
        accountManager.getPrimaryUserId().flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(NotLoggedIn.left())
            }

            getMailSettings(userId).map(::Right)
        }

    fun changeViewMode(viewMode: ViewMode) {
        viewModelScope.launch {
            accountManager.getPrimaryUserId().first()?.let { userId ->
                clearUserMessagesData.invoke(userId)
                updateViewMode.invoke(
                    userId, viewMode
                )
            }
        }
    }

    fun getRecoveryEmailFlow(): Flow<String> {
        return accountManager.getPrimaryUserId()
            .filterNotNull()
            .flatMapLatest { userId ->
                observeUserSettings(userId)
                    .map {
                        val recoveryEmail = it?.email?.value
                        if (recoveryEmail.isNullOrEmpty()) {
                            stringResourceResolver(R.string.not_set)
                        } else {
                            recoveryEmail
                        }
                    }
            }
    }
}
