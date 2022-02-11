/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.details.domain.usecase

import android.content.Context
import androidx.webkit.WebViewFeature
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.usecase.IsAppInDarkMode
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * A use case that checks whether a given message should be viewed in dark mode or not
 */
class GetViewInDarkModeMessagePreference @Inject constructor(
    private val messageRepository: MessageRepository,
    private val isAppInDarkMode: IsAppInDarkMode
) {

    suspend operator fun invoke(
        context: Context,
        userId: UserId,
        messageId: String
    ): Boolean {
        return if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            messageRepository.getViewInDarkModeMessagePreference(userId, messageId)
                ?: isAppInDarkMode(context)
        } else false
    }
}
