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

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import ch.protonmail.android.api.models.AttachmentHeaders
import ch.protonmail.android.core.ProtonMailApplication

class LocalAttachment @JvmOverloads constructor(var uri: Uri,
												val displayName: String,
												val size: Long = 0,
												val mimeType: String,
												var attachmentId: String = "",
												var messageId: String = "",
												var isEmbeddedImage: Boolean = false,
												var isUploading: Boolean = false,
												val keyPackets: String? = null,
												val headers: AttachmentHeaders? = null,
                                                val isUploaded: Boolean = false) : Parcelable {

	override fun describeContents(): Int {
		return 0
	}

	override fun writeToParcel(dest: Parcel, flags: Int) {
		dest.writeString(attachmentId)
		dest.writeString(messageId)
		dest.writeParcelable(uri, 0)
		dest.writeString(displayName)
		dest.writeLong(size)
		dest.writeString(mimeType)
		dest.writeInt(if (isEmbeddedImage) 1 else 0)
		dest.writeInt(if (isUploading) 1 else 0)
		dest.writeString(keyPackets)
		dest.writeString(headers?.toString() ?: "")
        dest.writeInt(if (isUploaded) 1 else 0)
	}

	companion object {

		fun fromAttachment(attachment: Attachment): LocalAttachment {
			val uriToParse = if (attachment.isPGPAttachment) {
				val encodedMimeData = Base64.encodeToString(
						attachment.mimeData,
						Base64.NO_WRAP)
				"data:application/octet-stream;base64,$encodedMimeData"
			} else {
				ProtonMailApplication.getApplication().api.getAttachmentUrl(attachment.attachmentId!!)
			}

			return LocalAttachment(
					uri = Uri.parse(uriToParse),
					displayName = attachment.fileName!!,
					size = attachment.fileSize,
					mimeType = attachment.mimeType!!,
					attachmentId = attachment.attachmentId!!,
					messageId = attachment.messageId,
					isEmbeddedImage = attachment.inline,
					headers = attachment.headers,
					keyPackets = attachment.keyPackets,
					isUploading =  attachment.isUploading,
                    isUploaded = attachment.isUploaded
			)
		}

		fun createLocalAttachmentList(attachmentList: List<Attachment>): List<LocalAttachment> {
			return attachmentList.map(Companion::fromAttachment)
		}

		@JvmField
		val CREATOR = object : Parcelable.Creator<LocalAttachment> {
			override fun createFromParcel(parcel: Parcel): LocalAttachment {
				val attachmentId = parcel.readString()
				val messageId = parcel.readString()
				val uri = parcel.readParcelable<Uri>(Uri::class.java.classLoader)
				val name = parcel.readString()
				val size = parcel.readLong()
				val mimeType = parcel.readString()
				val isEmbeddedImage = parcel.readInt() == 1
				val isUploading = parcel.readInt() == 1
				val keyPackets = parcel.readString()
				val serializedHeaders = parcel.readString()
				val headers = if (serializedHeaders.isEmpty()) null else AttachmentHeaders.fromString(
						serializedHeaders)
                val isUploaded = parcel.readInt() == 1

				return LocalAttachment(uri,
						name,
						size,
						mimeType,
						attachmentId,
						messageId,
						isEmbeddedImage,
						isUploading,
						keyPackets,
						headers,
                        isUploaded)
			}

			override fun newArray(size: Int): Array<LocalAttachment?> {
				return arrayOfNulls(size)
			}
		}
	}
}
