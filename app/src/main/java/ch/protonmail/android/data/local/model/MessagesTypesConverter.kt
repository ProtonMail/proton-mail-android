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

import android.util.Base64
import androidx.room.TypeConverter
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.enumerations.MessageEncryption
import ch.protonmail.android.api.models.messages.ParsedHeaders
import ch.protonmail.android.utils.FileUtils
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.serialize
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

class MessagesTypesConverter {

    @TypeConverter
    fun messageEncryptionToInt(messageEncryption: MessageEncryption): Int =
        messageEncryption.ordinal

    @TypeConverter
    fun intToMessageEncryption(messageEncryptionOrdinal: Int): MessageEncryption =
        MessageEncryption.values()[messageEncryptionOrdinal]

    @TypeConverter
    fun parsedHeadersToString(parsedHeaders: ParsedHeaders?) = parsedHeaders?.serialize()

    @TypeConverter
    fun stringToParsedHeaders(parsedHeadersString: String?): ParsedHeaders? =
        parsedHeadersString?.deserialize(ParsedHeaders.serializer())

    @TypeConverter
    fun messageRecipientsListToString(messageRecipient: List<MessageRecipient>?): String? =
        messageRecipient?.let(FileUtils::toString)

    @TypeConverter
    fun stringToMessageRecipientsList(messageRecipientString: String?): List<MessageRecipient>? {
        messageRecipientString ?: return null

        val decoded = Base64.decode(messageRecipientString.toByteArray(), Base64.DEFAULT)
        val inputStream = ByteArrayInputStream(decoded)

        return try {
            @Suppress("UNCHECKED_CAST")
            ObjectInputStream(inputStream).readObject() as List<MessageRecipient>

        } catch (e: Exception) {
            Timber.e(e, "Deserialization of recipients failed")
            emptyList()
        }
    }

    @TypeConverter
    fun messageTypeToInt(messageType: Message.MessageType): Int =
        messageType.ordinal

    @TypeConverter
    fun intToMessageType(messageTypeOrdinal: Int): Message.MessageType =
        Message.MessageType.values()[messageTypeOrdinal]
}
