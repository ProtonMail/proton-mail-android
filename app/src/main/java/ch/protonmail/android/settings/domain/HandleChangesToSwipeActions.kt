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

import ch.protonmail.android.domain.entity.Id
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class HandleChangesToSwipeActions @Inject constructor(
    private val mailSettingsRepository: MailSettingsRepository,
    private val dispatchers: DispatcherProvider
) {

    /**
     * @param userId Id of the user who is currently logged in
     * @param swipeRight and swipeLeft values that we want to change
     */
    suspend operator fun invoke(
        userId: Id,
        swipeRight: SwipeAction? = null,
        swipeLeft: SwipeAction? = null
    ) = withContext(dispatchers.Io) {
        swipeRight?.let {
            mailSettingsRepository.updateSwipeRight(UserId(userId.s), swipeRight)
        }
        swipeLeft?.let {
            mailSettingsRepository.updateSwipeLeft(UserId(userId.s), swipeLeft)
        }
    }
}
