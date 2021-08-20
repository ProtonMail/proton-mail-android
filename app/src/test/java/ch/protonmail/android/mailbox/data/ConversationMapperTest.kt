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
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.LabelContextDatabaseModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import ch.protonmail.android.mailbox.data.remote.model.CorrespondentApiModel
import ch.protonmail.android.mailbox.data.remote.model.LabelContextApiModel
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.LabelContext
import org.junit.Test
import kotlin.test.assertEquals

class ConversationMapperTest {


    private val testUserId = UserId("id")
    private val conversationsRemote = listOf(
        ConversationApiModel(
            id = "conversation1",
            order = 1,
            subject = "subject1",
            listOf(CorrespondentApiModel("sender1", "sender1@pm.com")),
            listOf(CorrespondentApiModel("recipient1", "recipient1@pm.com")),
            numMessages = 3,
            numUnread = 1,
            numAttachments = 0,
            expirationTime = 0L,
            size = 30L,
            listOf(
                LabelContextApiModel("0", 1, 2, 2, 30, 0),
                LabelContextApiModel("7", 0, 1, 0, 0, 0)
            )
        ),
        ConversationApiModel(
            id = "conversation2",
            order = 0,
            subject = "subject2",
            listOf(CorrespondentApiModel("sender1", "sender1@pm.com")),
            listOf(
                CorrespondentApiModel("recipient1", "recipient1@pm.com"),
                CorrespondentApiModel("recipient2", "recipient2@pm.com")
            ),
            numMessages = 1,
            numUnread = 0,
            numAttachments = 4,
            expirationTime = 12_345L,
            size = 0L,
            listOf(LabelContextApiModel("0", 0, 1, 3, 0, 4))
        )
    )

    private val conversationsEntity = listOf(
        ConversationDatabaseModel(
            id = "conversation1",
            order = 1,
            userId = "id",
            subject = "subject1",
            listOf(MessageSender("sender1", "sender1@pm.com")),
            listOf(MessageRecipient("recipient1", "recipient1@pm.com")),
            numMessages = 3,
            numUnread = 1,
            numAttachments = 0,
            expirationTime = 0L,
            size = 30L,
            listOf(
                LabelContextDatabaseModel("0", 1, 2, 2, 30, 0),
                LabelContextDatabaseModel("7", 0, 1, 0, 0, 0)
            )
        ),
        ConversationDatabaseModel(
            id = "conversation2",
            order = 0,
            userId = "id",
            subject = "subject2",
            listOf(MessageSender("sender1", "sender1@pm.com")),
            listOf(
                MessageRecipient("recipient1", "recipient1@pm.com"),
                MessageRecipient("recipient2", "recipient2@pm.com")
            ),
            numMessages = 1,
            numUnread = 0,
            numAttachments = 4,
            expirationTime = 12_345L,
            size = 0L,
            listOf(LabelContextDatabaseModel("0", 0, 1, 3, 0, 4))
        )
    )


    private val conversations = listOf(
        Conversation(
            id = "conversation1",
            subject = "subject1",
            senders = listOf(Correspondent("sender1", "sender1@pm.com")),
            receivers = listOf(Correspondent("recipient1", "recipient1@pm.com")),
            messagesCount = 3,
            unreadCount = 1,
            attachmentsCount = 0,
            expirationTime = 0L,
            labels = listOf(
                LabelContext("0", 1, 2, 2, 30, 0),
                LabelContext("7", 0, 1, 0, 0, 0)
            ),
            messages = null
        ),
        Conversation(
            id = "conversation2",
            subject = "subject2",
            senders = listOf(Correspondent("sender1", "sender1@pm.com")),
            receivers = listOf(
                Correspondent("recipient1", "recipient1@pm.com"),
                Correspondent("recipient2", "recipient2@pm.com")
            ),
            messagesCount = 1,
            unreadCount = 0,
            attachmentsCount = 4,
            expirationTime = 12_345L,
            labels = listOf(LabelContext("0", 0, 1, 3, 0, 4)),
            messages = null
        )
    )

    @Test
    fun verifyThatEmptyConversationApiModelListIsMappedProperly() {
        // given
        val conversation = listOf<ConversationApiModel>()
        val expected = emptyList<ConversationDatabaseModel>()

        // when
        val result = conversation.toListLocal(testUserId.id)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatEmptyConversationDatabaseModelListIsMappedProperly() {
        // given
        val conversation = listOf<ConversationDatabaseModel>()
        val expected = emptyList<Conversation>()

        // when
        val result = conversation.toDomainModelList()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatConversationApiModelIsMappedProperly() {

        // when
        val result = conversationsRemote[0].toLocal(testUserId.id)

        val expected = conversationsEntity[0]

        // then
        assertEquals(expected, result)
    }


    @Test
    fun verifyThatConversationDatabaseModelIsMappedProperly() {

        // when
        val result = conversationsEntity[0].toDomainModel()

        val expected = conversations[0]

        // then
        assertEquals(expected, result)
    }


    @Test
    fun verifyThatConversationApiModelListIsMappedProperly() {

        // when
        val result = conversationsRemote.toListLocal(testUserId.id)

        val expected = conversationsEntity

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatConversationDatabaseModelListIsMappedProperly() {

        // when
        val result = conversationsEntity.toDomainModelList()

        val expected = conversations

        // then
        assertEquals(expected, result)
    }
}
