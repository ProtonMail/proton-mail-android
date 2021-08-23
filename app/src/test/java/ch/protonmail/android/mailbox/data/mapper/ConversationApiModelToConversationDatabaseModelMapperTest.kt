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

package ch.protonmail.android.mailbox.data.mapper

import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import io.mockk.mockk
import me.proton.core.domain.arch.map
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.invoke
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for [ConversationApiModelToConversationDatabaseModelMapper]
 */
class ConversationApiModelToConversationDatabaseModelMapperTest {

    private val mapper = ConversationApiModelToConversationDatabaseModelMapper(
        messageSenderMapper = mockk(),
        messageRecipientMapper = mockk(),
        labelMapper = mockk()
    )

    private val userId = UserId("userId")

    // region 1
    private val id1 = "id_1"
    private val order1: Long = 5
    private val subject1 = "Hello world"
    private val numMessages1 = 5
    private val numUnread1 = 3
    private val numAttachments1 = 2
    private val expirationTime1: Long = 257
    private val size1: Long = 987
    private val time1: Long = 524
    private val conversationApiModel1 = ConversationApiModel(
        id = id1,
        order = order1,
        subject = subject1,
        senders = emptyList(),
        recipients = emptyList(),
        numMessages = numMessages1,
        numUnread = numUnread1,
        numAttachments = numAttachments1,
        expirationTime = expirationTime1,
        size = size1,
        labels = emptyList(),
        time = time1
    )
    // endregion

    // region 2
    private val id2 = "id_2"
    private val order2: Long = 42
    private val subject2 = "What a beautiful day!"
    private val numMessages2 = 32
    private val numUnread2 = 12
    private val numAttachments2 = 8
    private val expirationTime2: Long = 725
    private val size2: Long = 385
    private val time2: Long = 562
    private val conversationApiModel2 = ConversationApiModel(
        id = id2,
        order = order2,
        subject = subject2,
        senders = emptyList(),
        recipients = emptyList(),
        numMessages = numMessages2,
        numUnread = numUnread2,
        numAttachments = numAttachments2,
        expirationTime = expirationTime2,
        size = size2,
        labels = emptyList(),
        time = time2
    )
    // endregion
    private val conversationsApiModels = listOf(
        conversationApiModel1,
        conversationApiModel2
    )

    @Test
    fun mapsSingleConversation() {
        // given
        val input = conversationApiModel1
        val expected = ConversationDatabaseModel(
            id = id1,
            order = order1,
            userId = userId.id,
            subject = subject1,
            senders = emptyList(),
            recipients = emptyList(),
            numMessages = numMessages1,
            numUnread = numUnread1,
            numAttachments = numAttachments1,
            expirationTime = expirationTime1,
            size = size1,
            labels = emptyList(),
        )

        // when
        val result = mapper { input.toDatabaseModel(userId) }

        // then
        assertEquals(expected, result)
    }

    @Test
    fun mapsEmptyList() {
        // given
        val input = emptyList<ConversationApiModel>()
        val expected = emptyList<ConversationDatabaseModel>()

        // when
        val result = input.map(mapper) { it.toDatabaseModel(userId) }

        // then
        assertEquals(expected, result)
    }

    @Test
    fun mapsListOfConversations() {
        // given
        val input = conversationsApiModels
        val expectedElement1 = ConversationDatabaseModel(
            id = id1,
            order = order1,
            userId = userId.id,
            subject = subject1,
            senders = emptyList(),
            recipients = emptyList(),
            numMessages = numMessages1,
            numUnread = numUnread1,
            numAttachments = numAttachments1,
            expirationTime = expirationTime1,
            size = size1,
            labels = emptyList(),
        )
        val expectedElement2 = ConversationDatabaseModel(
            id = id2,
            order = order2,
            userId = userId.id,
            subject = subject2,
            senders = emptyList(),
            recipients = emptyList(),
            numMessages = numMessages2,
            numUnread = numUnread2,
            numAttachments = numAttachments2,
            expirationTime = expirationTime2,
            size = size2,
            labels = emptyList(),
        )
        val expected = listOf(expectedElement1, expectedElement2)

        // when
        val result = input.map(mapper) { it.toDatabaseModel(userId) }

        // then
        assertEquals(expected, result)
    }
}

