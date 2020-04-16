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

import androidx.room.TypeConverter
import android.util.Base64

import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.enumerations.MessageEncryption
import ch.protonmail.android.api.models.messages.ParsedHeaders
import ch.protonmail.android.utils.FileUtils
import ch.protonmail.android.utils.Logger

import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

/**
 * Created by Kamil Rajtar on 15.07.18.  */
class MessagesTypesConverter {
	@TypeConverter
	fun messageEncryptionToInt(messageEncryption:MessageEncryption)=messageEncryption.ordinal

	@TypeConverter
	fun intToMessageEncryption(messageEncryptionOrdinal:Int)=MessageEncryption.values()[messageEncryptionOrdinal]

	@TypeConverter
	fun parsedHeadersToString(parsedHeaders:ParsedHeaders?)=parsedHeaders?.let {
		FileUtils.toString(it)
	}

	@TypeConverter
	fun stringToParsedHeaders(parsedHeadersString:String?)=FileUtils.deserializeStringToObject(
			parsedHeadersString) as ParsedHeaders?

	@TypeConverter
	fun messageRecipientsListToString(messageRecipient: List<MessageRecipient>?): String? = messageRecipient?.let {
		FileUtils.toString(
				messageRecipient)
	}

	@TypeConverter
	fun stringToMessageRecipientsList(messageRecipientString: String?): List<MessageRecipient>? {
		if (messageRecipientString == null)
			return null
		val inputStream = ByteArrayInputStream(Base64.decode(messageRecipientString.toByteArray(), Base64.DEFAULT))

		return try {
			ObjectInputStream(inputStream).readObject() as List<MessageRecipient>
		} catch (e: Exception) {
			Logger.doLogException("MessagesTypesConverter", "DeSerialization of recipients failed", e)
			listOf()
		}
	}

	//TODO unsafe conversion create for label ids separate type
	@TypeConverter
	fun labelIdsToString(labelIds:List<String>)=labelIds.joinToString(";")

	//TODO unsafe conversion create for label ids separate type
	@TypeConverter
	fun stringToLabelIds(labelIdsString:String)=labelIdsString.split(";").dropLastWhile {it.isEmpty()}

	@TypeConverter
	fun messageTypeToInt(messageType:Message.MessageType)=messageType.ordinal

	@TypeConverter
	fun intToMessageType(messageTypeOrdinal:Int)=Message.MessageType.values()[messageTypeOrdinal]
}
