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
package ch.protonmail.android.api.models.messages.receive

import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.enumerations.MessageFlag
import ch.protonmail.android.api.models.factories.checkIfSet
import ch.protonmail.android.api.models.factories.parseBoolean
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.utils.MessageUtils
import timber.log.Timber
import javax.inject.Inject

class MessageFactory @Inject constructor(
    private val attachmentFactory: IAttachmentFactory,
    private val messageSenderFactory: MessageSenderFactory,
    private val messageLocationResolver: MessageLocationResolver
) {

    fun createDraftApiRequest(message: Message): DraftBody = DraftBody(message.toApiPayload())

    fun createMessage(serverMessage: ServerMessage): Message {
        return Message(
            messageId = serverMessage.id,
            conversationId = serverMessage.ConversationID,
            subject = serverMessage.Subject,
            Unread = serverMessage.Unread.parseBoolean("Unread"),
            Type = MessageUtils.calculateType(serverMessage.Flags),

            sender = messageSenderFactory.createMessageSender(requireNotNull(serverMessage.Sender)),
            time = serverMessage.time.checkIfSet("Time"),
            totalSize = serverMessage.Size.checkIfSet("Size"),
            location = messageLocationResolver.resolveLocationFromLabels(
                serverMessage.LabelIDs ?: emptyList()
            ).messageLocationTypeValue,

            isStarred = serverMessage.LabelIDs!!
                .asSequence()
                .filter { it.length <= 2 }
                .map { Constants.MessageLocationType.fromInt(it.toInt()) }
                .contains(Constants.MessageLocationType.STARRED),
            folderLocation = serverMessage.FolderLocation,
            numAttachments = serverMessage.NumAttachments,
            messageEncryption = MessageUtils.calculateEncryption(serverMessage.Flags),
            expirationTime = serverMessage.ExpirationTime,
            isReplied = serverMessage.Flags and MessageFlag.REPLIED.value == MessageFlag.REPLIED.value,
            isRepliedAll = serverMessage.Flags and MessageFlag.REPLIED_ALL.value == MessageFlag.REPLIED_ALL.value,
            isForwarded = serverMessage.Flags and MessageFlag.FORWARDED.value == MessageFlag.FORWARDED.value,
            isDownloaded = serverMessage.Body != null,
            spamScore = serverMessage.SpamScore,
            addressID = serverMessage.AddressID,
            messageBody = serverMessage.Body,
            mimeType = serverMessage.MIMEType,
            allLabelIDs = serverMessage.LabelIDs.toList(),
            toList = serverMessage.ToList?.toList() ?: listOf(),
            ccList = serverMessage.CCList?.toList() ?: listOf(),
            bccList = serverMessage.BCCList?.toList() ?: listOf(),
            replyTos = serverMessage.ReplyTos?.toList() ?: listOf(),
            header = serverMessage.Header,
            parsedHeaders = serverMessage.parsedHeaders
        ).apply {
            attachments = serverMessage.Attachments?.map(attachmentFactory::createAttachment) ?: emptyList()
            embeddedImageIds = serverMessage.embeddedImagesArray?.toList() ?: emptyList()
            val attachmentsListSize = attachments.size
            if (attachmentsListSize != 0 && attachmentsListSize != numAttachments) {
                throw IllegalArgumentException(
                    "Attachments size does not match expected: $numAttachments, actual: $attachmentsListSize "
                )
            }
            Timber.v(
                "created Message id: ${serverMessage.id?.take(
                    6
                )}, body size: ${serverMessage.Body?.length}, location: $location, isDownloaded: $isDownloaded"
            )
        }

    }
}
