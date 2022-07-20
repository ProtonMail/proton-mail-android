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

import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

/**
 * Returns a [UpdateSwipeActions.Result]
 */
class UpdateSwipeActions @Inject constructor(
    private val mailSettingsRepository: MailSettingsRepository,
    private val dispatchers: DispatcherProvider
) {

    /**
     * @param userId Id of the user who is currently logged in
     * @param swipeRight and swipeLeft values that we want to change
     */
    suspend operator fun invoke(
        userId: UserId,
        swipeRight: SwipeAction? = null,
        swipeLeft: SwipeAction? = null
    ): Result = runCatching {
        withContext(dispatchers.Io) {
            swipeRight?.let { mailSettingsRepository.updateSwipeRight(userId, it) }
            swipeLeft?.let { mailSettingsRepository.updateSwipeLeft(userId, it) }
        }
    }.fold(
        onSuccess = { Result.Success },
        onFailure = { Result.Error }
    )

    sealed class Result {

        object Success : Result()

        /**
         * Unknown error occurred
         */
        object Error : Result()
    }
}
