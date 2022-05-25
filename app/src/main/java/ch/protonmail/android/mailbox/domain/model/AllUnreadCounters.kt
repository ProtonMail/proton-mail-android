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

package ch.protonmail.android.mailbox.domain.model

import me.proton.core.mailsettings.domain.entity.ViewMode

/**
 * A set of all the [UnreadCounter]s, for each [ViewMode] and labelId
 */
data class AllUnreadCounters(
    val messagesCounters: List<UnreadCounter>,
    val conversationsCounters: List<UnreadCounter>
) {

    fun getFor(viewMode: ViewMode): List<UnreadCounter> =
        when (viewMode) {
            ViewMode.ConversationGrouping -> conversationsCounters
            ViewMode.NoConversationGrouping -> messagesCounters
        }
}
