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

import ch.protonmail.android.api.models.enumerations.MessageEncryption
import ch.protonmail.android.api.models.messages.ParsedHeaders
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Created by Kamil Rajtar on 18.07.18.  */
internal class MessagesTypesConverterTest {
	private val messagesTypesConverter = MessagesTypesConverter()

	@ParameterizedTest
	@EnumSource(MessageEncryption::class)
	fun messageEncryptionAll(messageEncryption:MessageEncryption) {
		val messageEncryptionInt = messagesTypesConverter.messageEncryptionToInt(messageEncryption)
		val actual = messagesTypesConverter.intToMessageEncryption(messageEncryptionInt)
		Assertions.assertEquals(messageEncryption, actual)
	}

	@Test
	fun parsedHeadersNullIn() {
		val expected = null
		val actual = messagesTypesConverter.parsedHeadersToString(null)
		Assertions.assertEquals(expected, actual)
	}

	@Test
	fun parsedHeadersNullOut() {
		val expected = null
		val actual = messagesTypesConverter.stringToParsedHeaders(null)
		Assertions.assertEquals(expected, actual)
	}

	@Disabled(value="Serialisation uses Base64 - not mocked")
	@Test
	fun parsedHeadersSimple() {
		val encryptionString = "Encryption"
		val authenticationString = "Authentication"
		val parsedHeaders = ParsedHeaders(encryptionString, authenticationString)
		val parsedHeadersString = messagesTypesConverter.parsedHeadersToString(parsedHeaders)
		val actual = messagesTypesConverter.stringToParsedHeaders(parsedHeadersString)
				?: throw  RuntimeException("Actual should not be null")
		Assertions.assertEquals(encryptionString, actual.recipientEncryption)
		Assertions.assertEquals(authenticationString, actual.recipientAuthentication)
	}

	@Test
	fun messagesRecipientsArrayNullIn() {
		val expected = null
		val actual = messagesTypesConverter.messageRecipientsListToString(null)
		Assertions.assertEquals(expected, actual)
	}

	@Test
	fun messagesRecipientsArrayNullOut() {
		val expected = null
		val actual = messagesTypesConverter.stringToMessageRecipientsList(null)
		Assertions.assertEquals(expected, actual)
	}

	@ParameterizedTest
	@EnumSource(Message.MessageType::class)
	fun messageType(messageType:Message.MessageType) {
		val messageTypeInt = messagesTypesConverter.messageTypeToInt(messageType)
		val actual = messagesTypesConverter.intToMessageType(messageTypeInt)
		Assertions.assertEquals(messageType, actual)
	}

	@Test
	fun labelIds() {
		val expected = listOf("Label 1", "Super label", "AA", "Label HMM")
		val serialised = messagesTypesConverter.labelIdsToString(expected)
		val actual = messagesTypesConverter.stringToLabelIds(serialised)
		Assertions.assertEquals(expected, actual)
	}
}
