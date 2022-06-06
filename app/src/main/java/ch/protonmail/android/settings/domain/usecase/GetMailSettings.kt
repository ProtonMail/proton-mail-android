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

package ch.protonmail.android.settings.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

class GetMailSettings @Inject constructor(
    private val mailSettingsRepository: MailSettingsRepository
) {

    operator fun invoke(userId: UserId): Flow<Result> =
        mailSettingsRepository.getMailSettingsFlow(userId).transformLatest { result ->
            when (result) {
                is DataResult.Success -> emit(Result.Success(result.value))
                is DataResult.Error -> emit(Result.Error(result.message))
                is DataResult.Processing -> Unit
            }.exhaustive
        }

    sealed class Result {
        data class Success(val mailSettings: MailSettings) : Result()
        data class Error(val message: String?) : Result()
    }
}
