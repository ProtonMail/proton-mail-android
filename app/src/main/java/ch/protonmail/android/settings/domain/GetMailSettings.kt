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

package ch.protonmail.android.settings.domain

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import me.proton.core.domain.arch.DataResult
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class GetMailSettings @Inject constructor(
    private val accountManager: AccountManager,
    private val mailSettingsRepository: MailSettingsRepository,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke() = withContext(dispatchers.Io) {
        accountManager.getPrimaryAccount()
            .filterNotNull()
            .flatMapLatest { account -> mailSettingsRepository.getMailSettingsFlow(account.userId) }
            .mapLatest { result ->
                when (result) {
                    is DataResult.Success -> {
                        MailSettingsState.Success(result.value)
                    }
                    is DataResult.Error -> MailSettingsState.Error.Message(result.message)
                    else -> Unit
                }
            }
    }

    sealed class MailSettingsState {
        data class Success(val mailSettings: MailSettings?) : MailSettingsState()
        sealed class Error : MailSettingsState() {
            data class Message(val message: String?) : Error()
            object NoPrimaryAccount : Error()
        }
    }
}
