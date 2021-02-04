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

import androidx.room.Room
import androidx.test.InstrumentationRegistry
import ch.protonmail.android.api.models.enumerations.MessageEncryption
import org.junit.Assert
import kotlin.test.Test

class MessagesDatabaseTest {
    private val context = InstrumentationRegistry.getTargetContext()
    private var databaseFactory = Room.inMemoryDatabaseBuilder(context, MessagesDatabaseFactory::class.java).build()
    private var initiallyEmptyDatabase = databaseFactory.getDatabase()

    private fun createBaseMessage(): Message {
        return Message().apply {
            setIsEncrypted(MessageEncryption.INTERNAL)
            sender = MessageSender("Test", "Test")
        }
    }

    @Test
    fun insertFindByIdShouldReturnTheSame() {
        val expected = createBaseMessage()
        val id = "testId"
        expected.messageId = id
        initiallyEmptyDatabase.saveMessage(expected)
        val actual = initiallyEmptyDatabase.findMessageByIdBlocking(id)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun insertFindByMessageDbIdShouldReturnTheSame() {
        val expected = createBaseMessage()
        val id = "testId"
        expected.messageId = id
        val dbId = initiallyEmptyDatabase.saveMessage(expected)
        val actual = initiallyEmptyDatabase.findMessageByMessageDbId(dbId)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun insertFindMessageLabelIdShouldReturnTheAppropriate() {
        val message1 = createBaseMessage()
        message1.messageId = "1"
        message1.allLabelIDs = listOf("1", "5", "10")
        val message2 = createBaseMessage()
        message2.messageId = "2"
        message2.allLabelIDs = listOf("1", "10")
        val message3 = createBaseMessage()
        message3.messageId = "3"
        message3.allLabelIDs = listOf("1", "5")

        initiallyEmptyDatabase.saveAllMessages(listOf(message1, message2, message3))

        val expected = listOf(message1, message3)
        val actual = initiallyEmptyDatabase.getMessagesByLabelId("5").sortedBy(Message::messageId)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun insertFindMessageLabelIdShouldReturnTheAppropriateSecond() {
        val message1 = createBaseMessage()
        message1.messageId = "1"
        message1.allLabelIDs = listOf("1", "abcdef50abcdef", "10")
        val message2 = createBaseMessage()
        message2.messageId = "2"
        message2.allLabelIDs = listOf("1", "10")
        val message3 = createBaseMessage()
        message3.messageId = "3"
        message3.allLabelIDs = listOf("1", "ttttt50tttt")
        val message4 = createBaseMessage()
        message4.messageId = "4"
        message4.allLabelIDs = listOf("1", "ttttt50")
        val message5 = createBaseMessage()
        message5.messageId = "5"
        message5.allLabelIDs = listOf("1", "50aaaaaaa")
        val message6 = createBaseMessage()
        message6.messageId = "6"
        message6.allLabelIDs = listOf("1", "aaaa5oaaaaaaa")

        initiallyEmptyDatabase.saveAllMessages(listOf(message1, message2, message3, message4, message5))

        val expected = listOf(message1, message3, message4, message5)
        val actual = initiallyEmptyDatabase.getMessagesByLabelId("50").sortedBy(Message::messageId)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testUpdateGoesFine() {
        val message1 = createBaseMessage()
        message1.messageId = "1"
        message1.allLabelIDs = listOf("1", "5", "10")
        val message2 = createBaseMessage()
        message2.messageId = "2"
        message2.allLabelIDs = listOf("1", "10")
        val message3 = createBaseMessage()
        message3.messageId = "3"
        message3.allLabelIDs = listOf("1", "5")

        initiallyEmptyDatabase.saveAllMessages(listOf(message1, message2, message3))
        val savedMessage = initiallyEmptyDatabase.findMessageByIdBlocking("2")
        savedMessage?.Unread = true
        initiallyEmptyDatabase.saveMessage(savedMessage!!)
        val savedMEssage2 = initiallyEmptyDatabase.findMessageByIdBlocking("2")
        Assert.assertNotNull(savedMEssage2)
    }
}
