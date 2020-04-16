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
package ch.protonmail.android.activities.composeMessage

import android.text.Spanned
import android.text.SpannedString
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.room.messages.LocalAttachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.BigContentHolder
import ch.protonmail.android.core.Constants

class MessageBuilderData(
    val message: Message,
    val senderEmailAddress: String,
    val senderName: String,
    val messageTitle: String?,
    val content: String,
    val body: String,
    val largeMessageBody: Boolean,
    val isPGPMime: Boolean,
    val messageTimestamp: Long,
    val messageId: String?,
    val addressId: String,
    val addressEmailAlias: String?,
    val mBigContentHolder: BigContentHolder,
    val attachmentList: ArrayList<LocalAttachment>,
    val embeddedAttachmentsList: ArrayList<LocalAttachment>,
    var isDirty: Boolean,
    val signature: String,
    val mobileSignature: String,
    val messagePassword: String?,
    val passwordHint: String?,
    val isPasswordValid: Boolean,
    val expirationTime: Long?,
    val sendPreferences: Map<String, SendPreference>,
    val isRespondInlineButtonVisible: Boolean,
    val isRespondInlineChecked: Boolean,
    val showImages: Boolean,
    val showRemoteContent: Boolean,
    val offlineDraftSaved: Boolean,
    val initialMessageContent: String,
    val decryptedMessage: String,
    val isMessageBodyVisible: Boolean,
    val quotedHeader: Spanned,
    val uploadAttachments: Boolean,
    val isTransient: Boolean
) {
    class Builder {
        private lateinit var message: Message
        private lateinit var senderEmailAddress: String
        private lateinit var senderName: String
        private var messageTitle: String? = ""
        private var content: String = ""
        private var body: String = ""
        private var largeMessageBody: Boolean = false
        private var isPGPMime: Boolean = false
        private var messageTimestamp: Long = 0L
        private var messageId: String? = ""
        private var addressId: String = ""
        private var addressEmailAlias: String? = null
        private var mBigContentHolder: BigContentHolder = BigContentHolder()
        private var attachmentList: ArrayList<LocalAttachment> = ArrayList()
        private var embeddedAttachmentsList: ArrayList<LocalAttachment> = ArrayList()
        private var isDirty: Boolean = false
        private var signature: String = ""
        private var mobileSignature: String = ""
        private var messagePassword: String? = null
        private var passwordHint: String? = ""
        private var isPasswordValid: Boolean = true
        private var expirationTime: Long? = 0L
        private var sendPreferences: Map<String, SendPreference> = emptyMap()
        private var isRespondInlineButtonVisible: Boolean = true
        private var isRespondInlineChecked: Boolean = false
        private var showImages: Boolean = false
        private var showRemoteContent: Boolean = false
        private var offlineDraftSaved: Boolean = false
        private var initialMessageContent: String = ""
        private var decryptedMessage: String = ""
        private var isMessageBodyVisible: Boolean = false
        private var quotedHeader: Spanned = SpannedString("")
        private var uploadAttachments: Boolean = false
        private var isTransient: Boolean = false

        @Synchronized
        fun fromOld(oldObject: MessageBuilderData) = apply {
            this.message = oldObject.message
            this.senderEmailAddress = oldObject.senderEmailAddress
            this.senderName = oldObject.senderName
            this.messageTitle = oldObject.messageTitle
            this.content = oldObject.content
            this.body = oldObject.body
            this.largeMessageBody = oldObject.largeMessageBody
            this.isPGPMime = oldObject.isPGPMime
            this.messageTimestamp = oldObject.messageTimestamp
            this.messageId = oldObject.messageId
            this.addressId = oldObject.addressId
            this.addressEmailAlias = oldObject.addressEmailAlias
            this.mBigContentHolder = oldObject.mBigContentHolder
            this.attachmentList = oldObject.attachmentList
            this.embeddedAttachmentsList = oldObject.embeddedAttachmentsList

            this.isDirty = oldObject.isDirty
            this.signature = oldObject.signature
            this.mobileSignature = oldObject.mobileSignature

            this.messagePassword = oldObject.messagePassword
            this.passwordHint = oldObject.passwordHint
            this.isPasswordValid = oldObject.isPasswordValid
            this.expirationTime = oldObject.expirationTime
            this.sendPreferences = oldObject.sendPreferences
            this.isRespondInlineButtonVisible = oldObject.isRespondInlineButtonVisible
            this.isRespondInlineChecked = oldObject.isRespondInlineChecked
            this.showImages = oldObject.showImages
            this.showRemoteContent = oldObject.showRemoteContent
            this.offlineDraftSaved = oldObject.offlineDraftSaved
            this.initialMessageContent = oldObject.initialMessageContent
            this.isMessageBodyVisible = oldObject.isMessageBodyVisible
            this.quotedHeader = oldObject.quotedHeader

            this.uploadAttachments = oldObject.uploadAttachments
            this.isTransient = oldObject.isTransient
        }

        fun message(message: Message) = apply { this.message = message }

        fun decryptedMessage(decryptedMessage: String) =
            apply { this.decryptedMessage = decryptedMessage }

        fun senderEmailAddress(senderEmailAddress: String) =
            apply {
                this.senderEmailAddress = senderEmailAddress
                message.sender!!.emailAddress = senderEmailAddress
            }

        fun messageSenderName(senderName: String) = apply {
            this.senderName = senderName
            message.senderName = senderName
        }

        fun messageTitle(messageTitle: String?) = apply { this.messageTitle = messageTitle }

        fun content(content: String) = apply { this.content = content }

        fun body() = apply {
            val bodyTemp = if (message.isPGPMime) message.decryptedBody else content
            if (bodyTemp != null && bodyTemp.isNotEmpty() && bodyTemp.toByteArray().size > Constants.MAX_INTENT_STRING_SIZE) {
                this.mBigContentHolder.content = bodyTemp
                this.largeMessageBody = true
            } else {
                this.body = bodyTemp ?: ""
            }
        }

        fun isPGPMime(isPGPMime: Boolean) = apply { this.isPGPMime = isPGPMime }

        fun messageTimestamp(messageTimestamp: Long) =
            apply { this.messageTimestamp = messageTimestamp }

        fun messageId() = apply { this.messageId = message.messageId }

        fun addressId(addressId: String) = apply { this.addressId = addressId }

        fun addressEmailAlias(addressEmailAlias: String?) = apply { this.addressEmailAlias = addressEmailAlias }

        fun attachmentList(attachments: ArrayList<LocalAttachment>) = apply {
            this.attachmentList = attachments
        }

        fun embeddedAttachmentsList(embeddedAttachments: ArrayList<LocalAttachment>) =
            apply { this.embeddedAttachmentsList = embeddedAttachments }

        fun isDirty(isDirty: Boolean) = apply { this.isDirty = isDirty }

        fun signature(signature: String) = apply { this.signature = signature }

        fun mobileSignature(mobileSignature: String) =
            apply { this.mobileSignature = mobileSignature }

        fun messagePassword(messagePassword: String?) =
            apply { this.messagePassword = messagePassword }

        fun passwordHint(passwordHint: String?) = apply { this.passwordHint = passwordHint }

        fun isPasswordValid(isPasswordValid: Boolean) =
            apply { this.isPasswordValid = isPasswordValid }

        fun expirationTime(expirationTime: Long?) = apply { this.expirationTime = expirationTime }

        fun sendPreferences(sendPreferences: Map<String, SendPreference>) =
            apply { this.sendPreferences = sendPreferences }

        fun isRespondInlineButtonVisible(isRespondInlineButtonVisible: Boolean) =
            apply { this.isRespondInlineButtonVisible = isRespondInlineButtonVisible }

        fun isRespondInlineChecked(isRespondInlineChecked: Boolean) =
            apply { this.isRespondInlineChecked = isRespondInlineChecked }

        fun showImages(showImages: Boolean) =
            apply { this.showImages = showImages }

        fun showRemoteContent(showRemoteContent: Boolean) =
            apply { this.showRemoteContent = showRemoteContent }

        fun offlineDraftSaved(offlineDraftSaved: Boolean) =
            apply { this.offlineDraftSaved = offlineDraftSaved }

        fun initialMessageContent(initialMessageContent: String) =
            apply { this.initialMessageContent = initialMessageContent }

        fun isMessageBodyVisible(isMessageBodyVisible: Boolean) =
            apply { this.isMessageBodyVisible = isMessageBodyVisible }

        fun quotedHeader(quotedHeader: Spanned) =
            apply { this.quotedHeader = quotedHeader }

        fun uploadAttachments(uploadAttachments: Boolean) =
            apply { this.uploadAttachments = uploadAttachments }

        fun isTransient(isTransient: Boolean) =
            apply { this.isTransient = isTransient }

        fun build() : MessageBuilderData {
            return MessageBuilderData(
                    message,
                    senderEmailAddress,
                    senderName,
                    messageTitle,
                    content,
                    body,
                    largeMessageBody,
                    isPGPMime,
                    messageTimestamp,
                    messageId,
                    addressId,
                    addressEmailAlias,
                    mBigContentHolder,
                    attachmentList,
                    embeddedAttachmentsList,
                    isDirty,
                    signature,
                    mobileSignature,
                    messagePassword,
                    passwordHint,
                    isPasswordValid,
                    expirationTime,
                    sendPreferences,
                    isRespondInlineButtonVisible,
                    isRespondInlineChecked,
                    showImages,
                    showRemoteContent,
                    offlineDraftSaved,
                    initialMessageContent,
                    decryptedMessage,
                    isMessageBodyVisible,
                    quotedHeader,
                    uploadAttachments,
                    isTransient
            )
        }
    }
}
