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

package ch.protonmail.android.details.presentation.mapper

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.domain.MessageBodyParser
import ch.protonmail.android.details.presentation.model.MessageDetailsListItem
import ch.protonmail.android.util.ProtonCalendarUtil
import me.proton.core.domain.arch.Mapper
import javax.inject.Inject

class MessageToMessageDetailsListItemMapper @Inject constructor(
    private val messageBodyParser: MessageBodyParser,
    private val protonCalendarUtil: ProtonCalendarUtil
) : Mapper<Message, MessageDetailsListItem> {

    fun toMessageDetailsListItem(
        message: Message,
        messageBody: String,
        shouldShowDecryptionError: Boolean,
        shouldShowLoadEmbeddedImagesButton: Boolean
    ) = MessageDetailsListItem.Body(
        message = message,
        messageFormattedHtml = messageBody,
        messageFormattedHtmlWithQuotedHistory = messageBody,
        showOpenInProtonCalendar = protonCalendarUtil.hasCalendarAttachment(message)
    ).apply {
        val messageBodyParts = messageBodyParser.splitBody(messageBody)
        messageFormattedHtml = messageBodyParts.messageBody
        messageFormattedHtmlWithQuotedHistory = messageBodyParts.messageBodyWithQuote
        showLoadEmbeddedImagesButton = shouldShowLoadEmbeddedImagesButton
        showDecryptionError = shouldShowDecryptionError
    }
}
