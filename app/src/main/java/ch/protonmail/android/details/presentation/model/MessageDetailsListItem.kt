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

package ch.protonmail.android.details.presentation.model

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.utils.ui.ExpandableRecyclerAdapter
import ch.protonmail.android.utils.ui.TYPE_HEADER
import ch.protonmail.android.utils.ui.TYPE_ITEM

open class MessageDetailsListItem(
    val type: Int,
    var message: Message,
    val showOpenInProtonCalendar: Boolean = false
) : ExpandableRecyclerAdapter.ListItem(type) {

    var messageFormattedHtmlWithQuotedHistory: String? = null
    var messageFormattedHtml: String? = null
    var showLoadEmbeddedImagesButton: Boolean = false
    var showDecryptionError: Boolean = false
    var embeddedImageIds: List<String> = emptyList()

    @Deprecated("Use Header subclass", ReplaceWith("MessageDetailsListItem.Header(message)"))
    constructor(message: Message) : this(TYPE_HEADER, message)

    @Deprecated(
        "Use Body subclass",
        ReplaceWith(
            """MessageDetailsListItem.Body(
                |   message = message, 
                |   messageFormattedHtml = messageContent, 
                |   messageFormattedHtmlWithQuotedHistory = originalMessageContent, 
                |   showOpenInProtonCalendar = showOpenInProtonCalendar
                |)"""
        )
    )
    constructor(
        message: Message,
        messageContent: String?,
        originalMessageContent: String?,
        showOpenInProtonCalendar: Boolean
    ) : this(TYPE_ITEM, message, showOpenInProtonCalendar) {
        this.messageFormattedHtml = messageContent
        this.messageFormattedHtmlWithQuotedHistory = originalMessageContent
    }

    class Header(
        message: Message
    ) : MessageDetailsListItem(TYPE_HEADER, message)

    class Body(
        message: Message,
        messageFormattedHtml: String?,
        messageFormattedHtmlWithQuotedHistory: String?,
        showOpenInProtonCalendar: Boolean
    ) : MessageDetailsListItem(TYPE_ITEM, message, showOpenInProtonCalendar) {

        init {
            this.messageFormattedHtml = messageFormattedHtml
            this.messageFormattedHtmlWithQuotedHistory = messageFormattedHtmlWithQuotedHistory
        }
    }
}
