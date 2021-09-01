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
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessagesTypesConverter
import junit.framework.Assert.assertEquals
import java.net.URLEncoder
import kotlin.test.Test

class MessagesTypesConverterTest {

    private val messagesTypesConverter = MessagesTypesConverter()

    @Test
    fun messageEncryptionMapsBetweenEnumValueAndOrdinal() {
        val messageEncryption = MessageEncryption.EXTERNAL_PGP
        val messageEncryptionInt = messagesTypesConverter.messageEncryptionToInt(messageEncryption)

        val actual = messagesTypesConverter.intToMessageEncryption(messageEncryptionInt)

        assertEquals(messageEncryption, actual)
    }

    @Test
    fun verifySerializationOfNUllParsedHeadersIsHandled() {
        val expected = null

        val actual = messagesTypesConverter.parsedHeadersToString(null)

        assertEquals(expected, actual)
    }

    @Test
    fun verifyDeserializationOfNullParsedHeadersIsHandled() {
        val expected = null

        val actual = messagesTypesConverter.stringToParsedHeaders(null)

        assertEquals(expected, actual)
    }

    @Test
    fun verifyParsedHeadersAreMappedCorrectly() {
        val expected = ParsedHeaders(
            "${URLEncoder.encode("a@a.com", "UTF-8")}=a",
            "${URLEncoder.encode("a@a.com", "UTF-8")}=b"
        )
        val parsedHeadersString = messagesTypesConverter.parsedHeadersToString(expected)

        val actual = messagesTypesConverter.stringToParsedHeaders(parsedHeadersString)

        assertEquals(actual, expected)
    }

    @Test
    fun messagesRecipientsArrayNullIn() {
        val expected = null

        val actual = messagesTypesConverter.messageRecipientsListToString(null)

        assertEquals(expected, actual)
    }

    @Test
    fun messagesRecipientsArrayNullOut() {
        val expected = null

        val actual = messagesTypesConverter.stringToMessageRecipientsList(null)

        assertEquals(expected, actual)
    }

    @Test
    fun messageTypeMapsCorrectlyBetweenEnumAndOrdinal() {
        val messageType = Message.MessageType.INBOX_AND_SENT
        val messageTypeInt = messagesTypesConverter.messageTypeToInt(messageType)

        val actual = messagesTypesConverter.intToMessageType(messageTypeInt)

        assertEquals(messageType, actual)
    }

    @Test
    fun labelIds() {
        val expected = listOf("Label 1", "Super label", "AA", "Label HMM")
        val serialised = messagesTypesConverter.labelIdsToString(expected)

        val actual = messagesTypesConverter.stringToLabelIds(serialised)

        assertEquals(expected, actual)
    }
}
