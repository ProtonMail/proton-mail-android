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
package ch.protonmail.android.activities.messageDetails

import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.core.BigContentHolder
import ch.protonmail.android.data.local.model.Message
import org.junit.Ignore
import kotlin.test.Test

class IntentExtrasDataTest {

    @Test
    @Ignore("Ignoring as proper mocking needs to be put in place")
    fun `detect user's email alias when sent to alias`() {

        val user = User()

        val message = Message().apply {
            toList = listOf(MessageRecipient("User1", "user1+alias@protonmail.com"), MessageRecipient("User2", "user2@pm.me"))
        }

        val extras = IntentExtrasData.Builder()
            .user(user)
            .message(message)
            .userAddresses()
            .toRecipientListString(message.toListString)
            .messageCcList()
            .senderEmailAddress()
            .addressEmailAlias()
            .messageSenderName()
            .content("")
            .mBigContentHolder(BigContentHolder())
            .attachments(arrayListOf(), mutableListOf())
            .build()

        assert(extras.addressEmailAlias == "user1+alias@protonmail.com")
    }

    @Test
    @Ignore("Ignoring as proper mocking needs to be put in place")
    fun `detect user's email alias when sent to alias, preserve case`() {

        val user = User()

        val message = Message().apply {
            toList = listOf(MessageRecipient("User1", "uSeR1+alias@protonmail.com"), MessageRecipient("User2", "user2@pm.me"))
        }

        val extras = IntentExtrasData.Builder()
            .user(user)
            .message(message)
            .userAddresses()
            .toRecipientListString(message.toListString)
            .messageCcList()
            .senderEmailAddress()
            .addressEmailAlias()
            .messageSenderName()
            .content("")
            .mBigContentHolder(BigContentHolder())
            .attachments(arrayListOf(), mutableListOf())
            .build()

        assert(extras.addressEmailAlias == "USER1+alias@protonmail.com")
    }

    @Test
    @Ignore("Ignoring as proper mocking needs to be put in place")
    fun `don't detect user's email alias when sent to alias of other user`() {

        val user = User()

        val message = Message().apply {
            toList = listOf(MessageRecipient("User1", "user1@protonmail.com"), MessageRecipient("User2", "user2+alias@pm.me"))
        }

        val extras = IntentExtrasData.Builder()
            .user(user)
            .message(message)
            .userAddresses()
            .toRecipientListString(message.toListString)
            .messageCcList()
            .senderEmailAddress()
            .addressEmailAlias()
            .messageSenderName()
            .content("")
            .mBigContentHolder(BigContentHolder())
            .attachments(arrayListOf(), mutableListOf())
            .build()

        assert(extras.addressEmailAlias == null)
    }
}
