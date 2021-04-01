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
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.RecipientType
import ch.protonmail.android.api.models.enumerations.MessageEncryption
import ch.protonmail.android.api.models.messages.ParsedHeaders
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.crypto.CipherText
import ch.protonmail.android.crypto.Crypto
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.crypto.KeyInformation
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.StringEscapeUtils
import timber.log.Timber
import java.io.Serializable
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.mail.internet.InternetHeaders

const val TABLE_MESSAGES = "messagev3"
const val COLUMN_MESSAGE_ID = "ID"
const val COLUMN_MESSAGE_SUBJECT = "Subject"
const val COLUMN_MESSAGE_TIME = "Time"
const val COLUMN_MESSAGE_LOCATION = "Location"
const val COLUMN_MESSAGE_FOLDER_LOCATION = "FolderLocation"
const val COLUMN_MESSAGE_TO_LIST = "ToList"
const val COLUMN_MESSAGE_CC_LIST = "CCList"
const val COLUMN_MESSAGE_BCC_LIST = "BCCList"
const val COLUMN_MESSAGE_IS_ENCRYPTED = "IsEncrypted"
const val COLUMN_MESSAGE_IS_STARRED = "Starred"
const val COLUMN_MESSAGE_SIZE = "Size"
const val COLUMN_MESSAGE_IS_REPLIED = "IsReplied"
const val COLUMN_MESSAGE_IS_REPLIED_ALL = "IsRepliedAll"
const val COLUMN_MESSAGE_IS_FORWARDED = "IsForwarded"
const val COLUMN_MESSAGE_BODY = "Body"
const val COLUMN_MESSAGE_INLINE_RESPONSE = "InlineResponse"
const val COLUMN_MESSAGE_LABELS = "LabelIDs"
const val COLUMN_MESSAGE_LOCAL_ID = "NewServerId"
const val COLUMN_MESSAGE_MIME_TYPE = "MIMEType"
const val COLUMN_MESSAGE_SPAM_SCORE = "SpamScore"
const val COLUMN_MESSAGE_ADDRESS_ID = "AddressID"
const val COLUMN_MESSAGE_IS_DOWNLOADED = "IsDownloaded"
const val COLUMN_MESSAGE_EXPIRATION_TIME = "ExpirationTime"
const val COLUMN_MESSAGE_HEADER = "Header"
const val COLUMN_MESSAGE_PARSED_HEADERS = "ParsedHeaders"
const val COLUMN_MESSAGE_NUM_ATTACHMENTS = "NumAttachments"
const val COLUMN_MESSAGE_UNREAD = "Unread"
const val COLUMN_MESSAGE_TYPE = "Type"
const val COLUMN_MESSAGE_SENDER_EMAIL = "SenderSerialized"
const val COLUMN_MESSAGE_SENDER_NAME = "SenderName"
const val COLUMN_MESSAGE_PREFIX_SENDER = "Sender_"
const val COLUMN_MESSAGE_REPLY_TOS = "ReplyTos"
const val COLUMN_MESSAGE_ACCESS_TIME = "AccessTime"
const val COLUMN_MESSAGE_DELETED = "Deleted"

@Entity(
    tableName = TABLE_MESSAGES,
    indices = [Index(COLUMN_MESSAGE_ID, unique = true), Index(COLUMN_MESSAGE_LOCATION)]
)
data class Message @JvmOverloads constructor(

    @ColumnInfo(name = COLUMN_MESSAGE_ID)
    var messageId: String? = null,

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
    var messageEncryption: MessageEncryption? = null,

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
    var allLabelIDs: List<String> = listOf(),

    @JvmSuppressWildcards
    @ColumnInfo(name = COLUMN_MESSAGE_TO_LIST)
    var toList: List<MessageRecipient> = listOf(),

    @JvmSuppressWildcards
    @ColumnInfo(name = COLUMN_MESSAGE_REPLY_TOS)
    var replyTos: List<MessageRecipient> = mutableListOf(),

    @JvmSuppressWildcards
    @ColumnInfo(name = COLUMN_MESSAGE_CC_LIST)
    var ccList: List<MessageRecipient> = mutableListOf(),

    @JvmSuppressWildcards
    @ColumnInfo(name = COLUMN_MESSAGE_BCC_LIST)
    var bccList: List<MessageRecipient> = listOf(),

    @ColumnInfo(name = COLUMN_MESSAGE_DELETED)
    var deleted: Boolean = false,

    @Embedded(prefix = COLUMN_MESSAGE_PREFIX_SENDER)
    var sender: MessageSender? = MessageSender(null, null)

) : Serializable {

    @Ignore
    var Attachments = listOf<Attachment>() // TODO change the name to lowercase, look out for getter naming conflicts
        internal set

    @Ignore
    var decryptedHTML: String? = null
        private set

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

    val toListString get() =
        MessageUtils.toContactString(toList)

    val toListStringGroupsAware get() =
        MessageUtils.toContactsAndGroupsString(toList)

    val ccListString get() =
        MessageUtils.toContactString(ccList)

    val bccListString: String get() =
        MessageUtils.toContactString(bccList)

    fun locationFromLabel(): Constants.MessageLocationType =
        allLabelIDs
            .asSequence()
            .filter { it.length <= 2 }
            .map { Constants.MessageLocationType.fromInt(it.toInt()) }
            .fold(Constants.MessageLocationType.STARRED) { location, newLocation ->

                if (newLocation !in listOf(
                        Constants.MessageLocationType.STARRED,
                        Constants.MessageLocationType.ALL_MAIL,
                        Constants.MessageLocationType.INVALID
                    ) && newLocation.messageLocationTypeValue < location.messageLocationTypeValue
                ) {
                    newLocation

                } else if (newLocation in listOf(
                        Constants.MessageLocationType.DRAFT,
                        Constants.MessageLocationType.SENT
                    )
                ) {
                    newLocation

                } else {
                    location
                }
            }

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

    fun writeTo(message: Message) {
        message.messageBody = messageBody
        message.embeddedImageIds = embeddedImageIds
        message.numAttachments = numAttachments
        val attachments = Attachments
        attachments.forEach { attachment ->
            attachment.isUploaded = true
            attachment.isNew = false
        }
        message.replyTos = replyTos
        message.isDownloaded = isDownloaded
        message.sender = sender
        message.setAttachmentList(attachments)
        message.setIsEncrypted(getIsEncrypted())
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

    fun getIsEncrypted(): MessageEncryption? = messageEncryption

    @Deprecated("Use getMessageEncryption()")
    fun isEncrypted(): Boolean = messageEncryption!!.isEndToEndEncrypted

    @WorkerThread
    fun attachmentsBlocking(messageDao: MessageDao): List<Attachment> = runBlocking {
        attachments(messageDao)
    }

    suspend fun attachments(messageDao: MessageDao): List<Attachment> {
        if (isPGPMime) {
            return this.Attachments
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

        Attachments = result
        return result
    }

    @MainThread
    fun getAttachmentsAsync(messageDao: MessageDao): LiveData<List<Attachment>> {
        if (isPGPMime) {
            val result = MutableLiveData<List<Attachment>>()
            result.value = this.Attachments
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

    fun setIsEncrypted(isEncrypted: MessageEncryption?) {
        this.messageEncryption = isEncrypted
    }

    fun setIsRead(isRead: Boolean) {
        this.Unread = !isRead
    }

    /**
     * @throws Exception
     */
    @JvmOverloads
    fun decrypt(userManager: UserManager, userId: Id, verKeys: List<KeyInformation>? = null) {
        val addressId = Id(checkNotNull(addressID))
        val addressCrypto = Crypto.forAddress(userManager, userId, addressId)
        decrypt(addressCrypto, verKeys)
    }

    private fun decryptMime(addressCrypto: AddressCrypto, keys: List<ByteArray>? = null) {
        val messageBody = checkNotNull(messageBody)
        val mimeDecryptor = addressCrypto.decryptMime(CipherText(messageBody))
        var body: String? = null
        var mimetype: String? = null
        var exception: Exception? = null
        val attachments = ArrayList<Attachment>()
        mimeDecryptor.onBody = { eventbody: String, eventmimetype: String ->
            body = eventbody
            mimetype = eventmimetype
        }
        mimeDecryptor.onError = {
            exception = it
        }
        if (keys != null && keys.isNotEmpty()) {
            for (key in keys) {
                mimeDecryptor.withVerificationKey(key)
            }
            mimeDecryptor.onVerified = { hasSig: Boolean, hasValidSig: Boolean ->
                hasValidSignature = hasSig && hasValidSig
                hasInvalidSignature = hasSig && !hasValidSig
            }
        }
        var count = 0
        mimeDecryptor.onAttachment = { headers: InternetHeaders, content: ByteArray ->
            attachments.add(Attachment.fromMimeAttachment(content, headers, messageId!!, count++))
        }
        mimeDecryptor.withMessageTime(time)
        mimeDecryptor.start()
        mimeDecryptor.await()

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

    fun searchForLocation(location: Int) = allLabelIDs
        .asSequence()
        .filter { it.length <= 2 }
        .map { Integer.valueOf(it) }
        .contains(location)

    fun setAttachmentList(attachmentList: List<Attachment>) {
        Attachments = attachmentList
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

    fun setFolderLocation(messageDao: MessageDao) {
        for (labelId in allLabelIDs) {
            val label = messageDao.findLabelById(labelId)
            if (label != null && label.exclusive) {
                folderLocation = label.id
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

    enum class MessageType {
        INBOX, DRAFT, SENT, INBOX_AND_SENT
    }
}
