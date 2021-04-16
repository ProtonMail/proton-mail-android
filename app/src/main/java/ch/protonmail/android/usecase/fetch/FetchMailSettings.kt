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

package ch.protonmail.android.usecase.fetch

import android.content.Context
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.prefs.SecureSharedPreferences
import timber.log.Timber
import javax.inject.Inject

class FetchMailSettings @Inject constructor(
    private val context: Context,
    private val protonMailApiManager: ProtonMailApiManager
) {

    suspend operator fun invoke(userId: Id) {
        Timber.v("FetchMailSettings started")
        val mailSettingsResponse = protonMailApiManager.fetchMailSettings(userId)
        mailSettingsResponse.mailSettings.save(SecureSharedPreferences.getPrefsForUser(context, userId))
    }
}
