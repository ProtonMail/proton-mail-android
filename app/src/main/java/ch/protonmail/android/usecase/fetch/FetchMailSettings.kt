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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.di.AppProcessLifecycleOwner
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.prefs.SecureSharedPreferences
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

class FetchMailSettings @Inject constructor(
    private val context: Context,
    private val protonMailApiManager: ProtonMailApiManager,
    private val mailSettingsRepository: MailSettingsRepository,
    private val dispatchers: DispatcherProvider,
    @AppProcessLifecycleOwner
    private val lifecycleOwner: LifecycleOwner
) {

    suspend operator fun invoke(
        userId: UserId
    ) = withContext(dispatchers.Io) {

        Timber.v("FetchMailSettings started")
        val mailSettingsResponse = protonMailApiManager.fetchMailSettings(Id(userId.id))
        mailSettingsResponse.mailSettings.save(SecureSharedPreferences.getPrefsForUser(context, Id(userId.id)))

        mailSettingsRepository.getMailSettingsFlow(userId, true).launchIn(lifecycleOwner.lifecycleScope)
    }
}
