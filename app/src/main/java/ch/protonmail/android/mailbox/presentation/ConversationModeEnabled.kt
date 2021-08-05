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

package ch.protonmail.android.mailbox.presentation

import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.featureflags.FeatureFlagsManager
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class ConversationModeEnabled @Inject constructor(
    private val featureFlagsManager: FeatureFlagsManager,
    private val accountManager: AccountManager,
    private val mailSettingsRepository: MailSettingsRepository,
    private val dispatchers: DispatcherProvider
) {

    private val forceMessagesViewModeLocations = listOf(
        MessageLocationType.DRAFT,
        MessageLocationType.ALL_DRAFT,
        MessageLocationType.SENT,
        MessageLocationType.ALL_SENT,
        MessageLocationType.SEARCH
    )

    /**
     * When location is null, the location is ignored.
     * When userId is null, use the current primary user id.
     */
    operator fun invoke(location: MessageLocationType?, userId: UserId? = null): Boolean {
        val isConversationViewMode: Boolean

        runBlocking {
            isConversationViewMode = getMailSettings(userId).viewMode?.enum == ViewMode.ConversationGrouping
        }

        return featureFlagsManager.isChangeViewModeFeatureEnabled() &&
            isConversationViewMode &&
            !forceMessagesViewModeLocations.contains(location)
    }

    private suspend fun getMailSettings(userId: UserId?): MailSettings = withContext(dispatchers.Io) {
        val primaryUserId = accountManager.getPrimaryUserId().filterNotNull().first()
        mailSettingsRepository.getMailSettings(userId ?: primaryUserId)
    }
}
