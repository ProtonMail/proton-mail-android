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
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.featureflags.FeatureFlagsManager
import javax.inject.Inject

private const val CONVERSATION_MODE_VIEW_MODE = 0

class ConversationModeEnabled @Inject constructor(
    private val featureFlagsManager: FeatureFlagsManager,
    private val userManager: UserManager
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
     */
    operator fun invoke(location: MessageLocationType?): Boolean {
        val isConversationViewMode = userManager
            .getCurrentUserMailSettingsBlocking()?.viewMode == CONVERSATION_MODE_VIEW_MODE
        return featureFlagsManager.isChangeViewModeFeatureEnabled() &&
            isConversationViewMode &&
            !forceMessagesViewModeLocations.contains(location)
    }


}
