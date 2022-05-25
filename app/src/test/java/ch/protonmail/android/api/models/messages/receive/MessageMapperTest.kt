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

package ch.protonmail.android.api.models.messages.receive

import ch.protonmail.android.api.models.MessagePayload
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import junit.framework.Assert.assertEquals
import kotlin.test.Test

class MessageMapperTest {

    @Test
    fun convertMessageToAPIMessagePayload() {
        val message = Message(
            messageId = "ID",
            subject = "Subject",
            sender = MessageSender("Name", "Address"),
            messageBody = "Body",
            toList = listOf(
                MessageRecipient("User1", "user1@protonmail.com"),
                MessageRecipient("User2", "user2@pm.me")
            ),
            ccList = listOf(
                MessageRecipient("User3", "user3@protonmail.com")
            ),
            bccList = listOf(),
            Unread = true
        )

        val actual = message.toApiPayload()

        val expected = MessagePayload(
            sender = ServerMessageSender("Name", "Address"),
            body = "Body",
            id = "ID",
            subject = "Subject",
            toList = listOf(
                MessageRecipient("User1", "user1@protonmail.com"),
                MessageRecipient("User2", "user2@pm.me")
            ),
            ccList = listOf(
                MessageRecipient("User3", "user3@protonmail.com")
            ),
            bccList = listOf(),
            unread = 1
        )

        assertEquals(expected, actual)
    }

}
