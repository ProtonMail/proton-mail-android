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

package ch.protonmail.android.api.models.messages.receive

import ch.protonmail.android.api.models.MessagePayload
import ch.protonmail.android.api.models.MessageRecipient
import junit.framework.Assert.assertEquals
import kotlin.test.Test

class ServerMessageTest {

    @Test
    fun convertServerMessageToMessagePayload() {
        val serverMessage = ServerMessage(
            ID = "ID",
            Subject = "Subject",
            Sender = ServerMessageSender("Name", "Address"),
            Body = "Body",
            ToList = listOf(MessageRecipient("User1", "user1@protonmail.com"), MessageRecipient("User2", "user2@pm.me")),
            CCList = listOf(MessageRecipient("User3", "user3@protonmail.com")),
            BCCList = listOf()
        )

        val actual = serverMessage.toMessagePayload()

        val expected = MessagePayload(
            "ID",
            "Subject",
            ServerMessageSender("Name", "Address"),
            "Body",
            listOf(MessageRecipient("User1", "user1@protonmail.com"), MessageRecipient("User2", "user2@pm.me")),
            listOf(MessageRecipient("User3", "user3@protonmail.com")),
            listOf()
        )

        assertEquals(expected, actual)
    }
}
