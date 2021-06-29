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

import androidx.annotation.VisibleForTesting
import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.enumerations.MessageFlag
import ch.protonmail.android.api.models.factories.checkIfSet
import ch.protonmail.android.api.models.factories.parseBoolean
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.extensions.notNull
import javax.inject.Inject

class MessageFactory @Inject constructor(
    private val attachmentFactory: IAttachmentFactory,
    private val messageSenderFactory: MessageSenderFactory
) {

    fun createDraftApiRequest(message: Message): DraftBody = DraftBody(message.toApiPayload())

    fun createMessage(serverMessage: ServerMessage): Message {
        return serverMessage.let {
            val message = Message()
            message.messageId = it.ID
            message.conversationId = it.ConversationID
            message.subject = it.Subject
            message.Unread = it.Unread.parseBoolean("Unread")

            message.Type = MessageUtils.calculateType(it.Flags)

            val serverMessageSender = it.Sender ?: throw IllegalArgumentException("Sender is not set")
            message.sender = messageSenderFactory.createMessageSender(serverMessageSender)
            message.time = it.Time.checkIfSet("Time")
            message.totalSize = it.Size.checkIfSet("Size")
            message.location = resolveLocationFromLabels(it.LabelIDs ?: emptyList())
            message.isStarred = it.LabelIDs!!
                .asSequence()
                .filter { it.length <= 2 }
                .map { Constants.MessageLocationType.fromInt(it.toInt()) }
                .contains(Constants.MessageLocationType.STARRED)
            message.folderLocation = it.FolderLocation
            message.numAttachments = it.NumAttachments
            message.messageEncryption = MessageUtils.calculateEncryption(it.Flags)
            message.expirationTime = it.ExpirationTime.notNull("ExpirationTime")
            message.isReplied = (it.Flags and MessageFlag.REPLIED.value) == MessageFlag.REPLIED.value
            message.isRepliedAll = (it.Flags and MessageFlag.REPLIED_ALL.value) == MessageFlag.REPLIED_ALL.value
            message.isForwarded = (it.Flags and MessageFlag.FORWARDED.value) == MessageFlag.FORWARDED.value
            message.isDownloaded = it.Body != null
            message.spamScore = it.SpamScore
            message.addressID = it.AddressID
            message.messageBody = it.Body
            message.mimeType = it.MIMEType
            message.allLabelIDs = it.LabelIDs?.toList() ?: listOf()
            message.toList = it.ToList?.toList() ?: listOf()
            message.ccList = it.CCList?.toList() ?: listOf()
            message.bccList = it.BCCList?.toList() ?: listOf()
            message.replyTos = it.ReplyTos?.toList() ?: listOf()
            message.header = it.Header
            message.parsedHeaders = it.parsedHeaders
            message.attachments = it.Attachments?.map(attachmentFactory::createAttachment) ?: emptyList()
            message.embeddedImageIds = it.embeddedImagesArray?.toList() ?: emptyList()
            val numOfAttachments = message.numAttachments
            val attachmentsListSize = message.attachments.size
            if (attachmentsListSize != 0 && attachmentsListSize != numOfAttachments) {
                throw IllegalArgumentException(
                    "Attachments size does not match expected: $numOfAttachments, actual: $attachmentsListSize "
                )
            }
            message
        }
    }

    @VisibleForTesting
    fun resolveLocationFromLabels(labelIds: List<String>): Int {
        if (labelIds.isEmpty()) {
            return 0 // Inbox
        }

        val validLocations: List<Int> = listOf(
            Constants.MessageLocationType.INBOX.messageLocationTypeValue,
            Constants.MessageLocationType.TRASH.messageLocationTypeValue,
            Constants.MessageLocationType.SPAM.messageLocationTypeValue,
            Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue,
            Constants.MessageLocationType.SENT.messageLocationTypeValue,
            Constants.MessageLocationType.DRAFT.messageLocationTypeValue,
        )

        for (i in labelIds.indices) {
            val item = labelIds[i]
            if (item.length <= 2) {
                val locationInt = item.toInt()
                if (locationInt in validLocations) {
                    return locationInt
                }
            } else {
                return Constants.MessageLocationType.LABEL_FOLDER.messageLocationTypeValue
            }
        }

        throw IllegalArgumentException("No valid location found in IDs: $labelIds ")
    }

}
