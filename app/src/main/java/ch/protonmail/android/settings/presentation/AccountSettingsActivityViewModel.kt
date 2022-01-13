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

package ch.protonmail.android.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.R
import ch.protonmail.android.settings.domain.GetMailSettings
import ch.protonmail.android.settings.domain.UpdateViewMode
import ch.protonmail.android.usecase.delete.ClearUserMessagesData
import ch.protonmail.android.utils.resources.StringResourceResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.usersettings.domain.usecase.GetSettings
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

@HiltViewModel
class AccountSettingsActivityViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private var clearUserMessagesData: ClearUserMessagesData,
    private var updateViewMode: UpdateViewMode,
    private val getMailSettings: GetMailSettings,
    private val getUserSettings: GetSettings,
    private val dispatcherProvider: DispatcherProvider,
    private val stringResourceResolver: StringResourceResolver
) : ViewModel() {

    private val _recoveryEmailResult = MutableSharedFlow<String>()
    val recoveryEmailResult: SharedFlow<String>
        get() = _recoveryEmailResult

    suspend fun getMailSettings() = getMailSettings.invoke()

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

    fun getRecoveryEmail() {
        accountManager.getPrimaryUserId()
            .filterNotNull()
            .map { userId ->
                val emailAddress = getUserSettings(userId).email?.value
                if (emailAddress.isNullOrEmpty()) {
                    stringResourceResolver(R.string.not_set)
                } else {
                    emailAddress
                }
            }
            .flowOn(dispatcherProvider.Io)
            .onEach { _recoveryEmailResult.emit(it) }
            .launchIn(viewModelScope)
    }
}
