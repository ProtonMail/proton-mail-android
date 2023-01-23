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
package ch.protonmail.android.data.local.model

import android.provider.BaseColumns
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import ch.protonmail.android.api.models.MessagePayload
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.RecipientType
import ch.protonmail.android.api.models.enumerations.MessageEncryption
import ch.protonmail.android.api.models.enumerations.MessageFlag
import ch.protonmail.android.api.models.enumerations.contains
import ch.protonmail.android.api.models.messages.ParsedHeaders
import ch.protonmail.android.api.models.messages.receive.MessageLocationResolver
import ch.protonmail.android.api.models.messages.receive.ServerMessageSender
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.crypto.CipherText
import ch.protonmail.android.crypto.Crypto
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.domain.util.checkNotBlank
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.crypto.KeyInformation
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.util.kotlin.toInt
import org.apache.commons.lang3.StringEscapeUtils
import timber.log.Timber
import java.io.Serializable
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.mail.internet.InternetHeaders

const val TABLE_MESSAGES = "messagev3"
const val COLUMN_CONVERSATION_ID = "ConversationID"
const val COLUMN_MESSAGE_ACCESS_TIME = "AccessTime"
const val COLUMN_MESSAGE_ADDRESS_ID = "AddressID"
const val COLUMN_MESSAGE_BCC_LIST = "BCCList"
const val COLUMN_MESSAGE_BODY = "Body"
const val COLUMN_MESSAGE_CC_LIST = "CCList"
const val COLUMN_MESSAGE_EXPIRATION_TIME = "ExpirationTime"
const val COLUMN_MESSAGE_FLAGS = "Flags"
const val COLUMN_MESSAGE_FOLDER_LOCATION = "FolderLocation"
const val COLUMN_MESSAGE_HEADER = "Header"
const val COLUMN_MESSAGE_ID = "ID"
const val COLUMN_MESSAGE_INLINE_RESPONSE = "InlineResponse"
const val COLUMN_MESSAGE_IS_DOWNLOADED = "IsDownloaded"
const val COLUMN_MESSAGE_IS_ENCRYPTED = "IsEncrypted"
const val COLUMN_MESSAGE_IS_FORWARDED = "IsForwarded"
const val COLUMN_MESSAGE_IS_REPLIED = "IsReplied"
const val COLUMN_MESSAGE_IS_REPLIED_ALL = "IsRepliedAll"
const val COLUMN_MESSAGE_IS_STARRED = "Starred"
const val COLUMN_MESSAGE_LABELS = "LabelIDs"
const val COLUMN_MESSAGE_LOCAL_ID = "NewServerId"
const val COLUMN_MESSAGE_LOCATION = "Location"
const val COLUMN_MESSAGE_MIME_TYPE = "MIMEType"
const val COLUMN_MESSAGE_NUM_ATTACHMENTS = "NumAttachments"
const val COLUMN_MESSAGE_PARSED_HEADERS = "ParsedHeaders"
const val COLUMN_MESSAGE_PREFIX_SENDER = "Sender_"
const val COLUMN_MESSAGE_REPLY_TOS = "ReplyTos"
const val COLUMN_MESSAGE_SENDER_EMAIL = "SenderSerialized"
const val COLUMN_MESSAGE_SENDER_NAME = "SenderName"
const val COLUMN_MESSAGE_SENDER_IS_PROTON = "IsProton"
const val COLUMN_MESSAGE_SIZE = "Size"
const val COLUMN_MESSAGE_SPAM_SCORE = "SpamScore"
const val COLUMN_MESSAGE_SUBJECT = "Subject"
const val COLUMN_MESSAGE_TIME = "Time"
const val COLUMN_MESSAGE_TO_LIST = "ToList"
const val COLUMN_MESSAGE_TYPE = "Type"
const val COLUMN_MESSAGE_UNREAD = "Unread"
const val COLUMN_MESSAGE_ORDER = "Order"

@Entity(
    tableName = TABLE_MESSAGES,
    indices = [Index(COLUMN_MESSAGE_ID, unique = true), Index(COLUMN_MESSAGE_LOCATION), Index(COLUMN_CONVERSATION_ID)]
)
data class Message @JvmOverloads constructor(

    @ColumnInfo(name = COLUMN_MESSAGE_ID)
    var messageId: String? = null,

    @ColumnInfo(name = COLUMN_CONVERSATION_ID)
    var conversationId: String? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_SUBJECT)
    var subject: String? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_UNREAD)
    var Unread: Boolean = false,

    @ColumnInfo(name = COLUMN_MESSAGE_TYPE)
    var Type: MessageType = MessageType.INBOX, // 0 = INBOX, 1 = DRAFT, 2 = SENT, 3 = INBOX_AND_SENT

    @ColumnInfo(name = COLUMN_MESSAGE_TIME)
    var time: Long = 0,

    @ColumnInfo(name = COLUMN_MESSAGE_SIZE)
    var totalSize: Long = 0,

    @ColumnInfo(name = COLUMN_MESSAGE_LOCATION)
    var location: Int = -1,

    @ColumnInfo(name = COLUMN_MESSAGE_FOLDER_LOCATION)
    var folderLocation: String? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_IS_STARRED)
    var isStarred: Boolean? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_NUM_ATTACHMENTS)
    var numAttachments: Int = 0,

    @ColumnInfo(name = COLUMN_MESSAGE_IS_ENCRYPTED)
    var messageEncryption: MessageEncryption = MessageEncryption.UNKNOWN,

    @ColumnInfo(name = COLUMN_MESSAGE_EXPIRATION_TIME)
    var expirationTime: Long = 0,

    @ColumnInfo(name = COLUMN_MESSAGE_IS_REPLIED)
    var isReplied: Boolean? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_IS_REPLIED_ALL)
    var isRepliedAll: Boolean? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_IS_FORWARDED)
    var isForwarded: Boolean? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_BODY)
    var messageBody: String? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_IS_DOWNLOADED)
    var isDownloaded: Boolean = false,

    @ColumnInfo(name = COLUMN_MESSAGE_ADDRESS_ID)
    var addressID: String? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_INLINE_RESPONSE)
    var isInline: Boolean = false,

    @ColumnInfo(name = COLUMN_MESSAGE_LOCAL_ID)
    var localId: String? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_MIME_TYPE)
    var mimeType: String? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_SPAM_SCORE)
    var spamScore: Int = 0,

    @ColumnInfo(name = COLUMN_MESSAGE_ACCESS_TIME)
    var accessTime: Long = 0,

    @ColumnInfo(name = COLUMN_MESSAGE_HEADER)
    var header: String? = null,

    @SerializedName(COLUMN_MESSAGE_PARSED_HEADERS)
    @ColumnInfo(name = COLUMN_MESSAGE_PARSED_HEADERS)
    var parsedHeaders: ParsedHeaders? = null,

    @ColumnInfo(name = COLUMN_MESSAGE_LABELS)
    var allLabelIDs: List<String> = emptyList(),

    @JvmSuppressWildcards
    @ColumnInfo(name = COLUMN_MESSAGE_TO_LIST)
    var toList: List<MessageRecipient> = emptyList(),

    @JvmSuppressWildcards
    @ColumnInfo(name = COLUMN_MESSAGE_REPLY_TOS)
    var replyTos: List<MessageRecipient> = mutableListOf(),

    @JvmSuppressWildcards
    @ColumnInfo(name = COLUMN_MESSAGE_CC_LIST)
    var ccList: List<MessageRecipient> = mutableListOf(),

    @JvmSuppressWildcards
    @ColumnInfo(name = COLUMN_MESSAGE_BCC_LIST)
    var bccList: List<MessageRecipient> = emptyList(),

    @Embedded(prefix = COLUMN_MESSAGE_PREFIX_SENDER)
    var sender: MessageSender? = MessageSender(null, null),

    @ColumnInfo(name = COLUMN_MESSAGE_FLAGS, defaultValue = "0")
    val flags: Long = 0,

    @ColumnInfo(name = COLUMN_MESSAGE_ORDER, defaultValue = Long.MAX_VALUE.toString())
    val order: Long = Long.MAX_VALUE

    ) : Serializable {

    @Ignore
    var attachments = listOf<Attachment>()
        internal set

    @Ignore
    var decryptedHTML: String? = null

    @Ignore
    var decryptedBody: String? = null

    @Ignore
    var hasValidSignature: Boolean = false

    @Ignore
    var hasInvalidSignature: Boolean = false

    @Ignore
    var embeddedImageIds = listOf<String>()

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = BaseColumns._ID)
    var dbId: Long? = null

    val labelIDsNotIncludingLocations: List<String>
        get() = allLabelIDs.asSequence().filter { it.length > 2 }.distinct().toList()

    // API returns time in seconds so we convert to milliseconds
    val timeMs: Long
        get() = TimeUnit.SECONDS.toMillis(time)

    val senderEmail: String
        get() = sender?.emailAddress ?: ""

    var senderName: String?
        get() = sender?.name
        set(senderName) {
            sender = sender?.copy(name = senderName) ?: MessageSender(
                senderName,
                null
            )
        }

    @Ignore
    var senderDisplayName: String? = null

    val isRead: Boolean
        get() {
            return !Unread
        }

    val isPGPMime: Boolean
        get() =
            messageEncryption in listOf(MessageEncryption.MIME_PGP) || Constants.MIME_TYPE_MULTIPART_MIXED == mimeType

    val replyToEmails: List<String>
        get() = replyTos
            .asSequence()
            .filter { it.emailAddress.isNotEmpty() }
            .map { it.emailAddress }
            .toList()

    val toListString
        get() =
            MessageUtils.toContactString(toList)

    val toListStringGroupsAware
        get() =
            MessageUtils.toContactsAndGroupsString(toList)

    val ccListString
        get() =
            MessageUtils.toContactString(ccList)

    val bccListString: String
        get() =
            MessageUtils.toContactString(bccList)

    val isSent: Boolean
        get() {
            if (Type in listOf(MessageType.SENT, MessageType.INBOX_AND_SENT)) {
                return true
            }
            val app = ProtonMailApplication.getApplication()
            val userManager = app.userManager
            val user = userManager.currentLegacyUser
            return user!!.addresses!!.any { it.email.equals(senderEmail, ignoreCase = true) }
        }

    val isScheduled: Boolean
        get() {
            return allLabelIDs.contains(MessageLocationType.ALL_SCHEDULED.asLabelIdString())
        }
    
    fun locationFromLabel(labelRepository: LabelRepository? = null): Constants.MessageLocationType =
        MessageLocationResolver(labelRepository).resolveLocationFromLabels(allLabelIDs)

    fun writeTo(message: Message) {
        message.messageBody = messageBody
        message.embeddedImageIds = embeddedImageIds
        message.numAttachments = numAttachments
        val attachments = attachments
        attachments.forEach { attachment ->
            attachment.isUploaded = true
            attachment.isNew = false
        }
        message.replyTos = replyTos
        message.isDownloaded = isDownloaded
        message.sender = sender
        message.setAttachmentList(attachments)
        message.messageEncryption = messageEncryption
        message.mimeType = mimeType
        message.spamScore = spamScore
        message.Type = Type
        message.header = header
        val parsedHeaders = parsedHeaders
        if (parsedHeaders != null) {
            message.parsedHeaders = parsedHeaders
        }
    }

    fun setEmbeddedImagesArray(decryptedMessage: String) {
        val pattern = Pattern.compile("cid:[\\w$&+,:;=?@#|'<>.^*()%!\\-]*")
        val matcher = pattern.matcher(decryptedMessage)
        val embedded: MutableList<String> = ArrayList()
        while (matcher.find()) {
            val match = matcher.group()
            embedded.add(match.removePrefix("cid:"))
        }
        embeddedImageIds = embedded
    }

    fun setLabelIDs(labelIDs: List<String>?) {
        allLabelIDs = labelIDs ?: ArrayList()
        location = locationFromLabel().messageLocationTypeValue
    }

    @WorkerThread
    @Deprecated("We target removing all logic from Models. `MessageDao` can be used to get attachments instead")
    fun attachmentsBlocking(messageDao: MessageDao): List<Attachment> = runBlocking {
        attachments(messageDao)
    }

    @Deprecated("We target removing all logic from Models. `MessageDao` can be used to get attachments instead")
    suspend fun attachments(messageDao: MessageDao): List<Attachment> {
        if (isPGPMime) {
            return this.attachments
        }
        val messageId = messageId
        if (messageId == null || messageId.isEmpty()) {
            return emptyList()
        }
        val result = messageDao.findAttachmentsByMessageId(messageId).first()
        for (att in result) {
            val oldInline = att.inline
            if (!att.inline) {
                att.setMessage(this)
            }
            if (att.inline != oldInline) {
                messageDao.saveAttachment(att)
            }
        }

        attachments = result
        return result
    }

    @MainThread
    @Deprecated("We target removing all logic from Models. `MessageDao` can be used to get attachments instead")
    fun getAttachmentsAsync(messageDao: MessageDao): LiveData<List<Attachment>> {
        if (isPGPMime) {
            val result = MutableLiveData<List<Attachment>>()
            result.value = this.attachments
            return result
        }
        val messageId = messageId
        if (messageId == null || messageId.isEmpty()) {

            val result = MutableLiveData<List<Attachment>>()
            result.value = emptyList()
            return result
        }
        return messageDao.findAttachmentsByMessageIdAsync(messageId)
    }

    fun setIsRead(isRead: Boolean) {
        this.Unread = !isRead
    }

    /**
     * @throws Exception
     */
    @JvmOverloads
    @Deprecated("This logic should be extracted to a testable component for any new usages. Tracked in MAILAND-1566")
    fun decrypt(userManager: UserManager, userId: UserId, verKeys: List<KeyInformation>? = null) {
        val addressId = AddressId(checkNotNull(addressID))
        val addressCrypto = Crypto.forAddress(userManager, userId, addressId)
        decrypt(addressCrypto, verKeys)
    }

    private fun decryptMime(addressCrypto: AddressCrypto, keys: List<ByteArray>? = null) {
        val messageBody = checkNotNull(messageBody)
        var body: String? = null
        var mimetype: String? = null
        var exception: Exception? = null
        val attachments = ArrayList<Attachment>()
        var attachmentCount = 0
        addressCrypto.decryptMime(
            message = CipherText(messageBody),
            onBody = { eventBody: String, eventMimeType: String ->
                body = eventBody
                mimetype = eventMimeType
            },
            onError = {
                exception = it
            },
            onVerified = { hasSig: Boolean, hasValidSig: Boolean ->
                hasValidSignature = hasSig && hasValidSig
                hasInvalidSignature = hasSig && !hasValidSig
            },
            onAttachment = { headers: InternetHeaders, content: ByteArray ->
                attachments.add(Attachment.fromMimeAttachment(content, headers, messageId!!, attachmentCount++))
            },
            keys = keys,
            time = time
        )

        if (exception != null) {
            throw exception!!
        }

        decryptedBody = body
        if (Constants.MIME_TYPE_PLAIN_TEXT == mimetype) {
            body = StringEscapeUtils.escapeHtml4(body)
            body = body!!.replace("(\r\n|\r|\n|\n\r)".toRegex(), "<br>")
            body = UiUtil.createLinks(body)
        }

        setEmbeddedImagesArray(body!!)

        decryptedHTML = body

        setAttachmentList(attachments)
        numAttachments = attachments.size
    }

    /**
     * @throws Exception
     */
    @JvmOverloads
    @Deprecated("This logic should be extracted to a testable component for any new usages. Tracked in MAILAND-1566")
    fun decrypt(addressCrypto: AddressCrypto, verKeys: List<KeyInformation>? = null) {
        var decryptedMessage: String
        val keys = ArrayList<ByteArray>()
        if (verKeys != null) {
            for (ki in verKeys) {
                if (ki.isValid && !ki.isCompromised) {
                    keys.add(ki.publicKey)
                }
            }
        }
        val messageBody = messageBody
        try {
            // We assert that 'messageBody' is not null here, because otherwise 'decrypt' would throw anyway
            messageBody!!
            if (isPGPMime) {
                decryptMime(addressCrypto, keys)
                return
            }
            val tct = if (verKeys != null) {
                val fromArmor = CipherText(messageBody)
                addressCrypto.decrypt(fromArmor, keys, time)
            } else {
                addressCrypto.decrypt(CipherText(messageBody))
            }
            val hasSense = verKeys != null && verKeys.isNotEmpty() && tct.hasSignature()
            hasValidSignature = hasSense && tct.isSignatureValid
            hasInvalidSignature = hasSense && !tct.isSignatureValid
            decryptedMessage = tct.decryptedData
        } catch (e: Exception) {
            Timber.i(e, "decrypt error verkeys size: ${verKeys?.size}, keys size: ${keys.size}")
            decryptedBody = messageBody
            decryptedHTML = messageBody
            throw e
        }

        decryptedBody = decryptedMessage
        val mimeType = this.mimeType

        if (Constants.MIME_TYPE_PLAIN_TEXT == mimeType) {
            decryptedMessage = StringEscapeUtils.escapeHtml4(decryptedMessage)
            decryptedMessage = decryptedMessage.replace("(\r\n|\r|\n|\n\r)".toRegex(), "<br>")
            decryptedMessage = UiUtil.createLinks(decryptedMessage)
        }

        setEmbeddedImagesArray(decryptedMessage)
        decryptedHTML = decryptedMessage
    }

    fun setAttachmentList(attachmentList: List<Attachment>) {
        attachments = attachmentList
    }

    fun getEventLabelIDs() = allLabelIDs

    fun addLabels(addedLabelsList: List<String>) {
        allLabelIDs = HashSet(allLabelIDs).apply { addAll(addedLabelsList) }.toList()
        location = locationFromLabel().messageLocationTypeValue
    }

    fun removeLabels(removedLabelsList: List<String>) {
        allLabelIDs = HashSet(allLabelIDs).apply { removeAll(removedLabelsList) }.toList()
        location = locationFromLabel().messageLocationTypeValue
    }

    fun setFolderLocation(labelRepository: LabelRepository) {
        for (labelId in allLabelIDs) {
            runBlocking {
                val label = labelRepository.findLabel(LabelId(labelId))
                if (label != null && label.type == LabelType.FOLDER) {
                    folderLocation = label.id.id
                }
            }
        }
    }

    fun calculateLocation() {
        location = locationFromLabel().messageLocationTypeValue
        isStarred = allLabelIDs
            .asSequence()
            .filter { it.length <= 2 }
            .map { Constants.MessageLocationType.fromInt(it.toInt()) }
            .contains(Constants.MessageLocationType.STARRED)
    }

    @WorkerThread
    fun checkIfAttHeadersArePresent(messageDao: MessageDao): Boolean =
        attachmentsBlocking(messageDao).asSequence().map(Attachment::headers).any { it == null }

    fun getList(recipientType: RecipientType): List<MessageRecipient> {
        return when (recipientType) {
            RecipientType.TO -> toList
            RecipientType.CC -> ccList
            RecipientType.BCC -> bccList
        }
    }

    fun isSenderEmailAlias() = senderEmail.contains("+")

    fun toApiPayload(): MessagePayload {
        val serverSender = run {
            val sender = checkNotNull(sender) { "Sender is required to create a Payload" }
            val senderAddress = checkNotNull(sender.emailAddress) { "Sender address is required to create a Payload" }
            ServerMessageSender(name = sender.name, address = senderAddress)
        }

        return MessagePayload(
            sender = serverSender,
            body = checkNotBlank(messageBody) { "An encrypted message body is required to create a Payload" },
            id = messageId,
            subject = subject,
            toList = toList,
            ccList = ccList,
            bccList = bccList,
            unread = Unread.toInt()
        )
    }

    fun isDraft() = allLabelIDs.any { labelId ->
        labelId in listOf(
            MessageLocationType.DRAFT.asLabelIdString(),
            MessageLocationType.ALL_DRAFT.asLabelIdString()
        )
    }

    fun isPhishing(): Boolean =
        MessageFlag.PHISHING_AUTO in flags || MessageFlag.PHISHING_MANUAL in flags

    enum class MessageType {
        INBOX, DRAFT, SENT, INBOX_AND_SENT
    }
}
