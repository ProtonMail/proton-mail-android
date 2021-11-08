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

package ch.protonmail.android.mailbox.domain.usecase

import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.featureflags.FeatureFlagsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import javax.inject.Inject

/**
 * Emit whether Conversation Mode is enabled or disabled
 * A parameter [MessageLocationType] is optional, if we want to know whether it's enabled on a specific location.
 */
class ObserveConversationModeEnabled @Inject constructor(
    private val featureFlagsManager: FeatureFlagsManager,
    private val mailSettingsRepository: MailSettingsRepository
) {

    private val forceMessagesViewModeLocations = listOf(
        MessageLocationType.DRAFT,
        MessageLocationType.ALL_DRAFT,
        MessageLocationType.SENT,
        MessageLocationType.ALL_SENT,
        MessageLocationType.SEARCH
    )

    /**
     * @param locationType used for know whether the Conversation Mode is enabled for a given location.
     *  Use `null` if you're interested in the global value, instead of a specific location
     */
    operator fun invoke(userId: UserId, locationType: MessageLocationType? = null): Flow<Boolean> =
        mailSettingsRepository.getMailSettingsFlow(userId).mapSuccessValueOrNull().map { settings ->
            val isFeatureEnabled = featureFlagsManager.isChangeViewModeFeatureEnabled()
            val isEnabledInSettings = settings?.viewMode?.enum == ViewMode.ConversationGrouping
            val isAvailableForLocation = locationType !in forceMessagesViewModeLocations
            isFeatureEnabled && isEnabledInSettings && isAvailableForLocation
        }.distinctUntilChanged()
}
