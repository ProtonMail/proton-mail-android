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
import android.text.TextUtils
import android.util.Base64
import android.webkit.URLUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.utils.AppUtil
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Formatter
import java.util.Random
import javax.mail.MessagingException
import javax.mail.Part
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimeBodyPart

const val TABLE_ATTACHMENTS = "attachmentv3"
const val COLUMN_ATTACHMENT_ID = "attachment_id"
const val COLUMN_ATTACHMENT_MESSAGE_ID = "message_id"
const val COLUMN_ATTACHMENT_UPLOADING = "uploading"
const val COLUMN_ATTACHMENT_UPLOADED = "uploaded"
const val COLUMN_ATTACHMENT_HEADERS = "headers"
const val COLUMN_ATTACHMENT_SIGNATURE = "signature"
const val COLUMN_ATTACHMENT_FILE_NAME = "file_name"
const val COLUMN_ATTACHMENT_MIME_TYPE = "mime_type"
const val COLUMN_ATTACHMENT_FILE_SIZE = "file_size"
const val COLUMN_ATTACHMENT_KEY_PACKETS = "key_packets"
const val COLUMN_ATTACHMENT_FILE_PATH = "file_path"
const val COLUMN_ATTACHMENT_MIME_DATA = "mime_data"
const val COLUMN_ATTACHMENT_IS_INLINE = "is_inline"
const val FIELD_ATTACHMENT_HEADERS = "Headers"

@Entity(
    tableName = TABLE_ATTACHMENTS,
    indices = [Index(COLUMN_ATTACHMENT_ID, unique = true)]
)
data class Attachment @JvmOverloads constructor(

    @ColumnInfo(name = COLUMN_ATTACHMENT_ID)
    var attachmentId: String? = null,

    @ColumnInfo(name = COLUMN_ATTACHMENT_FILE_NAME)
    var fileName: String? = null,

    @ColumnInfo(name = COLUMN_ATTACHMENT_MIME_TYPE)
    var mimeType: String? = null,

    @ColumnInfo(name = COLUMN_ATTACHMENT_FILE_SIZE)
    var fileSize: Long = 0,

    @ColumnInfo(name = COLUMN_ATTACHMENT_KEY_PACKETS)
    var keyPackets: String? = null,

    @ColumnInfo(name = COLUMN_ATTACHMENT_MESSAGE_ID)
    var messageId: String = "",

    @ColumnInfo(name = COLUMN_ATTACHMENT_UPLOADED)
    var isUploaded: Boolean = false,

    @ColumnInfo(name = COLUMN_ATTACHMENT_UPLOADING)
    var isUploading: Boolean = false,

    @ColumnInfo(name = COLUMN_ATTACHMENT_SIGNATURE)
    var signature: String? = null,

    @SerializedName(FIELD_ATTACHMENT_HEADERS)
    @ColumnInfo(name = COLUMN_ATTACHMENT_HEADERS)
    var headers: AttachmentHeaders? = null,

    @Ignore
    var isNew: Boolean = true,

    @ColumnInfo(name = COLUMN_ATTACHMENT_IS_INLINE)
    var inline: Boolean = false,

    /**
     * filePath used to store attached file path for drafts
     * at time to upload it will read file and upload
     */
    @ColumnInfo(name = COLUMN_ATTACHMENT_FILE_PATH)
    @Expose(serialize = false, deserialize = false)
    var filePath: String? = null,

    @ColumnInfo(name = COLUMN_ATTACHMENT_MIME_DATA)
    @Expose(serialize = false, deserialize = false)
    var mimeData: ByteArray? = null

) : Serializable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = BaseColumns._ID)
    var dbId: Long? = null

    val isPGPAttachment: Boolean
        get() = attachmentId!!.startsWith("PGPAttachment")

    val doesFileExist: Boolean
        get() = File(filePath).exists()

    /**
     * In case there are more values delimited with semicolon, returns only the first one.
     */
    val mimeTypeFirstValue: String?
        get() = mimeType?.substringBefore(";")

    fun setMessage(message: Message?) {
        if (message != null) {
            this.messageId = message.messageId ?: ""
            this.inline = isInline(message.embeddedImageIds)
        }
    }

    private fun isInline(embeddedImagesArray: List<String>): Boolean {
        val headers = headers ?: return false
        val contentDisposition = headers.contentDisposition
        var contentId = if (headers.contentId.isNullOrEmpty()) {
            headers.contentLocation
        } else {
            headers.contentId
        }
        contentId = contentId?.removeSurrounding("<", ">")
        val embeddedMimeTypes = listOf("image/gif", "image/jpeg", "image/png", "image/bmp")
        var containsInline = false
        for (element in contentDisposition) {
            if (element.contains("inline")) {
                containsInline = true
                break
            }
        }

        return contentDisposition != null &&
            (containsInline || embeddedImagesArray.contains(contentId?.removeSurrounding("<", ">"))) &&
            embeddedMimeTypes.contains(mimeType)
    }

    fun deleteLocalFile() {
        if (doesFileExist) {
            // filePath can't be null if doesFileExist
            File(checkNotNull(filePath)).delete()
        }
    }

    fun getFileContent(): ByteArray {
        val filePath = filePath
            ?: return byteArrayOf()

        return if (URLUtil.isDataUrl(filePath)) {
            Base64.decode(
                filePath.split(",")[1],
                Base64.DEFAULT
            )
        } else {
            val file = File(filePath)
            AppUtil.getByteArray(file)
        }
    }

    companion object {
        private fun fromLocalAttachment(
            messageDao: MessageDao,
            localAttachment: LocalAttachment,
            index: Long,
            useRandomIds: Boolean
        ): Attachment {
            if (!TextUtils.isEmpty(localAttachment.attachmentId)) {
                val savedAttachment = messageDao.findAttachmentById(localAttachment.attachmentId)
                if (savedAttachment != null)
                    return savedAttachment
            }
            val attachmentId = if (useRandomIds || localAttachment.attachmentId.isEmpty()) {
                getRandomAttachmentId(index)
            } else {
                localAttachment.attachmentId
            }
            localAttachment.attachmentId = attachmentId

            val uri = localAttachment.uri
            val uriString = uri.toString()
            val filePath = if (URLUtil.isNetworkUrl(uriString) || URLUtil.isDataUrl(uriString)) {
                uriString
            } else {
                uri.path
            }

            return Attachment(
                attachmentId = attachmentId,
                filePath = filePath,
                fileName = localAttachment.displayName,
                fileSize = localAttachment.size,
                mimeType = localAttachment.mimeType,
                keyPackets = localAttachment.keyPackets,
                headers = localAttachment.headers
            )

        }

        /**
         * @throws MessagingException
         * @throws NoSuchAlgorithmException
         * @throws IOException
         */
        fun fromMimeAttachment(part: Part, message_id: String, count: Int): Attachment {
            val data = getBytesFromInputStream(part.inputStream)
            return fromMimeAttachment(part, data, message_id, count)
        }

        /**
         * @throws MessagingException
         * @throws NoSuchAlgorithmException
         * @throws IOException
         */
        fun fromMimeAttachment(part: Part, data: ByteArray, message_id: String, count: Int): Attachment {
            val attachment = Attachment()

            attachment.fileName = part.fileName ?: "default"
            val encoding = if (part is MimeBodyPart) part.encoding else "binary"
            val contentId = (part as? MimeBodyPart)?.contentID
            val contentLocations = part.getHeader("content-location")
            val contentLocation = contentLocations?.firstOrNull()

            attachment.headers = AttachmentHeaders(
                part.contentType,
                encoding,
                listOf(part.disposition),
                contentId,
                contentLocation,
                ""
            )

            Formatter().use { format ->
                val md5 = MessageDigest.getInstance("MD5")
                md5.update(data)
                md5.digest().forEach { b ->
                    format.format("%02x", b)
                }
                attachment.attachmentId = "PGPAttachment/$message_id/$format/$count"
            }
            attachment.fileSize = data.size.toLong()
            attachment.mimeType = part.contentType.replace("(\\s*)?[\\r\\n]+(\\s*)".toRegex(), "")
            attachment.mimeData = data
            attachment.messageId = message_id
            return attachment
        }

        /**
         * @throws MessagingException
         * @throws NoSuchAlgorithmException
         * @throws IOException
         */
        fun fromMimeAttachment(data: ByteArray, headers: InternetHeaders, message_id: String, count: Int): Attachment =
            fromMimeAttachment(MimeBodyPart(headers, ByteArray(0)), data, message_id, count)

        /**
         * @throws IOException
         */
        private fun getBytesFromInputStream(inputStream: InputStream): ByteArray {
            val outputStream = ByteArrayOutputStream()

            val buffer = ByteArray(0xFFFF)
            var len = inputStream.read(buffer)
            while (len != -1) {
                outputStream.write(buffer, 0, len)
                len = inputStream.read(buffer)
            }

            return outputStream.toByteArray()
        }

        @JvmOverloads
        @Synchronized
        fun createAttachmentList(
            messageDao: MessageDao,
            localAttachmentList: List<LocalAttachment>,
            useRandomIds: Boolean = true
        ): List<Attachment> = localAttachmentList.map { localAttachment ->
            fromLocalAttachment(
                messageDao,
                localAttachment,
                localAttachmentList.indexOf(localAttachment).toLong(),
                useRandomIds
            )
        }

        private fun getRandomAttachmentId(index: Long): String {
            val random = Random(System.nanoTime())
            val randomOneSec = random.nextInt()
            return "" + -(System.currentTimeMillis() + randomOneSec.toLong() + index)
        }
    }
}
