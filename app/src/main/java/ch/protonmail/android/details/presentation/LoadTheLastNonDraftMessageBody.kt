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

package ch.protonmail.android.details.presentation

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.domain.usecase.MarkMessageAsReadIfNeeded
import ch.protonmail.android.details.presentation.model.ConversationUiModel
import ch.protonmail.android.utils.crypto.KeyInformation
import javax.inject.Inject

class LoadTheLastNonDraftMessageBody @Inject constructor(
    private val messageBodyLoader: MessageBodyLoader,
    private val markMessageAsReadIfNeeded: MarkMessageAsReadIfNeeded
) {

    suspend operator fun invoke(
        conversation: ConversationUiModel,
        formatMessageHtmlBody: (Message, Int, String, String, String) -> String,
        handleEmbeddedImagesLoading: (Message) -> Boolean,
        publicKeys: List<KeyInformation>?,
        visibleToTheUser: Boolean
    ): ConversationUiModel {
        conversation.messages
            .withIndex()
            .findLast { (_, message) -> !message.isDraft() }
            ?.let { (index, message) ->
                return addMessageBodyToConversation(
                    message,
                    formatMessageHtmlBody,
                    handleEmbeddedImagesLoading,
                    publicKeys,
                    visibleToTheUser,
                    conversation,
                    index
                )
            }
            ?: return conversation
    }

    private suspend fun addMessageBodyToConversation(
        message: Message,
        formatMessageHtmlBody: (Message, Int, String, String, String) -> String,
        handleEmbeddedImagesLoading: (Message) -> Boolean,
        publicKeys: List<KeyInformation>?,
        visibleToTheUser: Boolean,
        conversation: ConversationUiModel,
        index: Int
    ): ConversationUiModel {
        val messageWithLoadedBody = messageBodyLoader.loadExpandedMessageBody(
            message,
            formatMessageHtmlBody,
            handleEmbeddedImagesLoading,
            publicKeys
        )
        return if (messageWithLoadedBody != null) {
            markMessageAsReadIfNeeded(message, visibleToTheUser)
            val newMessageList = conversation.messages.toMessageListItemsLoadedAt(index, messageWithLoadedBody)
            conversation.copy(
                messageListItems = newMessageList, messages = newMessageList.map { it.message }
            )
        } else {
            conversation
        }
    }

    private fun List<Message>.toMessageListItemsLoadedAt(
        index: Int,
        messageWithLoadedBody: MessageDetailsListItem
    ): List<MessageDetailsListItem> =
        map { MessageDetailsListItem(it, null, null) }
            .toMutableList()
            .apply {
                if (index != -1) this[index] = messageWithLoadedBody
            }
}
