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

package ch.protonmail.android.settings.data

import ch.protonmail.android.prefs.SecureSharedPreferences
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

private const val PREF_HYPERLINK_CONFIRM = "confirmHyperlinks"

class SharedPreferencesAccountSettingsRepository @Inject constructor(
    private val secureSharedPreferencesFactory: SecureSharedPreferences.Factory,
    private val dispatchers: DispatcherProvider
) : AccountSettingsRepository {

    override suspend fun getShouldShowLinkConfirmationSetting(userId: UserId): Boolean = withContext(dispatchers.Io) {
        secureSharedPreferencesFactory.userPreferences(userId)
            .getBoolean(PREF_HYPERLINK_CONFIRM, true)
    }

    override suspend fun saveShouldShowLinkConfirmationSetting(
        shouldShowHyperlinkConfirmation: Boolean,
        userId: UserId
    ) {
        withContext(dispatchers.Io) {
            secureSharedPreferencesFactory.userPreferences(userId)[PREF_HYPERLINK_CONFIRM] =
                shouldShowHyperlinkConfirmation
        }
    }
}
