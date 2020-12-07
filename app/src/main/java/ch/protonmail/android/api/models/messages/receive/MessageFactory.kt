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

import ch.protonmail.android.api.models.NewMessage
import ch.protonmail.android.api.models.enumerations.MessageFlag
import ch.protonmail.android.api.models.factories.checkIfSet
import ch.protonmail.android.api.models.factories.makeInt
import ch.protonmail.android.api.models.factories.parseBoolean
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.extensions.notNull
import javax.inject.Inject

class MessageFactory @Inject constructor(
    private val attachmentFactory: IAttachmentFactory,
    private val messageSenderFactory: IMessageSenderFactory
) : IMessageFactory {

    override fun createDraftApiMessage(message: Message): NewMessage {
        TODO("Not yet implemented")
    }

    override fun createServerMessage(message: Message): ServerMessage {
        return message.let {
            val serverMessage = ServerMessage()
            serverMessage.ID = it.messageId
            serverMessage.Subject = it.subject
            serverMessage.Unread = it.Unread.makeInt()

            serverMessage.Type = it.Type.ordinal
            serverMessage.Sender = it.sender?.let(messageSenderFactory::createServerMessageSender)
            serverMessage.Time = it.time
            serverMessage.Size = it.totalSize
            serverMessage.FolderLocation = it.folderLocation
            serverMessage.Starred = it.isStarred.makeInt()
            serverMessage.NumAttachments = it.numAttachments
            serverMessage.ExpirationTime = it.expirationTime
            serverMessage.SpamScore = it.spamScore
            serverMessage.AddressID = it.addressID
            serverMessage.Body = it.messageBody
            serverMessage.MIMEType = it.mimeType
            serverMessage.LabelIDs = it.allLabelIDs
            serverMessage.ToList = it.toList
            serverMessage.CCList = it.ccList
            serverMessage.BCCList = it.bccList
            serverMessage.ReplyTos = it.replyTos
            serverMessage.Header = it.header
            serverMessage.parsedHeaders = it.parsedHeaders
            serverMessage.Attachments = it.Attachments.map(attachmentFactory::createServerAttachment)
            serverMessage
        }
    }

    override fun createMessage(serverMessage: ServerMessage): Message {
        return serverMessage.let {
            val message = Message()
            message.messageId = it.ID
            message.subject = it.Subject
            message.Unread = it.Unread.parseBoolean("Unread")

            message.Type = MessageUtils.calculateType(it.Flags)

            val serverMessageSender = it.Sender ?: throw RuntimeException("Sender is not set")
            message.sender = messageSenderFactory.createMessageSender(serverMessageSender)
            message.time = it.Time.checkIfSet("Time")
            message.totalSize = it.Size.checkIfSet("Size")
            message.location = it.LabelIDs!!
                    .asSequence()
                    .filter { it.length <= 2 }
                    .map { it.toInt() }
                    .fold(Constants.MessageLocationType.ALL_MAIL.messageLocationTypeValue) { location, newLocation ->
                        if (newLocation !in listOf(Constants.MessageLocationType.STARRED.messageLocationTypeValue,
                                        Constants.MessageLocationType.ALL_MAIL.messageLocationTypeValue,
                                        Constants.MessageLocationType.INVALID.messageLocationTypeValue) &&
                                newLocation < location) {
                            newLocation
                        } else if (newLocation in listOf(Constants.MessageLocationType.DRAFT.messageLocationTypeValue,
                                        Constants.MessageLocationType.SENT.messageLocationTypeValue)) {
                            newLocation
                        } else
                            location
                    }
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
            message.Attachments = it.Attachments.map(attachmentFactory::createAttachment)
            message.embeddedImagesArray = it.embeddedImagesArray.toMutableList()
            val numOfAttachments = message.numAttachments
            val attachmentsListSize = message.Attachments.size
            if (attachmentsListSize != 0 && attachmentsListSize != numOfAttachments)
                throw RuntimeException("Attachments size does not match expected: $numOfAttachments, actual: $attachmentsListSize ")
            message
        }
    }

}
