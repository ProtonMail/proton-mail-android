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

import app.cash.turbine.test
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.messages.receive.ServerMessage
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.details.data.remote.model.ConversationResponse
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.LabelContextDatabaseModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationsResponse
import ch.protonmail.android.mailbox.data.remote.model.CorrespondentApiModel
import ch.protonmail.android.mailbox.data.remote.model.LabelContextApiModel
import ch.protonmail.android.mailbox.data.remote.worker.DeleteConversationsRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.LabelConversationsRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsReadRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsUnreadRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.UnlabelConversationsRemoteWorker
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import ch.protonmail.android.mailbox.domain.model.LabelContext
import ch.protonmail.android.mailbox.domain.model.MessageDomainModel
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
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
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.toDuration

private const val NO_MORE_CONVERSATIONS_ERROR_CODE = 723_478

class ConversationsRepositoryImplTest : CoroutinesTest, ArchTest {

    private val testUserId = UserId("id")

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

    @RelaxedMockK
    private lateinit var labelConversationsRemoteWorker: LabelConversationsRemoteWorker.Enqueuer

    @RelaxedMockK
    private lateinit var unlabelConversationsRemoteWorker: UnlabelConversationsRemoteWorker.Enqueuer

    @RelaxedMockK
    private lateinit var deleteConversationsRemoteWorker: DeleteConversationsRemoteWorker.Enqueuer

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var conversationsRepository: ConversationsRepositoryImpl

    private val conversationId = "conversationId"
    private val conversationId1 = "conversationId1"
    private val userId = UserId("userId")

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
                markConversationsUnreadRemoteWorker,
                labelConversationsRemoteWorker,
                unlabelConversationsRemoteWorker,
                deleteConversationsRemoteWorker
            )
    }

    @Test
    fun verifyConversationsAreFetchedFromLocalInitially() {
        runBlockingTest {
            // given
            val parameters = buildGetConversationsParameters()
            coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(listOf())
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
            val parameters = buildGetConversationsParameters()

            val conversationsEntity = conversationsRemote.conversationResponse.toListLocal(testUserId.id)
            coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(conversationsEntity)
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
            val parameters = buildGetConversationsParameters()

            coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(emptyList())
            coEvery { conversationDao.insertOrUpdate(*anyVararg()) } returns Unit
            coEvery { api.fetchConversations(any()) } returns conversationsRemote

            val expectedConversations = conversationsRemote.conversationResponse.toListLocal(testUserId.id)

            // when
            conversationsRepository.getConversations(parameters).test {

                // then
                val actualLocalItems = expectItem() as DataResult.Success
                assertEquals(ResponseSource.Local, actualLocalItems.source)

                coVerify { api.fetchConversations(parameters) }
                coVerify { conversationDao.insertOrUpdate(*expectedConversations.toTypedArray()) }

                expectNoEvents()
            }
        }

    @Test
    fun verifyGetConversationsThrowsExceptionWhenFetchingDataFromApiWasNotSuccessful() = runBlocking {
        // given
        val parameters = buildGetConversationsParameters()
        val errorMessage = "Test - Bad Request"

        coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(emptyList())
        coEvery { conversationDao.insertOrUpdate(*anyVararg()) } returns Unit
        coEvery { api.fetchConversations(any()) } throws IOException(errorMessage)

        // when
        conversationsRepository.getConversations(parameters)
            .test(timeout = 3.toDuration(TimeUnit.SECONDS)) {
                // Then
                val actualItem = expectItem() as DataResult.Success
                assertEquals(ResponseSource.Local, actualItem.source)
                assertEquals(errorMessage, expectError().message)
            }
    }

    @Test
    fun verifyGetConversationsReturnsLocalDataWhenFetchingFromApiFails() = runBlocking {
        // given
        val parameters = buildGetConversationsParameters()
        val errorMessage = "Api call failed"

        coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(
            listOf(buildConversationDatabaseModel())
        )
        coEvery { conversationDao.insertOrUpdate(*anyVararg()) } returns Unit
        coEvery { api.fetchConversations(any()) } throws IOException(errorMessage)

        // when
        conversationsRepository.getConversations(parameters)
            .test(timeout = 3.toDuration(TimeUnit.SECONDS)) {
                // Then
                val firstActualItem = expectItem() as DataResult.Success
                assertEquals(ResponseSource.Local, firstActualItem.source)

                assertEquals(errorMessage, expectError().message)

                val expectedLocalConversations = listOf(
                    Conversation(
                        conversationId,
                        "subject",
                        listOf(Correspondent("sender-name", "email@proton.com")),
                        listOf(Correspondent("receiver-name", "email-receiver@proton.com")),
                        1,
                        0,
                        0,
                        0,
                        emptyList(),
                        null
                    )
                )
                assertEquals(expectedLocalConversations, firstActualItem.value)
            }

    }

    @Test
    fun verifyGetConversationsEmitNoMoreConversationsErrorWhenRemoteReturnsEmptyList() = runBlocking {
        // given
        val parameters = buildGetConversationsParameters()
        val conversationsEntity = conversationsRemote.conversationResponse.toListLocal(testUserId.id)
        val emptyConversationsResponse = ConversationsResponse(0, emptyList())
        coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(conversationsEntity)
        coEvery { conversationDao.insertOrUpdate(*anyVararg()) } returns Unit
        coEvery { api.fetchConversations(any()) } returns emptyConversationsResponse

        // when
        conversationsRepository.getConversations(parameters).test {

            // then
            val first = expectItem() as DataResult.Error.Remote
            assertEquals("No conversations", first.message)
            assertEquals(NO_MORE_CONVERSATIONS_ERROR_CODE, first.protonCode)

            val second = expectItem() as DataResult.Success
            assertEquals(ResponseSource.Local, second.source)

            expectNoEvents()
        }
    }

    @Test
    fun verifyLocalConversationWithMessagesIsReturnedWhenDataIsAvailableInTheLocalDB() {
        runBlocking {
            // given
            val conversationDbModel = buildConversationDatabaseModel()
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
            coEvery { messageDao.observeAllMessagesInfoFromConversation(conversationId) } returns flowOf(
                listOf(message)
            )
            coEvery { messageDao.findAttachmentsByMessageId(any()) } returns flowOf(emptyList())
            coEvery { conversationDao.observeConversation(conversationId, userId.id) } returns flowOf(
                conversationDbModel
            )

            // when
            val result = conversationsRepository.getConversation(conversationId, userId).take(1).toList()

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
    fun verifyConversationIsEmittedWithUpdatedDataWhenOneOfItsMessagesChangesInTheLocalDB() {
        runBlocking {
            // given
            val conversationDbModel = buildConversationDatabaseModel()
            val message = Message(
                messageId = "messageId9238483",
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
            val starredMessage = message.copy().apply {
                isStarred = true
                addLabels(listOf("10"))
            }
            coEvery { messageDao.observeAllMessagesInfoFromConversation(conversationId) } returns flowOf(
                listOf(message), listOf(starredMessage)
            )
            coEvery { messageDao.findAttachmentsByMessageId(any()) } returns flowOf(emptyList())
            coEvery { conversationDao.observeConversation(conversationId, userId.id) } returns flowOf(
                conversationDbModel
            )

            // when
            val result = conversationsRepository.getConversation(conversationId, userId).take(2).toList()

            // then
            val expectedMessage = MessageDomainModel(
                "messageId9238483",
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
                labelsIds = listOf("1", "2", "10")
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
    fun verifyConversationIsNotEmittedAgainIfItsValueDidntChange() {
        runBlocking {
            // given
            val conversationDbModel = buildConversationDatabaseModel()
            val message = Message(
                messageId = "messageId9238484",
                conversationId = conversationId,
                subject = "subject1232",
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
            coEvery { messageDao.observeAllMessagesInfoFromConversation(conversationId) } returns flowOf(
                listOf(message), listOf(message)
            )
            coEvery { messageDao.findAttachmentsByMessageId(any()) } returns flowOf(emptyList())
            coEvery { conversationDao.observeConversation(conversationId, userId.id) } returns flowOf(
                conversationDbModel
            )

            // when
            val result = conversationsRepository.getConversation(conversationId, userId).take(2).toList()

            // then
            val expectedMessage = MessageDomainModel(
                "messageId9238484",
                conversationId,
                "subject1232",
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
            assertEquals(DataResult.Processing(ResponseSource.Remote), result[1])
        }
    }

    @Test
    fun verifyConversationIsFetchedFromRemoteDataSourceAndStoredLocallyWhenNotAvailableInDb() {
        runBlocking {
            // given
            val conversationApiModel = ConversationApiModel(
                id = conversationId,
                order = 0L,
                subject = "subject",
                senders = listOf(
                    CorrespondentApiModel("sender-name", "email@proton.com")
                ),
                recipients = listOf(
                    CorrespondentApiModel("receiver-name", "email-receiver@proton.com")
                ),
                numMessages = 1,
                numUnread = 0,
                numAttachments = 0,
                expirationTime = 0,
                size = 1L,
                labels = emptyList(),
                time = 357
            )
            val apiMessage = ServerMessage(ID = "messageId23842737", conversationId)
            val conversationResponse = ConversationResponse(
                0,
                conversationApiModel,
                listOf(apiMessage)
            )
            val expectedMessage = Message(messageId = "messageId23842737", conversationId)
            val dbFlow =
                MutableSharedFlow<ConversationDatabaseModel?>(replay = 2, onBufferOverflow = BufferOverflow.SUSPEND)
            coEvery { api.fetchConversation(conversationId, userId) } returns conversationResponse
            coEvery { messageDao.observeAllMessagesInfoFromConversation(conversationId) } returns flowOf(emptyList())
            coEvery { conversationDao.observeConversation(conversationId, userId.id) } returns dbFlow
            every { messageFactory.createMessage(apiMessage) } returns expectedMessage
            val expectedConversationDbModel = buildConversationDatabaseModel()
            coEvery { conversationDao.insertOrUpdate(expectedConversationDbModel) } answers {
                dbFlow.tryEmit(
                    expectedConversationDbModel
                )
            }

            // when
            conversationsRepository.getConversation(conversationId, userId)
                .test(timeout = 3.toDuration(TimeUnit.SECONDS)) {
                    dbFlow.emit(null)

                    // then
                    assertEquals(DataResult.Processing(ResponseSource.Remote), expectItem())
                    assertEquals(ResponseSource.Remote, (expectItem() as DataResult.Success).source)
                    coVerify { messageDao.saveMessages(listOf(expectedMessage)) }
                    coVerify { conversationDao.insertOrUpdate(expectedConversationDbModel) }
                }

        }
    }

    @Test
    fun verifyGetConversationsReThrowsCancellationExceptionWithoutEmittingError() {
        runBlockingTest {
            // given
            val parameters = buildGetConversationsParameters(null)
            coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(emptyList())
            coEvery { api.fetchConversations(parameters) } throws CancellationException("Cancelled")

            // when
            conversationsRepository.getConversations(parameters)
                .test(timeout = 3.toDuration(TimeUnit.SECONDS)) {
                    // then
                    val actual = expectItem() as DataResult.Success
                    assertEquals(ResponseSource.Local, actual.source)
                }
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreMarkedRead() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val message = Message()
            coEvery { conversationDao.updateNumUnreadMessages(0, any()) } just runs
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOf(message, message)
            coEvery { messageDao.saveMessage(any()) } returns 123
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.markRead(conversationIds, userId)

            // then
            coVerify {
                conversationDao.updateNumUnreadMessages(0, conversationId)
                conversationDao.updateNumUnreadMessages(0, conversationId1)
            }
            coVerify(exactly = 4) {
                messageDao.saveMessage(message)
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreMarkedUnread() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val mailboxLocation = Constants.MessageLocationType.ARCHIVE
            val locationId = Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
            val message = Message(
                location = mailboxLocation.messageLocationTypeValue
            )
            val message2 = Message(
                location = mailboxLocation.messageLocationTypeValue
            )
            val unreadMessages = 0
            coEvery { conversationDao.findConversation(any(), any()) } returns
                mockk {
                    every { numUnread } returns unreadMessages
                }
            coEvery { conversationDao.updateNumUnreadMessages(unreadMessages + 1, any()) } just runs
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOf(message, message2)
            coEvery { messageDao.saveMessage(any()) } returns 123
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.markUnread(conversationIds, UserId("id"), mailboxLocation, locationId)

            // then
            coVerify {
                conversationDao.updateNumUnreadMessages(unreadMessages + 1, conversationId)
                conversationDao.updateNumUnreadMessages(unreadMessages + 1, conversationId1)
            }
            coVerify(exactly = 2) {
                messageDao.saveMessage(message)
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenMarkUnreadIsCalled() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val mailboxLocation = Constants.MessageLocationType.ARCHIVE
            val locationId = Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
            coEvery { conversationDao.findConversation(any(), any()) } returns null
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.markUnread(conversationIds, UserId("id"), mailboxLocation, locationId)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreStarred() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val conversationLabels = listOf(
                LabelContextDatabaseModel("10", 2, 4, 123, 123, 1),
                LabelContextDatabaseModel("2", 0, 3, 123, 123, 0)
            )
            val testMessageId = "messageId"
            val message = Message(
                messageId = testMessageId,
                time = 123
            )
            coEvery { conversationDao.findConversation(any(), any()) } returns
                mockk {
                    every { labels } returns conversationLabels
                    every { numUnread } returns 2
                    every { numMessages } returns 7
                    every { size } returns 123
                    every { numAttachments } returns 1
                }
            coEvery { conversationDao.updateLabels(any(), any()) } just runs
            coEvery { messageDao.findAttachmentsByMessageId(testMessageId) } returns flowOf(emptyList())
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOf(message, message)
            coEvery { messageDao.updateStarred(testMessageId, true) } just runs
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.star(conversationIds, userId)

            // then
            coVerify(exactly = 2) {
                conversationDao.updateLabels(any(), any())
            }
            coVerify(exactly = 4) {
                messageDao.updateStarred(any(), true)
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenStarIsCalled() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val testMessageId = "messageId"
            val message = Message(
                messageId = testMessageId,
                time = 123
            )
            coEvery { conversationDao.findConversation(any(), any()) } returns null
            coEvery { messageDao.findAttachmentsByMessageId(testMessageId) } returns flowOf(emptyList())
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOf(message, message)
            coEvery { messageDao.updateStarred(testMessageId, true) } just runs
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.star(conversationIds, userId)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreUnstarred() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val conversationLabels = listOf(
                LabelContextDatabaseModel("10", 2, 4, 123, 123, 1),
                LabelContextDatabaseModel("2", 0, 3, 123, 123, 0)
            )
            val testMessageId = "messageId"
            val message = Message(messageId = testMessageId)
            coEvery { conversationDao.findConversation(any(), any()) } returns
                mockk {
                    every { labels } returns conversationLabels
                    every { numUnread } returns 2
                    every { numMessages } returns 7
                    every { size } returns 123
                    every { numAttachments } returns 1
                }

            coEvery { conversationDao.updateLabels(any(), any()) } just runs
            coEvery { messageDao.findAttachmentsByMessageId(testMessageId) } returns flowOf(emptyList())
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOf(message, message)
            coEvery { messageDao.updateStarred(testMessageId, false) } just runs
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.unstar(conversationIds, userId)

            // then
            coVerify(exactly = 2) {
                conversationDao.updateLabels(any(), any())
            }
            coVerify(exactly = 4) {
                messageDao.updateStarred(testMessageId, false)
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenUnstarIsCalled() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            coEvery { conversationDao.findConversation(any(), any()) } returns null
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.unstar(conversationIds, userId)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreMovedToFolder() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val folderId = "folderId"
            val inboxId = "0"
            val starredId = "10"
            val allMailId = "5"
            val message = Message(
                time = 123,
                allLabelIDs = listOf(inboxId, allMailId),
            )
            val conversationLabels = listOf(
                LabelContextDatabaseModel(allMailId, 0, 2, 123, 123, 1),
                LabelContextDatabaseModel(starredId, 0, 2, 123, 123, 1),
                LabelContextDatabaseModel(inboxId, 0, 2, 123, 123, 0)
            )
            val label: Label = mockk {
                every { exclusive } returns false
            }
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOf(message, message)
            coEvery { messageDao.findLabelById(any()) } returns label
            coEvery { conversationDao.findConversation(any(), any()) } returns
                mockk {
                    every { labels } returns conversationLabels
                    every { numUnread } returns 0
                    every { numMessages } returns 2
                    every { size } returns 123
                    every { numAttachments } returns 1
                }
            coEvery { conversationDao.updateLabels(any(), any()) } just runs
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.moveToFolder(conversationIds, userId, folderId)

            // then
            coVerify(exactly = 2) {
                messageDao.saveMessages(any())
            }
            coVerify(exactly = 2) {
                conversationDao.updateLabels(any(), conversationId1)
            }
            coVerify(exactly = 2) {
                conversationDao.updateLabels(any(), conversationId1)
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenMoveToFolderIsCalled() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val folderId = "folderId"
            val inboxId = "0"
            val allMailId = "5"
            val message = Message(
                time = 123,
                allLabelIDs = listOf(inboxId, allMailId),
            )
            val label: Label = mockk {
                every { exclusive } returns false
            }
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOf(message, message)
            coEvery { messageDao.findLabelById(any()) } returns label
            coEvery { conversationDao.findConversation(any(), any()) } returns null
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.moveToFolder(conversationIds, userId, folderId)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreDeleted() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val currentFolderId = "folderId"
            val message1 = Message().apply {
                allLabelIDs = listOf("0", "5")
            }
            val message2 = Message().apply {
                allLabelIDs = listOf("3", "5")
            }
            val listOfMessages = listOf(message1, message2)
            val conversationLabels = listOf(
                LabelContextDatabaseModel("0", 0, 2, 123, 123, 1),
                LabelContextDatabaseModel("3", 0, 2, 123, 123, 1),
                LabelContextDatabaseModel("5", 0, 2, 123, 123, 0)
            )
            coEvery { conversationDao.deleteConversation(any(), userId.id) } just runs
            coEvery { conversationDao.findConversation(any(), any()) } returns
                mockk {
                    every { labels } returns conversationLabels
                }
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOfMessages
            coEvery { messageDao.saveMessages(listOfMessages) } just runs

            // when
            conversationsRepository.delete(conversationIds, userId, currentFolderId)

            // then
            coVerify { conversationDao.updateLabels(any(), conversationId1) }
            coVerify { conversationDao.updateLabels(any(), conversationId1) }
            coVerify(exactly = 2) { messageDao.saveMessages(any()) }
        }
    }

    @Test
    fun verifyMessagesAndConversationsAreLabeled() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val labelId = "labelId"
            val message = mockk<Message> {
                every { time } returns 123
                every { addLabels(any()) } just runs
            }
            val listOfMessages = listOf(message, message)
            val conversationLabels = listOf(
                LabelContextDatabaseModel("5", 0, 2, 123, 123, 1),
                LabelContextDatabaseModel("0", 0, 2, 123, 123, 0)
            )
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOfMessages
            coEvery { messageDao.saveMessage(message) } returns 123
            coEvery { conversationDao.findConversation(any(), userId.id) } returns
                mockk {
                    every { labels } returns conversationLabels
                    every { numUnread } returns 0
                    every { numMessages } returns 2
                    every { size } returns 123
                    every { numAttachments } returns 1
                }

            coEvery { conversationDao.updateLabels(any(), any()) } just runs
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.label(conversationIds, userId, labelId)

            // then
            coVerify(exactly = 4) { messageDao.saveMessage(message) }
            coVerify { conversationDao.updateLabels(any(), conversationId1) }
            coVerify { conversationDao.updateLabels(any(), conversationId1) }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenLabelIsCalled() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val labelId = "labelId"
            val message = mockk<Message> {
                every { time } returns 123
                every { addLabels(any()) } just runs
            }
            val listOfMessages = listOf(message, message)
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOfMessages
            coEvery { messageDao.saveMessage(message) } returns 123
            coEvery { conversationDao.findConversation(any(), userId.id) } returns null
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.label(conversationIds, userId, labelId)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyMessagesAndConversationsAreUnlabeled() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val labelId = "labelId"
            val message = mockk<Message> {
                every { removeLabels(any()) } just runs
            }
            val listOfMessages = listOf(message, message)
            val conversationLabels = listOf(
                LabelContextDatabaseModel("5", 0, 2, 123, 123, 1),
                LabelContextDatabaseModel("0", 0, 2, 123, 123, 0),
                LabelContextDatabaseModel("labelId", 0, 2, 123, 123, 0)
            )
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOfMessages
            coEvery { messageDao.saveMessage(message) } returns 123
            coEvery { conversationDao.findConversation(any(), userId.id) } returns
                mockk {
                    every { labels } returns conversationLabels
                }

            coEvery { conversationDao.updateLabels(any(), any()) } just runs
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.unlabel(conversationIds, userId, labelId)

            // then
            coVerify(exactly = 4) {
                messageDao.saveMessage(message)
            }
            coVerify {
                conversationDao.updateLabels(any(), conversationId1)
            }
            coVerify {
                conversationDao.updateLabels(any(), conversationId1)
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenUnlabelIsCalled() {
        runBlockingTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val labelId = "labelId"
            val message = mockk<Message> {
                every { removeLabels(any()) } just runs
            }
            val listOfMessages = listOf(message, message)
            coEvery { messageDao.findAllMessagesInfoFromConversation(any()) } returns listOfMessages
            coEvery { messageDao.saveMessage(message) } returns 123
            coEvery { conversationDao.findConversation(any(), userId.id) } returns null
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.unlabel(conversationIds, userId, labelId)

            // then
            assertEquals(expectedResult, result)
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
        senders = listOf(),
        recipients = listOf(),
        numMessages = 0,
        numUnread = 0,
        numAttachments = 0,
        expirationTime = 0L,
        size = 0,
        labels = labels,
        time = 357
    )

    private fun buildGetConversationsParameters(
        oldestConversationTimestamp: Long? = 1616496670,
        pageSize: Int? = 50
    ) = GetConversationsParameters(
        locationId = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
        userId = testUserId,
        oldestConversationTimestamp = oldestConversationTimestamp,
        pageSize = pageSize!!
    )

    private fun buildConversationDatabaseModel(): ConversationDatabaseModel =
        ConversationDatabaseModel(
            conversationId,
            0L,
            userId.id,
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

}
