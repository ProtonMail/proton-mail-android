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

import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.LocalAttachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.BigContentHolder
import ch.protonmail.android.core.Constants

class IntentExtrasData(
    val user: User,
    val userAddresses: List<Address>,
    val message: Message,
    val toRecipientListString: String,
    val messageCcList: String,
    val includeCCList: Boolean,
    val senderEmailAddress: String,
    val messageSenderName: String,
    val newMessageTitle: String?,
    val content: String,
    val body: String,
    val largeMessageBody: Boolean,
    val messageAction: Constants.MessageActionType,
    val imagesDisplayed: Boolean,
    val remoteContentDisplayed: Boolean,
    val isPGPMime: Boolean,
    val timeMs: Long,
    val messageIsEncrypted: Boolean,
    val messageId: String?,
    val addressID: String?,
    val addressEmailAlias: String?,
    val mBigContentHolder: BigContentHolder,
    val attachments: ArrayList<LocalAttachment>,
    val embeddedImagesAttachmentsExist: Boolean) {
    class Builder {
        private lateinit var user: User
        private lateinit var userAddresses: List<Address>
        private lateinit var message: Message
        private lateinit var toRecipientListString: String
        private lateinit var messageCcList: String
        private var includeCCList: Boolean = false
        private lateinit var senderEmailAddress: String
        private lateinit var messageSenderName: String
        private var newMessageTitle: String? = ""
        private lateinit var content: String
        private var body: String = ""
        private var largeMessageBody: Boolean = false
        private var messageAction = Constants.MessageActionType.REPLY
        private var imagesDisplayed: Boolean = false
        private var remoteContentDisplayed: Boolean = false
        private var isPGPMime: Boolean = false
        private var timeMs: Long = 0L
        private var messageIsEncrypted: Boolean = false
        private var messageId: String? = ""
        private var addressID: String? = ""
        private var addressEmailAlias: String? = ""
        private lateinit var mBigContentHolder: BigContentHolder
        private lateinit var attachments: ArrayList<LocalAttachment>
        private var embeddedImagesAttachmentsExist: Boolean = false
        fun user(user: User) = apply { this.user = user }
        fun userAddresses() = apply { this.userAddresses = user.addresses }
        fun message(message: Message) = apply { this.message = message }
        fun toRecipientListString(toRecipientListString: String) =
            apply { this.toRecipientListString = toRecipientListString }

        fun messageCcList() = apply { this.messageCcList = message.ccListString }
        fun includeCCList(includeCCList: Boolean) = apply { this.includeCCList = includeCCList }
        fun senderEmailAddress() =
            apply { this.senderEmailAddress = message.sender!!.emailAddress ?: "" }

        fun messageSenderName() = apply { this.messageSenderName = message.senderName ?: "" }
        fun newMessageTitle(newMessageTitle: String?) = apply {
            this.newMessageTitle =
                    newMessageTitle
        }

        fun content(content: String) = apply { this.content = content }
        fun mBigContentHolder(mBigContentHolder: BigContentHolder) =
            apply { this.mBigContentHolder = mBigContentHolder }

        fun body() = apply {
            val bodyTemp = if (message.isPGPMime) message.decryptedBody else content
            if (bodyTemp != null && bodyTemp.isNotEmpty() && bodyTemp.toByteArray().size > Constants.MAX_INTENT_STRING_SIZE) {
                this.mBigContentHolder.content = bodyTemp
                this.largeMessageBody = true
            } else {
                this.body = bodyTemp ?: ""
            }
        }

        fun messageAction(messageAction: Constants.MessageActionType) = apply { this.messageAction = messageAction }
        fun imagesDisplayed(imagesDisplayed: Boolean) =
            apply { this.imagesDisplayed = imagesDisplayed }
        fun remoteContentDisplayed(remoteContentDisplayed: Boolean) =
            apply { this.remoteContentDisplayed = remoteContentDisplayed }

        fun isPGPMime() = apply { this.isPGPMime = message.isPGPMime }
        fun timeMs() = apply { this.timeMs = message.timeMs }
        fun messageIsEncrypted() = apply { this.messageIsEncrypted = message.isEncrypted() }
        fun messageId() = apply { this.messageId = message.messageId }
        fun addressID() = apply { this.addressID = message.addressID }
        /**
         * This method extract user's email alias, but also normalizes it
         * so the non-alias part is equal to user's original address,
         * because API is case-sensitive when we send (not when we receive).
         */
        fun addressEmailAlias() = apply {
            val recipients = message.toList + message.ccList + message.bccList
            var originalAddress: String? = null
            val aliasAddress = recipients.find {
                if (it.address.contains("+")) {
                    val nonAliasAddress = "${it.address.substringBefore("+")}@${it.address.substringAfter("@")}"
                    val address = user.addresses.find { address -> address.email.equals(nonAliasAddress, ignoreCase = true) }
                    if (address != null) {
                        originalAddress = address.email
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }

            if (aliasAddress != null) {
                val aliasPart = aliasAddress.address.substringBefore("@").substringAfter("+")
                this.addressEmailAlias = "${(originalAddress as String).substringBefore("@")}+$aliasPart@${(originalAddress as String).substringAfter("@")}"
            } else {
                this.addressEmailAlias = null
            }
        }
        fun attachments(attachments: ArrayList<LocalAttachment>, embeddedImagesAttachments:
        MutableList<Attachment>?) = apply {
            if (!message.isPGPMime && messageAction == Constants.MessageActionType.FORWARD) {
                this.embeddedImagesAttachmentsExist = embeddedImagesAttachments != null &&
                        !embeddedImagesAttachments.isEmpty()
                this.attachments = attachments
            } else if (!message.isPGPMime && (messageAction == Constants.MessageActionType.REPLY || messageAction == Constants.MessageActionType.REPLY_ALL) && embeddedImagesAttachments != null) {
                val att =
                    ArrayList(LocalAttachment.createLocalAttachmentList(embeddedImagesAttachments))
                this.attachments = att
                this.embeddedImagesAttachmentsExist = true
            } else {
                this.attachments = ArrayList() // TODO temporary workaround for non-initialized lateinit crash
            }
        }

        fun build() = IntentExtrasData(
            user,
            userAddresses,
            message,
            toRecipientListString,
            messageCcList,
            includeCCList,
            senderEmailAddress,
            messageSenderName,
            newMessageTitle,
            content,
            body,
            largeMessageBody,
            messageAction,
            imagesDisplayed,
            remoteContentDisplayed,
            isPGPMime,
            timeMs,
            messageIsEncrypted,
            messageId,
            addressID,
            addressEmailAlias,
            mBigContentHolder,
            attachments,
            embeddedImagesAttachmentsExist
        )
    }
}
