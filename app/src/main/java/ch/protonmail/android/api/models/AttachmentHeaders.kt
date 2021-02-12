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
package ch.protonmail.android.api.models

import android.util.Base64
import ch.protonmail.android.api.utils.Fields
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.lang.reflect.Type

/**
 * [AttachmentHeaders] model to hold the headers for attachment.
 * All fields in this class can be sent from server either as String or as Array, it was confirmed that
 * it's ok to always parse them as String and if they are sent as an Array in that case get the first element.
 */
class AttachmentHeaders(
    @SerializedName(Fields.Attachment.CONTENT_TYPE)
    var contentType: String = EMPTY_STRING,
    @SerializedName(Fields.Attachment.CONTENT_TRANSFER_ENCODING)
    var contentTransferEncoding: String = EMPTY_STRING,
    @SerializedName(Fields.Attachment.CONTENT_DISPOSITION)
    var contentDisposition: List<String> = listOf(),
    @SerializedName(Fields.Attachment.CONTENT_ID)
    var contentId: String = EMPTY_STRING,
    @SerializedName(Fields.Attachment.CONTENT_LOCATION)
    var contentLocation: String = EMPTY_STRING,
    @SerializedName(Fields.Attachment.CONTENT_ENCRYPTION)
    var contentEncryption: String = EMPTY_STRING
) : Serializable {

    override fun toString(): String {
        val out = ByteArrayOutputStream()
        try {
            ObjectOutputStream(out).writeObject(this)
        } catch (e: IOException) {
            Timber.d(e, "Serialization of att headers failed")
        }
        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
    }

    class AttachmentHeadersDeserializer : JsonDeserializer<AttachmentHeaders> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): AttachmentHeaders {
            val jsonObject = json.asJsonObject
            val contentType = getAsString(jsonObject[Fields.Attachment.CONTENT_TYPE])
            val contentTransferEncoding = getAsString(jsonObject[Fields.Attachment.CONTENT_TRANSFER_ENCODING])

            val contentId = getAsString(jsonObject[Fields.Attachment.CONTENT_ID])
            val contentLocation = getAsString(jsonObject[Fields.Attachment.CONTENT_LOCATION])

            val contentDisposition = getAsArray(jsonObject[Fields.Attachment.CONTENT_DISPOSITION]) as List<String>
            val contentEncryption = getAsString(jsonObject[Fields.Attachment.CONTENT_ENCRYPTION])

            return AttachmentHeaders(
                contentType,
                contentTransferEncoding,
                contentDisposition,
                contentId,
                contentLocation,
                contentEncryption
            )
        }
    }

    companion object {
        private const val serialVersionUID = -8741548902749037534L

        fun getAsString(jsonElement: JsonElement?): String {
            return if (jsonElement != null) {
                if (jsonElement.isJsonArray) {
                    jsonElement.asJsonArray[0].asString
                } else {
                    jsonElement.asString
                }
            } else ""
        }

        fun getAsArray(jsonElement: JsonElement?): Any {
            val listOfHeaders = arrayListOf<String>()
            if (jsonElement == null) return listOfHeaders
            if (jsonElement.isJsonArray) {
                val iterator: Iterator<JsonElement> = jsonElement.asJsonArray.iterator()
                while (iterator.hasNext()) {
                    val header = iterator.next().asString
                    // apparently sometimes null objects are received from the api
                    if (header != null) {
                        listOfHeaders.add(header)
                    }
                }
            } else {
                listOfHeaders.add(jsonElement.asString)
            }
            return listOfHeaders
        }

        fun fromString(value: String?): AttachmentHeaders? {
            val input = ByteArrayInputStream(Base64.decode(value, Base64.DEFAULT))
            var result: AttachmentHeaders? = null
            try {
                result = ObjectInputStream(input).readObject() as AttachmentHeaders
            } catch (e: IOException) {
                Timber.d(e, "DeSerialization of recipients failed")
            }
            return result
        }
    }
}
