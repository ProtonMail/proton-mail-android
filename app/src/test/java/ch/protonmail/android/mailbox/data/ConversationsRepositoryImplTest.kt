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

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.LabelContextDatabaseModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationsResponse
import ch.protonmail.android.mailbox.data.remote.model.LabelContextApiModel
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import ch.protonmail.android.mailbox.domain.model.LabelContext
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ConversationsRepositoryImplTest : CoroutinesTest, ArchTest {

    private val testUserId = Id("id")

    private val conversationsRemote = ConversationsResponse(
        total = 5,
        listOf(
            getConversationApiModel(
                "conversation5", 0, "subject5",
                listOf(
                    LabelContextApiModel("0", 0, 1, 0, 0, 0),
                    LabelContextApiModel("7", 0, 1, 3, 0, 0)
                )
            ),
            getConversationApiModel(
                "conversation4", 2, "subject4",
                listOf(
                    LabelContextApiModel("0", 0, 1, 1, 0, 0)
                )
            ),
            getConversationApiModel(
                "conversation3", 3, "subject3",
                listOf(
                    LabelContextApiModel("0", 0, 1, 1, 0, 0),
                    LabelContextApiModel("7", 0, 1, 1, 0, 0)
                )
            ),
            getConversationApiModel(
                "conversation2", 1, "subject2",
                listOf(
                    LabelContextApiModel("0", 0, 1, 1, 0, 0)
                )
            ),
            getConversationApiModel(
                "conversation1", 4, "subject1",
                listOf(
                    LabelContextApiModel("0", 0, 1, 4, 0, 0)
                )
            )
        )
    )

    private val conversationsOrdered = listOf(
        getConversation(
            "conversation1", "subject1",
            listOf(
                LabelContext("0", 0, 1, 4, 0, 0)
            )
        ),
        getConversation(
            "conversation3", "subject3",
            listOf(
                LabelContext("0", 0, 1, 1, 0, 0),
                LabelContext("7", 0, 1, 1, 0, 0)
            )
        ),
        getConversation(
            "conversation4", "subject4",
            listOf(
                LabelContext("0", 0, 1, 1, 0, 0)
            )
        ),
        getConversation(
            "conversation2", "subject2",
            listOf(
                LabelContext("0", 0, 1, 1, 0, 0)
            )
        ),
        getConversation(
            "conversation5", "subject5",
            listOf(
                LabelContext("0", 0, 1, 0, 0, 0),
                LabelContext("7", 0, 1, 3, 0, 0)
            )
        )
    )

    @MockK
    private lateinit var conversationDao: ConversationDao

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var conversationsRepository: ConversationsRepositoryImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        conversationsRepository =
            ConversationsRepositoryImpl(
                conversationDao,
                api
            )
    }

    @Test
    fun verifyConversationsIsFetchedFromLocalInitially() {
        runBlockingTest {
            // given
            val parameters = GetConversationsParameters(
                page = 0,
                labelId = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
                userId = testUserId,
                pageSize = 2
            )
            coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(listOf())

            // when
            val result = conversationsRepository.getConversations(parameters).first()

            // then
            assertEquals(DataResult.Success(ResponseSource.Local, listOf()), result)
        }
    }

    @Test
    fun verifyConversationsAreRetrievedInCorrectOrder() =
        runBlockingTest {

            // given
            val parameters = GetConversationsParameters(
                page = 0,
                labelId = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
                userId = testUserId,
                pageSize = 5,
            )

            val conversationsEntity = conversationsRemote.conversationResponse.toListLocal(testUserId.s)
            coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(conversationsEntity)

            // when
            val result = conversationsRepository.getConversations(parameters).first()

            // then
            assertEquals(DataResult.Success(ResponseSource.Local, conversationsOrdered), result)

        }

    @Test
    fun verifyGetConversationsFetchesDataFromRemoteApiAndStoresResultInTheLocalDatabase() = runBlocking {
        // given
        val parameters = GetConversationsParameters(
            page = 0,
            labelId = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
            userId = testUserId,
            pageSize = 5
        )

        coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(emptyList())
        coEvery { conversationDao.insertOrUpdate(*anyVararg()) } returns Unit
        coEvery { api.fetchConversations(any()) } returns conversationsRemote

        // when
        val result = conversationsRepository.getConversations(parameters).take(3).toList()

        // Then
        val actualLocalItems = result[0] as DataResult.Success
        assertEquals(ResponseSource.Local, actualLocalItems.source)

        val actualProcessingResult = result[1] as DataResult.Processing
        assertEquals(ResponseSource.Remote, actualProcessingResult.source)

        val actualRemoteItems = result[2] as DataResult.Success
        assertEquals(ResponseSource.Remote, actualRemoteItems.source)

        val expectedConversations = conversationsRemote.conversationResponse.toListLocal(testUserId.s)
        coVerify { api.fetchConversations(parameters) }
        coVerify { conversationDao.insertOrUpdate(*expectedConversations.toTypedArray()) }
    }

    @Test
    fun verifyGetConversationsEmitsErrorAndReturnsLocalDataWhenFetchingFromApiFails() = runBlocking {
        // given
        val parameters = GetConversationsParameters(
            page = 0,
            labelId = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
            userId = testUserId,
            pageSize = 5
        )

        val senders = listOf(
            MessageSender("sender", "sender@pm.me")
        )
        val recipients = listOf(
            MessageRecipient("recipient", "recipient@pm.ch")
        )
        coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(
            listOf(
                ConversationDatabaseModel(
                    "conversationId234423",
                    3,
                    "userID",
                    "subject28348",
                    senders,
                    recipients,
                    3,
                    1,
                    4,
                    0,
                    0,
                    listOf(LabelContextDatabaseModel("labelId123", 1, 0, 0, 0, 0))
                )
            )
        )
        coEvery { conversationDao.insertOrUpdate(*anyVararg()) } returns Unit
        coEvery { api.fetchConversations(any()) } throws IOException("Api call failed")

        // when
        val result = conversationsRepository.getConversations(parameters).take(3).toList()

        // Then
        val actualLocalItems = result[0] as DataResult.Success
        assertEquals(ResponseSource.Local, actualLocalItems.source)
        val expectedLocalConversations = listOf(
            Conversation(
                "conversationId234423",
                "subject28348",
                listOf(Correspondent("sender", "sender@pm.me")),
                listOf(Correspondent("recipient", "recipient@pm.ch")),
                3,
                1,
                4,
                0,
                listOf(LabelContext("labelId123", 1, 0, 0, 0, 0)),
                null
            )
        )
        assertEquals(expectedLocalConversations, actualLocalItems.value)

        val actualProcessingResult = result[1] as DataResult.Processing
        assertEquals(ResponseSource.Remote, actualProcessingResult.source)

        val actualRemoteItems = result[2] as DataResult.Error
        assertEquals(ResponseSource.Remote, actualRemoteItems.source)
    }

    private fun getConversation(
        id: String,
        subject: String,
        labels: List<LabelContext>
    ) = Conversation(
        id = id,
        subject = subject,
        senders = listOf(),
        receivers = listOf(),
        messagesCount = 0,
        unreadCount = 0,
        attachmentsCount = 0,
        expirationTime = 0,
        labels = labels,
        messages = null
    )

    private fun getConversationApiModel(
        id: String,
        order: Long,
        subject: String,
        labels: List<LabelContextApiModel>
    ) = ConversationApiModel(
        id = id,
        order = order,
        subject = subject,
        listOf(),
        listOf(),
        0,
        0,
        0,
        0L,
        0,
        labels = labels
    )

}
