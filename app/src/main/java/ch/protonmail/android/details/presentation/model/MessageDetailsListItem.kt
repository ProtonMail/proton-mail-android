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

package ch.protonmail.android.details.presentation.model

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.utils.ui.ExpandableRecyclerAdapter
import ch.protonmail.android.utils.ui.TYPE_HEADER
import ch.protonmail.android.utils.ui.TYPE_ITEM

sealed class MessageDetailsListItem(
    val type: Int,
) : ExpandableRecyclerAdapter.ListItem(type) {

    abstract val message: Message

    data class Header(
        override val message: Message
    ) : MessageDetailsListItem(TYPE_HEADER)

    data class Body(
        override val message: Message,
        val messageFormattedHtml: String?,
        val messageFormattedHtmlWithQuotedHistory: String?,
        val showOpenInProtonCalendar: Boolean,
        val showLoadEmbeddedImagesButton: Boolean,
        val showDecryptionError: Boolean,
        val showScheduledInfo: Boolean,
        val embeddedImageIds: List<String> = emptyList()
    ) : MessageDetailsListItem(TYPE_ITEM)
}
