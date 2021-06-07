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
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.messages.receive.ServerMessage
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.details.data.remote.model.ConversationResponse
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.LabelContextDatabaseModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationsResponse
import ch.protonmail.android.mailbox.data.remote.model.CorrespondentApiModel
import ch.protonmail.android.mailbox.data.remote.model.LabelContextApiModel
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsReadRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsUnreadRemoteWorker
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import ch.protonmail.android.mailbox.domain.model.LabelContext
import ch.protonmail.android.mailbox.domain.model.MessageDomainModel
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val NO_MORE_CONVERSATIONS_ERROR_CODE = 723478

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

    @RelaxedMockK
    private lateinit var conversationDao: ConversationDao

    @RelaxedMockK
    private lateinit var messageDao: MessageDao

    @RelaxedMockK
    private lateinit var messageFactory: MessageFactory

    @RelaxedMockK
    private lateinit var markConversationsReadRemoteWorker: MarkConversationsReadRemoteWorker.Enqueuer

    @RelaxedMockK
    private lateinit var markConversationsUnreadRemoteWorker: MarkConversationsUnreadRemoteWorker.Enqueuer

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var conversationsRepository: ConversationsRepositoryImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        conversationsRepository =
            ConversationsRepositoryImpl(
                conversationDao,
                messageDao,
                api,
                messageFactory,
                markConversationsReadRemoteWorker,
                markConversationsUnreadRemoteWorker
            )
    }

    @Test
    fun verifyConversationsAreFetchedFromLocalInitially() {
        runBlockingTest {
            // given
            val parameters = GetConversationsParameters(
                locationId = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
                userId = testUserId,
                oldestConversationTimestamp = 1616496670,
                pageSize = 2
            )
            coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(listOf())
            coEvery { api.fetchConversations(any()) } returns conversationsRemote

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
                locationId = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
                userId = testUserId,
                oldestConversationTimestamp = 1616496670,
                pageSize = 5,
            )

            val conversationsEntity = conversationsRemote.conversationResponse.toListLocal(testUserId.s)
            coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(conversationsEntity)
            coEvery { api.fetchConversations(any()) } returns conversationsRemote

            // when
            val result = conversationsRepository.getConversations(parameters).first()

            // then
            assertEquals(DataResult.Success(ResponseSource.Local, conversationsOrdered), result)

        }

    @Test
    fun verifyGetConversationsFetchesDataFromRemoteApiAndStoresResultInTheLocalDatabaseWhenResponseIsSuccessful() =
        runBlocking {
            // given
            val parameters = GetConversationsParameters(
                locationId = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
                userId = testUserId,
                oldestConversationTimestamp = 1616496670,
                pageSize = 5
            )

            coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(emptyList())
            coEvery { conversationDao.insertOrUpdate(*anyVararg()) } returns Unit
            coEvery { api.fetchConversations(any()) } returns conversationsRemote

            // when
            val result = conversationsRepository.getConversations(parameters).take(1).toList()

            // Then
            val actualLocalItems = result[0] as DataResult.Success
            assertEquals(ResponseSource.Local, actualLocalItems.source)

            val expectedConversations = conversationsRemote.conversationResponse.toListLocal(testUserId.s)
            coVerify { api.fetchConversations(parameters) }
            coVerify { conversationDao.insertOrUpdate(*expectedConversations.toTypedArray()) }
        }

    @Test
    fun verifyGetConversationsEmitErrorWhenFetchingDataFromApiWasNotSuccessful() = runBlocking {
        // given
        val parameters = GetConversationsParameters(
            locationId = "8234",
            userId = testUserId,
            oldestConversationTimestamp = 823848238
        )

        coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(emptyList())
        coEvery { conversationDao.insertOrUpdate(*anyVararg()) } returns Unit
        coEvery { api.fetchConversations(any()) } throws IOException("Test - Bad Request")

        // when
        val result = conversationsRepository.getConversations(parameters).take(2).toList()

        // Then
        val actualApiError = result[0] as DataResult.Error
        assertEquals("Test - Bad Request", actualApiError.message)

        val actualLocalItems = result[1] as DataResult.Success
        assertEquals(ResponseSource.Local, actualLocalItems.source)
    }

    @Test
    fun verifyGetConversationsReturnsLocalDataWhenFetchingFromApiFails() = runBlocking {
        // given
        val parameters = GetConversationsParameters(
            locationId = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
            userId = testUserId,
            oldestConversationTimestamp = 1616496670,
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
        val result = conversationsRepository.getConversations(parameters).take(2).toList()

        // Then
        val actualError = result[0] as DataResult.Error.Remote
        assertNotNull(actualError)

        val actualLocalItems = result[1] as DataResult.Success
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
    }

    @Test
    fun verifyGetConversationsEmitNoMoreConversationsErrorWhenRemoteReturnsEmptyList() = runBlocking {
        // given
        val parameters = GetConversationsParameters(
            locationId = "8234",
            userId = testUserId,
            oldestConversationTimestamp = 823848238
        )
        val conversationsEntity = conversationsRemote.conversationResponse.toListLocal(testUserId.s)
        val emptyConversationsResponse = ConversationsResponse(0, emptyList())
        coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(conversationsEntity)
        coEvery { conversationDao.insertOrUpdate(*anyVararg()) } returns Unit
        coEvery { api.fetchConversations(any()) } returns emptyConversationsResponse

        // when
        val result = conversationsRepository.getConversations(parameters).take(2).toList()

        // Then
        val actualApiError = result[0] as DataResult.Error.Remote
        assertEquals("No conversations", actualApiError.message)
        assertEquals(NO_MORE_CONVERSATIONS_ERROR_CODE, actualApiError.protonCode)

        val actualLocalItems = result[1] as DataResult.Success
        assertEquals(ResponseSource.Local, actualLocalItems.source)
    }

    @Test
    fun verifyLocalConversationWithMessagesIsReturnedWhenDataIsAvailableInTheLocalDB() {
        runBlocking {
            // given
            val conversationId = "conversationId234823"
            val userId = "userId82384e2"
            val conversationDbModel = ConversationDatabaseModel(
                conversationId,
                0L,
                userId,
                "subject",
                listOf(
                    MessageSender("sender-name", "email@proton.com")
                ),
                listOf(
                    MessageRecipient("receiver-name", "email-receiver@proton.com")
                ),
                1,
                0,
                0,
                0,
                1,
                emptyList()
            )
            val message = Message(
                messageId = "messageId9238482",
                conversationId = conversationId,
                subject = "subject1231",
                Unread = false,
                sender = MessageSender("senderName", "sender@protonmail.ch"),
                toList = listOf(),
                time = 82374723L,
                numAttachments = 1,
                expirationTime = 0L,
                isReplied = false,
                isRepliedAll = true,
                isForwarded = false,
                allLabelIDs = listOf("1", "2")
            )
            coEvery { messageDao.findAllMessageFromAConversation(conversationId) } returns flowOf(listOf(message))
            coEvery { conversationDao.getConversation(conversationId, userId) } returns flowOf(
                conversationDbModel
            )

            // when
            val result = conversationsRepository.getConversation(conversationId, Id(userId)).take(1).toList()

            // then
            val expectedMessage = MessageDomainModel(
                "messageId9238482",
                conversationId,
                "subject1231",
                false,
                Correspondent("senderName", "sender@protonmail.ch"),
                listOf(),
                82374723L,
                1,
                0L,
                isReplied = false,
                isRepliedAll = true,
                isForwarded = false,
                ccReceivers = emptyList(),
                bccReceivers = emptyList(),
                labelsIds = listOf("1", "2")
            )
            val expectedConversation = Conversation(
                conversationId,
                "subject",
                listOf(
                    Correspondent("sender-name", "email@proton.com")
                ),
                listOf(
                    Correspondent("receiver-name", "email-receiver@proton.com")
                ),
                1,
                0,
                0,
                0,
                emptyList(),
                listOf(
                    expectedMessage
                )
            )
            assertEquals(DataResult.Success(ResponseSource.Local, expectedConversation), result[0])
        }
    }

    @Test
    fun verifyConversationIsFetchedFromRemoteDataSourceAndStoredLocallyWhenNotAvailableInDb() {
        runBlocking {
            // given
            val conversationId = "conversationId2347393"
            val userId = "userId82sd8238"
            val conversationApiModel = ConversationApiModel(
                conversationId,
                0L,
                "subject",
                listOf(
                    CorrespondentApiModel("sender-name", "email@proton.com")
                ),
                listOf(
                    CorrespondentApiModel("receiver-name", "email-receiver@proton.com")
                ),
                1,
                0,
                0,
                0,
                1L,
                emptyList()
            )
            val apiMessage = ServerMessage(ID = "messageId23842737", conversationId)
            val conversationResponse = ConversationResponse(
                0,
                conversationApiModel,
                listOf(apiMessage)
            )
            val expectedMessage = Message(messageId = "messageId23842737", conversationId)
            coEvery { api.fetchConversation(conversationId, Id(userId)) } returns conversationResponse
            coEvery { messageDao.findAllMessageFromAConversation(conversationId) } returns flowOf(emptyList())
            coEvery { conversationDao.getConversation(conversationId, userId) } throws Exception("no conversations")
            every { messageFactory.createMessage(apiMessage) } returns expectedMessage

            // when
            val result = conversationsRepository.getConversation(conversationId, Id(userId)).take(3).toList()

            // then
            val expectedConversationDbModel = ConversationDatabaseModel(
                conversationId,
                0L,
                userId,
                "subject",
                listOf(
                    MessageSender("sender-name", "email@proton.com")
                ),
                listOf(
                    MessageRecipient("receiver-name", "email-receiver@proton.com")
                ),
                1,
                0,
                0,
                0,
                1,
                emptyList()
            )
            val errorMessage =
                "Failed to read from Source of Truth. key: ConversationStoreKey(conversationId=conversationId2347393, userId=Id(s=userId82sd8238))"
            val actualLocalError = result[0] as DataResult.Error.Local
            assertEquals(errorMessage, actualLocalError.message)
            assertEquals(DataResult.Processing(ResponseSource.Remote), result[1])
            coVerify { messageDao.saveMessages(expectedMessage) }
            coVerify { conversationDao.insertOrUpdate(expectedConversationDbModel) }
        }
    }

    @Test
    @Throws(CancellationException::class)
    fun verifyGetConversationsReThrowsCancellationExceptionWithoutEmittingError() {
        runBlocking {
            // given
            val parameters = GetConversationsParameters(
                locationId = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
                userId = testUserId,
                oldestConversationTimestamp = null
            )
            coEvery { api.fetchConversations(parameters) } throws CancellationException("Cancelled")

            // when
            try {
                conversationsRepository.getConversations(parameters).first()
            } catch (exception: CancellationException) {
                assertEquals("Cancelled", exception.message)
            }

            verify { conversationDao wasNot Called }
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreMarkedRead() {
        runBlockingTest {
            // given
            val conversation1 = "conversation1"
            val conversation2 = "conversation2"
            val conversationIds = listOf(conversation1, conversation2)
            val message = mockk<Message> {
                every { setIsRead(any()) } just runs
            }
            coEvery { conversationDao.updateNumUnreadMessages(0, any()) } just runs
            coEvery { messageDao.findAllMessageFromAConversation(any()) } returns flowOf(listOf(message, message))
            coEvery { messageDao.saveMessage(any()) } returns 123

            // when
            conversationsRepository.markRead(conversationIds)

            // then
            coVerify {
                conversationDao.updateNumUnreadMessages(0, conversation1)
                conversationDao.updateNumUnreadMessages(0, conversation2)
            }
            coVerify(exactly = 4) {
                messageDao.saveMessage(message)
            }
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreMarkedUnread() {
        runBlockingTest {
            // given
            val conversation1 = "conversation1"
            val conversation2 = "conversation2"
            val conversationIds = listOf(conversation1, conversation2)
            val mailboxLocation = Constants.MessageLocationType.ARCHIVE
            val message = mockk<Message> {
                every { setIsRead(any()) } just runs
                every { location } returns mailboxLocation.messageLocationTypeValue
            }
            val unreadMessages = 0
            every { conversationDao.getConversation(any(), any()) } returns flowOf(
                mockk {
                    every { numUnread } returns unreadMessages
                }
            )
            coEvery { conversationDao.updateNumUnreadMessages(unreadMessages + 1, any()) } just runs
            coEvery { messageDao.findAllMessageFromAConversation(any()) } returns flowOf(listOf(message, message))
            coEvery { messageDao.saveMessage(any()) } returns 123

            // when
            conversationsRepository.markUnread(conversationIds, UserId("id"), mailboxLocation)

            // then
            coVerify {
                conversationDao.updateNumUnreadMessages(unreadMessages + 1, conversation1)
                conversationDao.updateNumUnreadMessages(unreadMessages + 1, conversation2)
            }
            coVerify(exactly = 2) {
                messageDao.saveMessage(message)
            }
        }
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
