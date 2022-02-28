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

package ch.protonmail.android.details.presentation.util

import androidx.fragment.app.FragmentActivity
import ch.protonmail.android.R
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.domain.MessageBodyDecryptor
import ch.protonmail.android.details.presentation.mapper.MessageToMessageDetailsListItemMapper
import ch.protonmail.android.details.presentation.model.MessageDetailsListItem
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.utils.crypto.KeyInformation
import ch.protonmail.android.utils.css.MessageBodyCssProvider
import ch.protonmail.android.utils.resources.StringResourceResolver
import ch.protonmail.android.utils.ui.screen.RenderDimensionsProvider
import javax.inject.Inject

class MessageBodyLoader @Inject constructor(
    private val userManager: UserManager,
    private val messageRepository: MessageRepository,
    private val renderDimensionsProvider: RenderDimensionsProvider,
    private val messageBodyCssProvider: MessageBodyCssProvider,
    private val getStringResource: StringResourceResolver,
    private val messageToMessageDetailsItemMapper: MessageToMessageDetailsListItemMapper,
    private val decryptMessageBody: MessageBodyDecryptor
) {

    suspend fun loadExpandedMessageBodyOrNull(
        expandedMessage: Message,
        formatMessageHtmlBody: (
            message: Message,
            windowWidth: Int,
            messageBodyCss: String,
            messageBodyDarkModeCss: String,
            defaultErrorMessage: String
        ) -> String,
        handleEmbeddedImagesLoading: (Message) -> Boolean,
        publicKeys: List<KeyInformation>?,
        // TODO: Rethink this- should not pass an activity instance here, perhaps should pass the
        // render dimension as a param directly from the activity
        fragmentActivity: FragmentActivity
    ): MessageDetailsListItem? {
        val fetchedMessage = fetchMessageBody(requireNotNull(expandedMessage.messageId))
        return if (fetchedMessage != null) {
            val messageDecrypted = decryptMessageBody(fetchedMessage, publicKeys)
            val messageBody = formatMessageHtmlBody(
                fetchedMessage,
                renderDimensionsProvider.getRenderWidth(fragmentActivity),
                messageBodyCssProvider.getMessageBodyCss(),
                messageBodyCssProvider.getMessageBodyDarkModeCss(userManager.requireCurrentUserId(), fetchedMessage.messageId!!),
                getStringResource(R.string.request_timeout)
            )
            fetchedMessage.decryptedHTML = messageBody
            messageToMessageDetailsItemMapper.toMessageDetailsListItem(
                message = fetchedMessage,
                messageBody = messageBody,
                shouldShowDecryptionError = !messageDecrypted,
                shouldShowLoadEmbeddedImagesButton = handleEmbeddedImagesLoading(fetchedMessage)
            )
        } else {
            null
        }
    }

    private suspend fun fetchMessageBody(messageId: String): Message? {
        val userId = userManager.requireCurrentUserId()
        return messageRepository.getMessage(userId, messageId, true)
    }
}

