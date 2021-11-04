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

package ch.protonmail.android.activities.messageDetails

import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.body.MessageBodyDecryptor
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.domain.MessageBodyParser
import ch.protonmail.android.details.presentation.MessageDetailsListItem
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.utils.crypto.KeyInformation
import ch.protonmail.android.utils.css.MessageBodyCssProvider
import ch.protonmail.android.utils.resources.StringResourceResolver
import ch.protonmail.android.utils.ui.screen.RenderDimensionsProvider
import javax.inject.Inject

internal class MessageBodyLoader @Inject constructor(
    private val userManager: UserManager,
    private val messageRepository: MessageRepository,
    private val renderDimensionsProvider: RenderDimensionsProvider,
    private val messageBodyCssProvider: MessageBodyCssProvider,
    private val getStringResource: StringResourceResolver,
    private val messageBodyParser: MessageBodyParser,
    private val decryptMessageBody: MessageBodyDecryptor
) {

    suspend fun loadExpandedMessageBody(
        expandedMessage: Message,
        formatMessageHtmlBody: (Message, Int, String, String, String) -> String,
        handleEmbeddedImagesLoading: (Message) -> Boolean,
        publicKeys: List<KeyInformation>?
    ): MessageDetailsListItem? {
        val fetchedMessage = fetchMessageBody(requireNotNull(expandedMessage.messageId))
        return if (fetchedMessage != null) {
            val messageDecrypted = decryptMessageBody(fetchedMessage, publicKeys)
            val messageBody = formatMessageHtmlBody(
                fetchedMessage,
                renderDimensionsProvider.getRenderWidth(),
                messageBodyCssProvider.getMessageBodyCss(),
                messageBodyCssProvider.getMessageBodyDarkModeCss(),
                getStringResource(R.string.request_timeout)
            )
            fetchedMessage.decryptedHTML = messageBody
            fetchedMessage.toMessageDetailsListItem(messageBody, messageDecrypted, handleEmbeddedImagesLoading)
        } else {
            null
        }
    }

    private suspend fun fetchMessageBody(messageId: String): Message? {
        val userId = userManager.requireCurrentUserId()
        return messageRepository.getMessage(userId, messageId, true)
    }

    private fun Message.toMessageDetailsListItem(
        messageBody: String,
        decrypted: Boolean,
        handleEmbeddedImagesLoading: (Message) -> Boolean
    ) = MessageDetailsListItem(this, messageBody, messageBody).apply {
        val messageBodyParts = messageBodyParser.splitBody(messageBody)
        messageFormattedHtml = messageBodyParts.messageBody
        messageFormattedHtmlWithQuotedHistory = messageBodyParts.messageBodyWithQuote
        showLoadEmbeddedImagesButton = handleEmbeddedImagesLoading(this@toMessageDetailsListItem)
        showDecryptionError = !decrypted
    }
}

