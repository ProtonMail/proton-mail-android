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

package ch.protonmail.android.mailbox.data

import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.data.local.model.ConversationEntity
import ch.protonmail.android.mailbox.data.remote.model.ConversationRemote
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import kotlin.test.assertEquals
import org.junit.Test

class ConversationMapperTest {


    private val testUserId = Id("id")
    private val conversationsRemote = listOf(
        ConversationRemote(
            id = "conversation1",
            order = 1,
            subject = "subject1",
//            listOf(MessageSender("sender1", "sender1@pm.com")),
//            listOf(MessageRecipient("recipient1", "recipient1@pm.com")),
            numMessages = 3,
            numUnread = 1,
            numAttachments = 0,
            expirationTime = 0L,
            size = 0L
        ),
        ConversationRemote(
            id = "conversation2",
            order = 0,
            subject = "subject2",
//            listOf(MessageSender("sender1", "sender1@pm.com")),
//            listOf(
//                MessageRecipient("recipient1", "recipient1@pm.com"),
//                MessageRecipient("recipient2", "recipient2@pm.com")
//            ),
            numMessages = 1,
            numUnread = 1,
            numAttachments = 0,
            expirationTime = 0L,
            size = 0L
        )
    )

    private val conversationsEntity = listOf(
        ConversationEntity(
            id = "conversation1",
            order = 1,
            userId = "id",
            subject = "subject1",
//            listOf(MessageSender("sender1", "sender1@pm.com")),
//            listOf(MessageRecipient("recipient1", "recipient1@pm.com")),
            numMessages = 3,
            numUnread = 1,
            numAttachments = 0,
            expirationTime = 0L,
            size = 0L
        ),
        ConversationEntity(
            id = "conversation2",
            order = 0,
            userId = "id",
            subject = "subject2",
//            listOf(MessageSender("sender1", "sender1@pm.com")),
//            listOf(
//                MessageRecipient("recipient1", "recipient1@pm.com"),
//                MessageRecipient("recipient2", "recipient2@pm.com")
//            ),
            numMessages = 1,
            numUnread = 1,
            numAttachments = 0,
            expirationTime = 0L,
            size = 0L
        )
    )


    private val conversations = listOf(
        Conversation(
            id = "conversation1",
            subject = "subject1",
            listOf(Correspondent("sender1", "sender1@pm.com")),
            listOf(Correspondent("recipient1", "recipient1@pm.com")),
            messagesCount = 3,
            unreadCount = 1,
            attachmentsCount = 0,
            expirationTime = 0L
        ),
        Conversation(
            id = "conversation2",
            subject = "subject2",
            listOf(Correspondent("sender1", "sender1@pm.com")),
            listOf(
                Correspondent("recipient1", "recipient1@pm.com"),
                Correspondent("recipient2", "recipient2@pm.com")
            ),
            messagesCount = 1,
            unreadCount = 1,
            attachmentsCount = 0,
            expirationTime = 0L
        )
    )

    @Test
    fun verifyThatEmptyConversationRemoteAreMappedProperly() {
        // given
        val conversation = listOf<ConversationRemote>()
        val expected = emptyList<ConversationEntity>()

        // when
        val result = conversation.toListLocal(testUserId.s)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatEmptyConversationEntityAreMappedProperly() {
        // given
        val conversation = listOf<ConversationEntity>()
        val expected = emptyList<Conversation>()

        // when
        val result = conversation.toDomainModelList()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatConversationRemoteIsMappedProperly() {

        // when
        val result = conversationsRemote[0].toLocal(testUserId.s)

        val expected = conversationsEntity[0]

        // then
        assertEquals(expected, result)
    }


    @Test
    fun verifyThatConversationEntityIsMappedProperly() {

        // when
        val result = conversationsEntity[0].toDomainModel()

        val expected = conversations[0]

        // then
        assertEquals(expected, result)
    }


    @Test
    fun verifyThatConversationRemoteListIsMappedProperly() {

        // when
        val result = conversationsRemote.toListLocal(testUserId.s)

        val expected = conversationsEntity

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatConversationEntityListIsMappedProperly() {

        // when
        val result = conversationsEntity.toDomainModelList()

        val expected = conversations

        // then
        assertEquals(expected, result)
    }
}
