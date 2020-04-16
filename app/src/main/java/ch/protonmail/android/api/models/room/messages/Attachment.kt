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
package ch.protonmail.android.api.models.room.messages

import android.provider.BaseColumns
import android.text.TextUtils
import android.webkit.URLUtil
import androidx.room.*
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.api.models.AttachmentHeaders
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.crypto.Crypto
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.proton.gopenpgp.armor.Armor
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.mail.MessagingException
import javax.mail.Part
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimeBodyPart

// region constants
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
// endregion

@Entity(tableName = TABLE_ATTACHMENTS,
		indices = [Index(value = [COLUMN_ATTACHMENT_ID], unique = true)])
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

	/**
	 * In case there are more values delimited with semicolon, returns only the first one.
	 */
	val mimeTypeFirstValue: String?
		get() = mimeType?.substringBefore(";")

	fun setMessage(message: Message?) {
		if (message != null) {
			this.messageId = message.messageId ?: ""
			this.inline = isInline(message.embeddedImagesArray)
		}
	}

	private fun isInline(embeddedImagesArray: List<String>): Boolean {
        val headers = headers ?: return false
		val contentDisposition = headers.contentDisposition
        var contentId = headers.contentId
        if (TextUtils.isEmpty(contentId)) {
            contentId = headers.contentLocation
        }
		if (!TextUtils.isEmpty(contentId)) {
			contentId = contentId.removeSurrounding("<", ">")
		}
		val embeddedMimeTypes = Arrays.asList("image/gif", "image/jpeg", "image/png", "image/bmp")
		var containsInline = false
		for (element in contentDisposition) {
			if (element.contains("inline")) {
				containsInline = true
				break
			}
		}

		return contentDisposition != null && (containsInline || embeddedImagesArray.contains(contentId.removeSurrounding("<",">"))) && embeddedMimeTypes.contains(mimeType)
    }

	@Throws(Exception::class)
	fun uploadAndSave(messageDetailsRepository: MessageDetailsRepository, api:ProtonMailApi, crypto:Crypto) : String? {
		val filePath = filePath
		val fileContent = if (URLUtil.isDataUrl(filePath)) {
			android.util.Base64.decode(filePath!!.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()[1],
					android.util.Base64.DEFAULT)
		} else {
			val file = File(filePath!!)
			AppUtil.getByteArray(file)
		}
		return uploadAndSave(messageDetailsRepository, fileContent, api, crypto)
	}

	@Throws(Exception::class)
	fun uploadAndSave(messageDetailsRepository: MessageDetailsRepository, fileContent:ByteArray, api: ProtonMailApi, crypto:Crypto) : String? {
		val headers = headers
		val bct = crypto.encrypt(fileContent, fileName)
		val keyPackage = RequestBody.create(MediaType.parse(mimeType!!), bct.keyPacket)
		val dataPackage = RequestBody.create(MediaType.parse(mimeType!!), bct.dataPacket)
		val signature = RequestBody.create(MediaType.parse("application/octet-stream"), Armor.unarmor(crypto.sign(fileContent)))
		val response =
				if (headers != null && headers.contentDisposition.contains("inline") && headers.contentId != null) {
					var contentID = headers.contentId
					val parts = contentID.split("<").dropLastWhile { it.isEmpty() }.toTypedArray()
					if (parts.size > 1) {
						contentID = parts[1].replace(">", "")
					}
					api.uploadAttachmentInline(this, messageId, contentID, keyPackage,
							dataPackage, signature)
				} else {
					api.uploadAttachment(this, messageId, keyPackage, dataPackage, signature)
				}

		if (response.code == Constants.RESPONSE_CODE_OK) {
			attachmentId = response.attachmentID
			keyPackets = response.attachment.keyPackets
			this.signature = response.attachment.signature
			isUploaded = true
			messageDetailsRepository.saveAttachment(this)
		}
		return attachmentId
	}

	companion object {
		private fun fromLocalAttachment(messagesDatabase: MessagesDatabase,
										localAttachment: LocalAttachment,
										index: Long,
										useRandomIds: Boolean): Attachment {
			if (!TextUtils.isEmpty(localAttachment.attachmentId)) {
				val savedAttachment = messagesDatabase.findAttachmentById(localAttachment.attachmentId)
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

			return Attachment(attachmentId = attachmentId,
					filePath = filePath,
					fileName = localAttachment.displayName,
					fileSize = localAttachment.size,
					mimeType = localAttachment.mimeType,
					keyPackets = localAttachment.keyPackets,
					headers = localAttachment.headers)

		}

		@Throws(MessagingException::class, NoSuchAlgorithmException::class, IOException::class)
		fun fromMimeAttachment(part: Part, message_id: String, count: Int): Attachment {
			val data = getBytesFromInputStream(part.inputStream)
			return fromMimeAttachment(part, data, message_id, count);
		}

		@Throws(MessagingException::class, NoSuchAlgorithmException::class, IOException::class)
		fun fromMimeAttachment(part: Part, data: ByteArray, message_id: String, count: Int): Attachment {
			val attachment = Attachment()

			attachment.fileName = part.fileName ?: "default"
			val encoding = if (part is MimeBodyPart) part.encoding else "binary"
			val contentId = (part as? MimeBodyPart)?.contentID
			val contentLocations = part.getHeader("content-location")
			val contentLocation = contentLocations?.firstOrNull()

			attachment.headers = (AttachmentHeaders(part.contentType,
					encoding,
					listOf(part.disposition),
					listOf(contentId),
					contentLocation, ""))

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

		@Throws(MessagingException::class, NoSuchAlgorithmException::class, IOException::class)
		fun fromMimeAttachment(data: ByteArray, headers: InternetHeaders, message_id: String, count: Int): Attachment {
			return fromMimeAttachment(MimeBodyPart(headers, ByteArray(0)), data, message_id, count)
		}

		@Throws(IOException::class)
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
		fun createAttachmentList(messagesDatabase: MessagesDatabase,
								 localAttachmentList: List<LocalAttachment>,
								 useRandomIds: Boolean = true): List<Attachment> {
			return localAttachmentList.map { localAttachment ->
				Attachment.fromLocalAttachment(messagesDatabase, localAttachment, localAttachmentList.indexOf(localAttachment).toLong(), useRandomIds)
			}
		}

		private fun getRandomAttachmentId(index: Long): String {
			val random = Random(System.nanoTime())
			val randomOneSec = random.nextInt()
			return "" + -(System.currentTimeMillis() + randomOneSec.toLong() + index)
		}
	}
}
