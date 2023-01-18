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

package ch.protonmail.android.mailbox.data

import app.cash.turbine.test
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.messages.receive.ServerMessage
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.details.data.remote.model.ConversationResponse
import ch.protonmail.android.labels.data.remote.worker.LabelConversationsRemoteWorker
import ch.protonmail.android.labels.data.remote.worker.UnlabelConversationsRemoteWorker
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.UnreadCounterDao
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.LabelContextDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterEntity
import ch.protonmail.android.mailbox.data.mapper.ApiToDatabaseUnreadCounterMapper
import ch.protonmail.android.mailbox.data.mapper.ConversationApiModelToConversationDatabaseModelMapper
import ch.protonmail.android.mailbox.data.mapper.ConversationApiModelToConversationMapper
import ch.protonmail.android.mailbox.data.mapper.ConversationDatabaseModelToConversationMapper
import ch.protonmail.android.mailbox.data.mapper.ConversationsResponseToConversationsDatabaseModelsMapper
import ch.protonmail.android.mailbox.data.mapper.ConversationsResponseToConversationsMapper
import ch.protonmail.android.mailbox.data.mapper.CorrespondentApiModelToCorrespondentMapper
import ch.protonmail.android.mailbox.data.mapper.CorrespondentApiModelToMessageRecipientMapper
import ch.protonmail.android.mailbox.data.mapper.CorrespondentApiModelToMessageSenderMapper
import ch.protonmail.android.mailbox.data.mapper.DatabaseToDomainUnreadCounterMapper
import ch.protonmail.android.mailbox.data.mapper.LabelContextApiModelToLabelContextDatabaseModelMapper
import ch.protonmail.android.mailbox.data.mapper.LabelContextApiModelToLabelContextMapper
import ch.protonmail.android.mailbox.data.mapper.LabelContextDatabaseModelToLabelContextMapper
import ch.protonmail.android.mailbox.data.mapper.MessageRecipientToCorrespondentMapper
import ch.protonmail.android.mailbox.data.mapper.MessageSenderToCorrespondentMapper
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationsResponse
import ch.protonmail.android.mailbox.data.remote.model.CorrespondentApiModel
import ch.protonmail.android.mailbox.data.remote.model.CountsApiModel
import ch.protonmail.android.mailbox.data.remote.model.CountsResponse
import ch.protonmail.android.mailbox.data.remote.model.LabelContextApiModel
import ch.protonmail.android.mailbox.data.remote.worker.DeleteConversationsRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsReadRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsUnreadRemoteWorker
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.GetAllConversationsParameters
import ch.protonmail.android.mailbox.domain.model.GetOneConversationParameters
import ch.protonmail.android.mailbox.domain.model.LabelContext
import ch.protonmail.android.mailbox.domain.model.MessageDomainModel
import ch.protonmail.android.mailbox.domain.model.UnreadCounter
import ch.protonmail.android.usecase.message.ChangeMessagesReadStatus
import ch.protonmail.android.usecase.message.ChangeMessagesStarredStatus
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withTimeout
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.UnconfinedCoroutinesTest
import me.proton.core.test.kotlin.flowTest
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.seconds
import kotlin.time.toDuration

private const val STARRED_LABEL_ID = "10"

class ConversationsRepositoryImplTest : ArchTest by ArchTest(), CoroutinesTest by UnconfinedCoroutinesTest() {

    private val testUserId = UserId("id")

    private val conversationsRemote = ConversationsResponse(
        total = 5,
        listOf(
            buildConversationApiModel(
                "conversation5", 0, "subject5",
                listOf(
                    LabelContextApiModel("0", 0, 1, 0, 0, 0),
                    LabelContextApiModel("7", 0, 1, 3, 0, 0)
                )
            ),
            buildConversationApiModel(
                "conversation4", 2, "subject4",
                listOf(
                    LabelContextApiModel("0", 0, 1, 1, 0, 0)
                )
            ),
            buildConversationApiModel(
                "conversation3", 3, "subject3",
                listOf(
                    LabelContextApiModel("0", 0, 1, 1, 0, 0),
                    LabelContextApiModel("7", 0, 1, 1, 0, 0)
                )
            ),
            buildConversationApiModel(
                "conversation2", 1, "subject2",
                listOf(
                    LabelContextApiModel("0", 0, 1, 1, 0, 0)
                )
            ),
            buildConversationApiModel(
                "conversation1", 4, "subject1",
                listOf(
                    LabelContextApiModel("0", 0, 1, 4, 0, 0)
                )
            )
        )
    )

    private val conversationsOrdered = listOf(
        buildConversation(
            "conversation1", "subject1",
            listOf(
                LabelContext("0", 0, 1, 4, 0, 0)
            )
        ),
        buildConversation(
            "conversation3", "subject3",
            listOf(
                LabelContext("0", 0, 1, 1, 0, 0),
                LabelContext("7", 0, 1, 1, 0, 0)
            )
        ),
        buildConversation(
            "conversation4", "subject4",
            listOf(
                LabelContext("0", 0, 1, 1, 0, 0)
            )
        ),
        buildConversation(
            "conversation2", "subject2",
            listOf(
                LabelContext("0", 0, 1, 1, 0, 0)
            )
        ),
        buildConversation(
            "conversation5", "subject5",
            listOf(
                LabelContext("0", 0, 1, 0, 0, 0),
                LabelContext("7", 0, 1, 3, 0, 0)
            )
        )
    )

    private val userManager: UserManager = mockk {
        every { currentUserId } returns testUserId
        every { requireCurrentUserId() } returns testUserId
    }

    private val conversationDao: ConversationDao = mockk {
        coEvery { updateLabels(any(), any()) } just Runs
        coEvery { insertOrUpdate(*anyVararg()) } just Runs
    }

    private val messageDao: MessageDao = mockk {
        every { observeAllMessagesInfoFromConversation(any()) } returns flowOf(emptyList())
        coEvery { saveMessages(any()) } just Runs
    }

    private val unreadCounterDao: UnreadCounterDao = mockk {
        every { observeConversationsUnreadCounters(any()) } returns flowOf(emptyList())
        coEvery { insertOrUpdate(any<Collection<UnreadCounterEntity>>()) } just Runs
    }

    private val databaseProvider: DatabaseProvider = mockk {
        every { provideConversationDao(any()) } returns conversationDao
        every { provideMessageDao(any()) } returns messageDao
        every { provideUnreadCounterDao(any()) } returns unreadCounterDao
    }

    private val api: ProtonMailApiManager = mockk {
        coEvery { fetchConversationsCounts(testUserId) } returns CountsResponse(emptyList())
    }

    private val conversationApiModelToConversationMapper = ConversationApiModelToConversationMapper(
        CorrespondentApiModelToCorrespondentMapper(),
        LabelContextApiModelToLabelContextMapper()
    )

    private val databaseModelToConversationMapper = ConversationDatabaseModelToConversationMapper(
        MessageSenderToCorrespondentMapper(),
        MessageRecipientToCorrespondentMapper(),
        LabelContextDatabaseModelToLabelContextMapper()
    )

    private val apiToDatabaseConversationMapper = ConversationApiModelToConversationDatabaseModelMapper(
        CorrespondentApiModelToMessageSenderMapper(),
        CorrespondentApiModelToMessageRecipientMapper(),
        LabelContextApiModelToLabelContextDatabaseModelMapper()
    )

    private val messageFactory: MessageFactory = mockk(relaxed = true)

    private val labelsRepository: LabelRepository = mockk(relaxed = true)

    private val markConversationsReadRemoteWorker: MarkConversationsReadRemoteWorker.Enqueuer = mockk(relaxed = true)

    private val markConversationsUnreadRemoteWorker: MarkConversationsUnreadRemoteWorker.Enqueuer = mockk(relaxed = true)

    private val labelConversationsRemoteWorker: LabelConversationsRemoteWorker.Enqueuer = mockk(relaxed = true)

    private val conversationId = "conversationId"
    private val conversationId1 = "conversationId1"
    private val conversationId2 = "conversationId2"
    private val messageId1 = "messageId1"
    private val messageId2 = "messageId2"
    private val inboxLabelId = MessageLocationType.INBOX.messageLocationTypeValue.toString()
    private val trashLabelId = MessageLocationType.TRASH.messageLocationTypeValue.toString()
    private val starredLabelId = MessageLocationType.STARRED.messageLocationTypeValue.toString()
    private val labelId1 = "labelId1"
    private val labelId2 = "labelId2"
    private val folderId = "folderId"

    private val unlabelConversationsRemoteWorker: UnlabelConversationsRemoteWorker.Enqueuer = mockk(relaxed = true)
    private val deleteConversationsRemoteWorker: DeleteConversationsRemoteWorker.Enqueuer = mockk(relaxed = true)

    private val connectivityManager: NetworkConnectivityManager = mockk {
        every { isInternetConnectionPossible() } returns true
    }
    private val markUnreadLatestNonDraftMessageInLocation: MarkUnreadLatestNonDraftMessageInLocation = mockk {
        coEvery { this@mockk.invoke(any(), any(), any()) } just runs
    }

    private lateinit var conversationsRepository: ConversationsRepositoryImpl

    @BeforeTest
    fun setUp() {
        conversationsRepository = ConversationsRepositoryImpl(
            userManager = userManager,
            databaseProvider = databaseProvider,
            api = api,
            responseToConversationsMapper = ConversationsResponseToConversationsMapper(
                conversationApiModelToConversationMapper
            ),
            databaseToConversationMapper = databaseModelToConversationMapper,
            apiToDatabaseConversationMapper = apiToDatabaseConversationMapper,
            responseToDatabaseConversationsMapper = ConversationsResponseToConversationsDatabaseModelsMapper(
                apiToDatabaseConversationMapper
            ),
            messageFactory = messageFactory,
            databaseToDomainUnreadCounterMapper = DatabaseToDomainUnreadCounterMapper(),
            apiToDatabaseUnreadCounterMapper = ApiToDatabaseUnreadCounterMapper(),
            markConversationsReadWorker = markConversationsReadRemoteWorker,
            markConversationsUnreadWorker = markConversationsUnreadRemoteWorker,
            labelConversationsRemoteWorker = labelConversationsRemoteWorker,
            unlabelConversationsRemoteWorker = unlabelConversationsRemoteWorker,
            deleteConversationsRemoteWorker = deleteConversationsRemoteWorker,
            connectivityManager = connectivityManager,
            markUnreadLatestNonDraftMessageInLocation = markUnreadLatestNonDraftMessageInLocation,
            labelsRepository = labelsRepository,
            externalScope = TestScope(dispatchers.Io)
        )
    }

    @Test
    fun verifyConversationsAreFetchedFromLocalInitially() {
        coroutinesTest {
            // given
            val parameters = buildGetConversationsParameters()
            coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(listOf())
            coEvery { api.fetchConversations(any()) } returns conversationsRemote

            // when
            val result = conversationsRepository.observeConversations(parameters).first()

            // then
            assertEquals(DataResult.Success(ResponseSource.Local, listOf()), result)
        }
    }

    @Test
    fun verifyConversationsAreRetrievedInCorrectOrder() =
        coroutinesTest {
            // given
            val parameters = buildGetConversationsParameters()

            val conversationsEntity = apiToDatabaseConversationMapper
                .toDatabaseModels(conversationsRemote.conversations, testUserId)
            coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(conversationsEntity)
            coEvery { api.fetchConversations(any()) } returns conversationsRemote

            // when
            val result = conversationsRepository.observeConversations(parameters).first()

            // then
            assertEquals(DataResult.Success(ResponseSource.Local, conversationsOrdered), result)
        }

    @Test
    fun verifyGetConversationsFetchesDataFromRemoteApiAndStoresResultInTheLocalDatabaseWhenResponseIsSuccessful() =
        coroutinesTest {
            // given
            val parameters = buildGetConversationsParameters()

            coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(emptyList())
            coEvery { conversationDao.insertOrUpdate(*anyVararg()) } returns Unit
            coEvery { api.fetchConversations(any()) } returns conversationsRemote

            val expectedConversations = apiToDatabaseConversationMapper
                .toDatabaseModels(conversationsRemote.conversations, testUserId)

            // when
            conversationsRepository.observeConversations(parameters,).test {

                // then
                val actualLocalItems = awaitItem() as DataResult.Success
                assertEquals(ResponseSource.Local, actualLocalItems.source)

                coVerify { api.fetchConversations(parameters) }
                coVerify { conversationDao.insertOrUpdate(*expectedConversations.toTypedArray()) }

                val actualRemoteItems = awaitItem() as DataResult.Success
                assertEquals(ResponseSource.Remote, actualRemoteItems.source)
            }
        }

    @Test
    fun verifyGetConversationsThrowsExceptionWhenFetchingDataFromApiWasNotSuccessful() = coroutinesTest {
        // given
        val parameters = buildGetConversationsParameters()
        val errorMessage = "Test - Bad Request"

        coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(emptyList())
        coEvery { conversationDao.insertOrUpdate(*anyVararg()) } returns Unit
        coEvery { api.fetchConversations(any()) } throws IOException(errorMessage)

        // when
        conversationsRepository.observeConversations(parameters).test {

            // then
            val actualLocalItems = awaitItem() as DataResult.Success
            assertEquals(ResponseSource.Local, actualLocalItems.source)

            val actualError = awaitItem() as DataResult.Error
            assertEquals(ResponseSource.Remote, actualError.source)
            assertEquals("Test - Bad Request", actualError.message)
        }
    }

    @Test
    fun verifyGetConversationsReturnsLocalDataWhenFetchingFromApiFails() = coroutinesTest {
        // given
        val labelId = MessageLocationType.INBOX.asLabelId()
        val parameters = buildGetConversationsParameters(labelId = labelId)
        val errorMessage = "Api call failed"

        val labelContextDatabaseModel = LabelContextDatabaseModel(
            id = labelId.id,
            contextNumUnread = 0,
            contextNumMessages = 0,
            contextTime = 0,
            contextSize = 0,
            contextNumAttachments = 0
        )
        val conversationDatabaseModels = listOf(
            buildConversationDatabaseModel(labels = listOf(labelContextDatabaseModel))
        )
        coEvery { conversationDao.observeConversations(parameters.userId.id) } returns
            flowOf(conversationDatabaseModels)
        coEvery { api.fetchConversations(any()) } throws IOException(errorMessage)

        val expectedLocalConversations =
            databaseModelToConversationMapper.toDomainModels(conversationDatabaseModels)

        // when
        conversationsRepository.observeConversations(parameters).test {

            // then
            assertEquals(expectedLocalConversations.local(), awaitItem())

            val actualError = awaitItem() as DataResult.Error
            assertEquals(ResponseSource.Remote, actualError.source)
            assertEquals("Api call failed", actualError.message)
        }
    }

    @Test
    fun verifyLocalConversationWithMessagesIsReturnedWhenDataIsAvailableInTheLocalDB() {
        coroutinesTest {
            // given
            val conversationDbModel = buildConversationDatabaseModel()
            val message = Message(
                messageId = "messageId9238482",
                conversationId = conversationId,
                subject = "subject1231",
                Unread = false,
                sender = MessageSender("senderName", "sender@protonmail.ch"),
                toList = listOf(),
                time = 82_374_723L,
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
            coEvery { conversationDao.observeConversation(testUserId.id, conversationId) } returns flowOf(
                conversationDbModel
            )

            val expectedMessage = MessageDomainModel(
                "messageId9238482",
                conversationId,
                "subject1231",
                false,
                Correspondent("senderName", "sender@protonmail.ch"),
                listOf(),
                82_374_723L,
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

            // when
            conversationsRepository.getConversation(testUserId, conversationId).test {

                // then
                assertEquals(expectedConversation.local(), awaitItem())
                awaitItem()
            }
        }
    }

    @Test
    fun verifyConversationIsEmittedWithUpdatedDataWhenOneOfItsMessagesChangesInTheLocalDB() {
        coroutinesTest {
            // given
            val conversationDbModel = buildConversationDatabaseModel()
            val message = Message(
                messageId = "messageId9238483",
                conversationId = conversationId,
                subject = "subject1231",
                Unread = false,
                sender = MessageSender("senderName", "sender@protonmail.ch"),
                toList = listOf(),
                time = 82_374_723L,
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
            coEvery { conversationDao.observeConversation(testUserId.id, conversationId) } returns flowOf(
                conversationDbModel
            )

            // when
            val result = conversationsRepository.getConversation(testUserId, conversationId).take(2).toList()

            // then
            val expectedMessage = MessageDomainModel(
                "messageId9238483",
                conversationId,
                "subject1231",
                false,
                Correspondent("senderName", "sender@protonmail.ch"),
                listOf(),
                82_374_723L,
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
            assertEquals(DataResult.Success(ResponseSource.Local, expectedConversation), result[1])
        }
    }

    @Test
    fun verifyConversationIsNotEmittedAgainIfItsValueDidntChange() {
        coroutinesTest {
            // given
            val conversationDbModel = buildConversationDatabaseModel()
            val message = Message(
                messageId = "messageId9238484",
                conversationId = conversationId,
                subject = "subject1232",
                Unread = false,
                sender = MessageSender("senderName", "sender@protonmail.ch"),
                toList = listOf(),
                time = 82_374_723L,
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
            coEvery { conversationDao.observeConversation(testUserId.id, conversationId) } returns flowOf(
                conversationDbModel
            )

            val expectedMessage = MessageDomainModel(
                "messageId9238484",
                conversationId,
                "subject1232",
                false,
                Correspondent("senderName", "sender@protonmail.ch"),
                listOf(),
                82_374_723L,
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

            // when
            conversationsRepository.getConversation(testUserId, conversationId).test {

                // then
                assertEquals(DataResult.Success(ResponseSource.Local, expectedConversation), awaitItem())
                assertEquals(DataResult.Processing(ResponseSource.Remote), awaitItem())
            }
        }
    }

    @Test
    fun verifyConversationIsFetchedFromRemoteDataSourceAndStoredLocallyWhenNotAvailableInDb() {
        coroutinesTest {
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
                contextTime = 357
            )
            val apiMessage = ServerMessage(id = "messageId23842737", conversationId)
            val conversationResponse = ConversationResponse(
                0,
                conversationApiModel,
                listOf(apiMessage)
            )
            val expectedMessage = Message(messageId = "messageId23842737", conversationId)
            val dbFlow =
                MutableSharedFlow<ConversationDatabaseModel?>(replay = 2, onBufferOverflow = BufferOverflow.SUSPEND)
            val params = GetOneConversationParameters(testUserId, conversationId)
            coEvery { api.fetchConversation(params) } returns conversationResponse
            coEvery { messageDao.observeAllMessagesInfoFromConversation(conversationId) } returns flowOf(emptyList())
            coEvery { conversationDao.observeConversation(testUserId.id, conversationId) } returns dbFlow
            every { messageFactory.createMessage(apiMessage) } returns expectedMessage
            val expectedConversationDbModel = buildConversationDatabaseModel()
            coEvery { conversationDao.insertOrUpdate(expectedConversationDbModel) } coAnswers {
                dbFlow.emit(
                    expectedConversationDbModel
                )
            }
            withTimeout(3.toDuration(DurationUnit.SECONDS)) {
                flowTest(conversationsRepository.getConversation(testUserId, conversationId)) {
                    // then
                    assertEquals(DataResult.Processing(ResponseSource.Remote), awaitItem())
                    assertEquals(ResponseSource.Local, (awaitItem() as DataResult.Success).source)
                    coVerify { messageDao.saveMessages(listOf(expectedMessage)) }
                    coVerify { conversationDao.insertOrUpdate(expectedConversationDbModel) }
                }
            }

            // when
            dbFlow.emit(null)
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreMarkedRead() {
        coroutinesTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val message = Message()
            coEvery { conversationDao.updateNumUnreadMessages(any(), 0) } just runs
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns listOf(message, message)
            coEvery { messageDao.saveMessage(any()) } returns 123
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.markRead(conversationIds, testUserId)

            // then
            coVerify {
                conversationDao.updateNumUnreadMessages(conversationId, 0)
                conversationDao.updateNumUnreadMessages(conversationId1, 0)
            }
            coVerify(exactly = 4) {
                messageDao.saveMessage(message)
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyGetConversationsReThrowsCancellationExceptionWithoutEmittingError() {
        coroutinesTest {
            // given
            val parameters = buildGetConversationsParameters(end = null)
            coEvery { conversationDao.observeConversations(testUserId.id) } returns flowOf(emptyList())
            coEvery { api.fetchConversations(parameters) } throws CancellationException("Cancelled")

            // when
            val job = flowTest(conversationsRepository.observeConversations(parameters)) {
                // then
                val actual = awaitItem() as DataResult.Success
                assertEquals(ResponseSource.Local, actual.source)

                awaitItem()
            }
            job.join()
            assertTrue(job.isCancelled, "Job was not cancelled.")
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreMarkedUnread() {
        coroutinesTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val mailboxLocation = MessageLocationType.ARCHIVE
            val locationId = MessageLocationType.ARCHIVE.asLabelIdString()
            val message = Message(
                location = mailboxLocation.messageLocationTypeValue
            )
            val message2 = Message(
                location = mailboxLocation.messageLocationTypeValue
            )
            val conversationMessagesList = listOf(message, message2)
            val unreadMessages = 0
            coEvery { conversationDao.findConversation(any(), any()) } returns
                mockk {
                    every { numUnread } returns unreadMessages
                }
            coEvery { conversationDao.updateNumUnreadMessages(any(), unreadMessages + 1) } just runs
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns conversationMessagesList
            coEvery { messageDao.saveMessage(any()) } returns 123
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.markUnread(conversationIds, UserId("id"), locationId)

            // then
            coVerify {
                conversationDao.updateNumUnreadMessages(conversationId, unreadMessages + 1)
                conversationDao.updateNumUnreadMessages(conversationId1, unreadMessages + 1)
            }
            coVerify(exactly = 2) {
                markUnreadLatestNonDraftMessageInLocation(
                    conversationMessagesList,
                    locationId,
                    testUserId
                )
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenMarkUnreadIsCalled() {
        coroutinesTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val locationId = MessageLocationType.ARCHIVE.asLabelIdString()
            coEvery { conversationDao.findConversation(any(), any()) } returns null
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.markUnread(conversationIds, UserId("id"), locationId)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyConversationsAreUpdatedWhenMessagesAreMarkedAsRead() = coroutinesTest {
        // given
        val action = ChangeMessagesReadStatus.Action.ACTION_MARK_READ
        val message1 = Message(
            conversationId = conversationId1
        )
        val conversation1 = buildConversationDatabaseModel(
            userId = testUserId.id,
            id = conversationId1,
            numMessages = 2,
            numUnread = 1
        )
        val updatedConversation1 = buildConversationDatabaseModel(
            userId = testUserId.id,
            id = conversationId1,
            numMessages = 2,
            numUnread = 0
        )
        coEvery { messageDao.findMessageByIdOnce(messageId1) } returns message1
        coEvery { conversationDao.findConversation(testUserId.id, conversationId1) } returns conversation1
        coEvery { conversationDao.update(updatedConversation1) } returns 123

        // when
        conversationsRepository.updateConvosBasedOnMessagesReadStatus(
            testUserId,
            listOf(messageId1),
            action
        )

        // then
        coVerify {
            conversationDao.update(updatedConversation1)
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreStarred() {
        coroutinesTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val conversationLabels = listOf(
                LabelContextDatabaseModel("10", 2, 4, 123, 123, 1),
                LabelContextDatabaseModel("2", 0, 3, 123, 123, 0)
            )
            val testMessageId = "messageId"
            val message = Message(
                messageId = testMessageId,
                allLabelIDs = emptyList(),
                isStarred = false
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
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns listOf(message, message)
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.star(conversationIds, testUserId)

            // then
            val expected = Message(
                messageId = testMessageId,
                isStarred = true,
                allLabelIDs = listOf(STARRED_LABEL_ID), // Needed to ensure that the starred label is added locally
                location = 10 // Changed to starred (10) when adding starred label
            )
            coVerify(exactly = 2) {
                conversationDao.updateLabels(any(), any())
            }
            // Expected exactly 2 times as in this test we star two conversations
            coVerify(exactly = 2) {
                messageDao.saveMessages(listOf(expected, expected))
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenStarIsCalled() {
        coroutinesTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val testMessageId = "messageId"
            val message = Message(
                messageId = testMessageId,
                time = 123
            )
            coEvery { conversationDao.findConversation(any(), any()) } returns null
            coEvery { messageDao.findAttachmentsByMessageId(testMessageId) } returns flowOf(emptyList())
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns listOf(message, message)
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.star(conversationIds, testUserId)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreUnstarred() {
        coroutinesTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val conversationLabels = listOf(
                LabelContextDatabaseModel("10", 2, 4, 123, 123, 1),
                LabelContextDatabaseModel("2", 0, 3, 123, 123, 0)
            )
            val testMessageId = "messageId"
            val message = Message(
                messageId = testMessageId,
                isStarred = true,
                allLabelIDs = listOf(STARRED_LABEL_ID),
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
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns listOf(message, message)
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.unstar(conversationIds, testUserId)

            // then
            val expected = Message(
                messageId = testMessageId,
                isStarred = false,
                allLabelIDs = emptyList(), // Needed to ensure that the starred label is removed locally
                location = 0 // Changed when removing labels, defaults to INBOX (0) if no labels
            )
            coVerify(exactly = 2) {
                conversationDao.updateLabels(any(), any())
            }
            // Expected exactly 2 times as in this test we unstar two conversations
            coVerify(exactly = 2) {
                messageDao.saveMessages(listOf(expected, expected))
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenUnstarIsCalled() {
        coroutinesTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns mockk(relaxed = true)
            coEvery { conversationDao.findConversation(any(), any()) } returns null
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.unstar(conversationIds, testUserId)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyConversationsAreUpdatedWhenMessagesAreStarred() = coroutinesTest {
        // given
        val action = ChangeMessagesStarredStatus.Action.ACTION_STAR
        val inboxLabel = buildLabelContextDatabaseModel(
            id = inboxLabelId,
            contextNumUnread = 0,
            contextNumMessages = 2,
            contextNumAttachments = 3
        )
        val starredLabel = buildLabelContextDatabaseModel(
            id = starredLabelId,
            contextNumUnread = 1,
            contextNumMessages = 1,
            contextTime = 123,
            contextSize = 123,
            contextNumAttachments = 2
        )
        val conversation1 = buildConversationDatabaseModel(
            userId = testUserId.id,
            id = conversationId1,
            numMessages = 2,
            numUnread = 0,
            numAttachments = 3,
            labels = listOf(inboxLabel)
        )
        val updatedConversation1 = buildConversationDatabaseModel(
            userId = testUserId.id,
            id = conversationId1,
            numMessages = 2,
            numUnread = 0,
            numAttachments = 3,
            labels = listOf(inboxLabel, starredLabel)
        )
        val message1 = Message(
            conversationId = conversationId1,
            Unread = true,
            time = 123,
            totalSize = 123,
            numAttachments = 2
        )
        coEvery { messageDao.findMessageByIdOnce(messageId1) } returns message1
        coEvery { conversationDao.findConversation(testUserId.id, conversationId1) } returns conversation1
        coEvery { conversationDao.update(updatedConversation1) } returns 123

        // when
        conversationsRepository.updateConvosBasedOnMessagesStarredStatus(
            testUserId,
            listOf(messageId1),
            action
        )

        // then
        coVerify {
            conversationDao.update(updatedConversation1)
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreMovedToFolder() {
        coroutinesTest {
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
                every { type } returns LabelType.MESSAGE_LABEL
            }
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns listOf(message, message)
            coEvery { labelsRepository.findLabel(any()) } returns label
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
            val result = conversationsRepository.moveToFolder(conversationIds, testUserId, folderId)

            // then
            coVerify(exactly = 2) {
                messageDao.saveMessages(any())
            }
            coVerify(exactly = 2) {
                conversationDao.updateLabels(conversationId1, any())
            }
            coVerify(exactly = 2) {
                conversationDao.updateLabels(conversationId1, any())
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenMoveToFolderIsCalled() {
        coroutinesTest {
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
                every { type } returns LabelType.MESSAGE_LABEL
            }
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns listOf(message, message)
            coEvery { labelsRepository.findLabel(any()) } returns label
            coEvery { conversationDao.findConversation(any(), any()) } returns null
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.moveToFolder(conversationIds, testUserId, folderId)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun `verify conversations are updated when messages location changes`() =
        coroutinesTest {
            // given
            val message = Message(
                messageId = messageId1,
                conversationId = conversationId1,
                Unread = false,
                time = 0,
                totalSize = 0,
                numAttachments = 0
            )
            val conversation = buildConversationDatabaseModel(
                id = conversationId1,
                labels = listOf(
                    buildLabelContextDatabaseModel(
                        id = inboxLabelId,
                        contextNumMessages = 1
                    )
                )
            )
            val updatedConversation = buildConversationDatabaseModel(
                id = conversationId1,
                labels = listOf(
                    buildLabelContextDatabaseModel(
                        id = folderId,
                        contextNumMessages = 1
                    )
                )
            )
            coEvery { messageDao.findMessageByIdOnce(messageId1) } returns message
            coEvery { conversationDao.findConversation(testUserId.id, conversationId1) } returns conversation
            coEvery { conversationDao.update(updatedConversation) } returns 123

        // when
        conversationsRepository.updateConvosBasedOnMessagesLocation(
            testUserId,
            listOf(messageId1),
            inboxLabelId,
            folderId
        )

        // then
        coVerify {
            conversationDao.update(updatedConversation)
        }
    }

    @Test
    fun verifyConversationsAndMessagesAreDeleted() {
        coroutinesTest {
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
            coEvery { conversationDao.deleteConversation(testUserId.id, any()) } just runs
            coEvery { conversationDao.findConversation(any(), any()) } returns
                mockk {
                    every { labels } returns conversationLabels
                }
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns listOfMessages
            coEvery { messageDao.deleteMessagesByIds(any()) } just runs

            // when
            conversationsRepository.delete(conversationIds, testUserId, currentFolderId)

            // then
            coVerify { conversationDao.updateLabels(conversationId1, any()) }
            coVerify { conversationDao.updateLabels(conversationId1, any()) }
            coVerify(exactly = 2) { messageDao.deleteMessagesByIds(any()) }
        }
    }

    @Test
    fun verifyConversationsAreUpdatedWhenMessagesAreDeleted() = coroutinesTest {
        // given
        val messageIds = listOf(messageId1, messageId2)
        val inboxLabel = buildLabelContextDatabaseModel(
            id = inboxLabelId,
            contextNumUnread = 0,
            contextNumMessages = 1,
            contextNumAttachments = 0
        )
        val trashLabel = buildLabelContextDatabaseModel(
            id = trashLabelId,
            contextNumUnread = 1,
            contextNumMessages = 1,
            contextNumAttachments = 1
        )
        val message1 = Message(
            conversationId = conversationId1
        )
        val message2 = Message(
            conversationId = conversationId2,
            Unread = true,
            numAttachments = 1,
            allLabelIDs = listOf(trashLabelId)
        )
        val conversation1 = buildConversationDatabaseModel(
            id = conversationId1,
            numMessages = 1
        )
        val conversation2 = buildConversationDatabaseModel(
            id = conversationId2,
            numMessages = 2,
            numUnread = 1,
            numAttachments = 1,
            labels = listOf(inboxLabel, trashLabel)
        )
        val updatedConversation2 = buildConversationDatabaseModel(
            id = conversationId2,
            numMessages = 1,
            numUnread = 0,
            numAttachments = 0,
            labels = listOf(inboxLabel)
        )
        coEvery { messageDao.findMessageByIdOnce(messageId1) } returns message1
        coEvery { messageDao.findMessageByIdOnce(messageId2) } returns message2
        coEvery { conversationDao.findConversation(testUserId.id, conversationId1) } returns conversation1
        coEvery { conversationDao.findConversation(testUserId.id, conversationId2) } returns conversation2
        coEvery {
            conversationDao.deleteConversation(testUserId.id, any())
        } just runs
        coEvery {
            conversationDao.update(updatedConversation2)
        } returns 123

        // when
        conversationsRepository.updateConversationsWhenDeletingMessages(testUserId, messageIds)

        // then
        coVerify(exactly = 1) {
            conversationDao.deleteConversation(testUserId.id, conversationId1)
        }
        coVerify(exactly = 1) {
            conversationDao.update(updatedConversation2)
        }
    }

    @Test
    fun verifyMessagesAndConversationsAreLabeled() {
        coroutinesTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val labelId = "labelId"
            val message = Message(
                allLabelIDs = emptyList()
            )
            val listOfMessages = listOf(message, message)
            val conversationLabels = listOf(
                LabelContextDatabaseModel("5", 0, 2, 123, 123, 1),
                LabelContextDatabaseModel("0", 0, 2, 123, 123, 0)
            )
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns listOfMessages
            coEvery { conversationDao.findConversation(testUserId.id, any()) } returns
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
            val result = conversationsRepository.label(conversationIds, testUserId, labelId)

            // then
            val expected = Message(
                allLabelIDs = listOf(labelId),
                // Needed because the addition of labels will change location to "LABEL" since 'labelId' it's the only label
                location = MessageLocationType.LABEL.messageLocationTypeValue
            )
            coVerify(exactly = 2) { messageDao.saveMessages(listOf(expected, expected)) }
            coVerify { conversationDao.updateLabels(conversationId1, any()) }
            coVerify { conversationDao.updateLabels(conversationId1, any()) }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenLabelIsCalled() {
        coroutinesTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val labelId = "labelId"
            val message = mockk<Message> {
                every { time } returns 123
                every { addLabels(any()) } just runs
            }
            val listOfMessages = listOf(message, message)
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns listOfMessages
            coEvery { conversationDao.findConversation(testUserId.id, any()) } returns null
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.label(conversationIds, testUserId, labelId)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyMessagesAndConversationsAreUnlabeled() {
        coroutinesTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val labelId = "labelId"
            val message = Message(
                allLabelIDs = listOf(labelId)
            )
            val listOfMessages = listOf(message, message)
            val conversationLabels = listOf(
                LabelContextDatabaseModel("5", 0, 2, 123, 123, 1),
                LabelContextDatabaseModel("0", 0, 2, 123, 123, 0),
                LabelContextDatabaseModel("labelId", 0, 2, 123, 123, 0)
            )
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns listOfMessages
            coEvery { conversationDao.findConversation(testUserId.id, any()) } returns
                mockk {
                    every { labels } returns conversationLabels
                }

            coEvery { conversationDao.updateLabels(any(), any()) } just runs
            val expectedResult = ConversationsActionResult.Success

            // when
            val result = conversationsRepository.unlabel(conversationIds, testUserId, labelId)

            // then
            val expected = Message(
                allLabelIDs = emptyList(),
                // Needed because the removal of labels will change location, defalting to INBOX if no labels
                location = MessageLocationType.INBOX.messageLocationTypeValue
            )
            coVerify(exactly = 2) {
                messageDao.saveMessages(listOf(expected, expected))
            }
            coVerify {
                conversationDao.updateLabels(conversationId, any())
            }
            coVerify {
                conversationDao.updateLabels(conversationId1, any())
            }
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyErrorResultIsReturnedIfConversationIsNullWhenUnlabelIsCalled() {
        coroutinesTest {
            // given
            val conversationIds = listOf(conversationId, conversationId1)
            val labelId = "labelId"
            val message = mockk<Message> {
                every { removeLabels(any()) } just runs
            }
            val listOfMessages = listOf(message, message)
            coEvery { messageDao.findAllConversationMessagesSortedByNewest(any()) } returns listOfMessages
            coEvery { conversationDao.findConversation(testUserId.id, any()) } returns null
            val expectedResult = ConversationsActionResult.Error

            // when
            val result = conversationsRepository.unlabel(conversationIds, testUserId, labelId)

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun `verify conversation is updated when labels are added and removed from a message`() {
        coroutinesTest {
            // given
            val message = Message(
                conversationId = conversationId1,
                Unread = false,
                time = 10,
                totalSize = 10,
                numAttachments = 0
            )
            val conversation = buildConversationDatabaseModel(
                id = conversationId1,
                labels = listOf(
                    buildLabelContextDatabaseModel(id = inboxLabelId),
                    buildLabelContextDatabaseModel(id = labelId2)
                )
            )
            val updatedConversation = buildConversationDatabaseModel(
                id = conversationId1,
                labels = listOf(
                    buildLabelContextDatabaseModel(id = inboxLabelId),
                    buildLabelContextDatabaseModel(
                        id = labelId1,
                        contextNumMessages = 1,
                        contextTime = 10,
                        contextSize = 10
                    )
                )
            )
            coEvery { messageDao.findMessageByIdOnce(messageId1) } returns message
            coEvery { conversationDao.findConversation(testUserId.id, conversationId1) } returns conversation
            coEvery { conversationDao.update(updatedConversation) } returns 123

            // when
            conversationsRepository.updateConversationBasedOnMessageLabels(
                testUserId,
                messageId1,
                listOf(labelId1),
                listOf(labelId2)
            )

            // then
            coVerify {
                conversationDao.update(updatedConversation)
            }
        }
    }

    @Test
    fun correctlyFiltersConversationsByRequestedLocationFromDatabase() = coroutinesTest {
        // given
        val archivedConversation = buildConversationDatabaseModel(
            id = "conversation 1",
            labels = listOf(inboxLabelContextDatabaseModel(), archiveLabelContextDatabaseModel())
        )
        val inboxConversation = buildConversationDatabaseModel(
            id = "conversation 2",
            labels = listOf(inboxLabelContextDatabaseModel())
        )
        val conversations = listOf(inboxConversation, archivedConversation)
        coEvery { conversationDao.observeConversations(any()) } returns flowOf(conversations)

        val expected = databaseModelToConversationMapper
            .toDomainModels(listOf(archivedConversation))
            .local()

        // when
        val params = GetAllConversationsParameters(testUserId, labelId = MessageLocationType.ARCHIVE.asLabelId())
        conversationsRepository.observeConversations(params).test {

            // then
            assertEquals(expected, awaitItem())
            awaitItem() // Ignored Remote data
        }
    }

    @Test
    fun correctlyFiltersConversationsByRequestedLabelFromDatabase() = coroutinesTest {
        // given
        val archivedConversation = buildConversationDatabaseModel(
            id = "conversation 1",
            labels = listOf(inboxLabelContextDatabaseModel(), archiveLabelContextDatabaseModel())
        )
        val customLabelId = LabelId("custom label id")
        val customLabelConversation = buildConversationDatabaseModel(
            id = "conversation 2",
            labels = listOf(customContextDatabaseModel(customLabelId.id))
        )
        val conversations = listOf(customLabelConversation, archivedConversation)
        coEvery { conversationDao.observeConversations(any()) } returns flowOf(conversations)

        val expected = databaseModelToConversationMapper
            .toDomainModels(listOf(customLabelConversation))
            .local()

        // when
        val params = GetAllConversationsParameters(testUserId, labelId = customLabelId)
        conversationsRepository.observeConversations(params).test {

            // then
            assertEquals(expected, awaitItem())
            awaitItem() // Ignored Remote data
        }
    }

    @Test
    fun unreadCountersAreCorrectlyFetchedFromDatabase() = coroutinesTest {
        // given
        val labelId = "inbox"
        val unreadCount = 15
        val databaseModel = UnreadCounterEntity(
            userId = testUserId,
            type = UnreadCounterEntity.Type.CONVERSATIONS,
            labelId = labelId,
            unreadCount = unreadCount
        )
        every { unreadCounterDao.observeConversationsUnreadCounters(testUserId) } returns flowOf(listOf(databaseModel))
        val expected = DataResult.Success(ResponseSource.Local, listOf(UnreadCounter(labelId, unreadCount)))

        // when
        conversationsRepository.getUnreadCounters(testUserId).test {

            // then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun unreadCountersAreCorrectlyFetchedFromApi() = coroutinesTest {
        // given
        val labelId = "inbox"
        val unreadCount = 15
        val apiModel = CountsApiModel(
            labelId = labelId,
            total = 0,
            unread = unreadCount
        )

        setupUnreadCounterDaoToSimulateReplace()

        val apiResponse = CountsResponse(listOf(apiModel))
        coEvery { api.fetchConversationsCounts(testUserId) } returns apiResponse

        val expectedList = DataResult.Success(ResponseSource.Local, listOf(UnreadCounter(labelId, unreadCount)))

        // when
        conversationsRepository.getUnreadCounters(testUserId).test {

            // then
            assertEquals(expectedList, awaitItem())
        }
    }

    @Test
    fun unreadCountersAreRefreshedFromApi() = coroutinesTest {
        // given
        val labelId = "inbox"
        val firstUnreadCount = 15
        val secondUnreadCount = 20
        val thirdUnreadCount = 25

        val firstApiModel = CountsApiModel(
            labelId = labelId,
            total = 0,
            unread = firstUnreadCount
        )
        val secondApiModel = firstApiModel.copy(
            unread = secondUnreadCount
        )
        val thirdApiModel = secondApiModel.copy(
            unread = thirdUnreadCount
        )

        val firstApiResponse = CountsResponse(listOf(firstApiModel))
        val secondApiResponse = CountsResponse(listOf(secondApiModel))
        val thirdApiResponse = CountsResponse(listOf(thirdApiModel))
        val allApiResponses = listOf(firstApiResponse, secondApiResponse, thirdApiResponse)

        setupUnreadCounterDaoToSimulateReplace()

        var apiCounter = 0
        coEvery { api.fetchConversationsCounts(testUserId) } answers {
            allApiResponses[apiCounter++]
        }

        val firstExpected = listOf(UnreadCounter(labelId, firstUnreadCount)).local()
        val secondExpected = listOf(UnreadCounter(labelId, secondUnreadCount)).local()
        val thirdExpected = listOf(UnreadCounter(labelId, thirdUnreadCount)).local()

        // when
        conversationsRepository.getUnreadCounters(testUserId).test {

            // then
            assertEquals(firstExpected, awaitItem())

            conversationsRepository.refreshUnreadCounters()
            assertEquals(secondExpected, awaitItem())

            conversationsRepository.refreshUnreadCounters()
            assertEquals(thirdExpected, awaitItem())
        }
    }

    @Test
    fun handlesExceptionDuringUnreadCountersRefreshAndContinuesObserving() = coroutinesTest {
        // given
        val expectedMessage = "Invalid username!"
        coEvery { api.fetchConversationsCounts(testUserId) } answers {
            throw IllegalArgumentException(expectedMessage)
        }

        // when
        conversationsRepository.getUnreadCounters(testUserId).test {

            // then
            val actual = awaitItem() as DataResult.Error.Remote
            assertEquals(expectedMessage, actual.message)
        }
    }

    @Test
    fun getCountersIsCancelledWhenApiCallIsCancelled() = coroutinesTest {
        // given
        coEvery { api.fetchConversationsCounts(testUserId) } throws(CancellationException("Cancelled"))

        // when
        conversationsRepository.getUnreadCounters(testUserId).test(timeout = 1.seconds) {
            expectNoEvents()
        }
    }

    private fun setupUnreadCounterDaoToSimulateReplace() {

        val counters = MutableStateFlow(emptyList<UnreadCounterEntity>())

        every { unreadCounterDao.observeConversationsUnreadCounters(testUserId) } returns counters
        coEvery { unreadCounterDao.insertOrUpdate(any<Collection<UnreadCounterEntity>>()) } answers {
            counters.value = firstArg<Collection<UnreadCounterEntity>>().toList()
        }
    }

    private fun buildConversation(
        id: String,
        subject: String = "A subject",
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

    private fun buildConversationApiModel(
        id: String,
        order: Long = 0,
        subject: String = "A subject",
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
        contextTime = 357
    )

    private fun buildConversationDatabaseModel(
        id: String = conversationId,
        order: Long = 0,
        userId: String = testUserId.id,
        subject: String = "Subject",
        senders: List<MessageSender> = emptyList(),
        recipients: List<MessageRecipient> = emptyList(),
        numMessages: Int = 0,
        numUnread: Int = 0,
        numAttachments: Int = 0,
        expirationTime: Long = 0,
        size: Long = 0,
        labels: List<LabelContextDatabaseModel> = emptyList()
    ) = ConversationDatabaseModel(
        id = id,
        order = order,
        userId = userId,
        subject = subject,
        senders = senders,
        recipients = recipients,
        numMessages = numMessages,
        numUnread = numUnread,
        numAttachments = numAttachments,
        expirationTime = expirationTime,
        size = size,
        labels = labels
    )

    private fun buildConversationDatabaseModel(
        userId: UserId = testUserId,
        id: String,
        order: Long = 0,
        subject: String = "A subject",
        labels: List<LabelContextDatabaseModel>
    ) = ConversationDatabaseModel(
        userId = userId.id,
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
    )

    private fun buildGetConversationsParameters(
        labelId: LabelId = MessageLocationType.INBOX.asLabelId(),
        end: Long? = 1_616_496_670,
        pageSize: Int? = 50
    ) = GetAllConversationsParameters(
        labelId = labelId,
        userId = testUserId,
        end = end,
        pageSize = pageSize!!
    )

    private fun buildConversationDatabaseModel(
        labels: List<LabelContextDatabaseModel> = emptyList()
    ): ConversationDatabaseModel =
        ConversationDatabaseModel(
            id = conversationId,
            order = 0L,
            userId = testUserId.id,
            subject = "subject",
            senders = listOf(
                MessageSender("sender-name", "email@proton.com")
            ),
            recipients = listOf(
                MessageRecipient("receiver-name", "email-receiver@proton.com")
            ),
            numMessages = 1,
            numUnread = 0,
            numAttachments = 0,
            expirationTime = 0,
            size = 1,
            labels = labels
        )

    private fun buildLabelContextDatabaseModel(
        id: String,
        contextNumUnread: Int = 0,
        contextNumMessages: Int = 0,
        contextTime: Long = 0,
        contextSize: Int = 0,
        contextNumAttachments: Int = 0
    ) = LabelContextDatabaseModel(
        id = id,
        contextNumUnread = contextNumUnread,
        contextNumMessages = contextNumMessages,
        contextTime = contextTime,
        contextSize = contextSize,
        contextNumAttachments = contextNumAttachments
    )

    private fun customContextDatabaseModel(labelId: String) = LabelContextDatabaseModel(
        id = labelId,
        contextNumUnread = 0,
        contextNumMessages = 0,
        contextTime = 0L,
        contextSize = 0,
        contextNumAttachments = 0
    )

    private fun inboxLabelContextDatabaseModel(): LabelContextDatabaseModel =
        customContextDatabaseModel(MessageLocationType.INBOX.asLabelIdString())

    private fun archiveLabelContextDatabaseModel(): LabelContextDatabaseModel =
        customContextDatabaseModel(MessageLocationType.ARCHIVE.asLabelIdString())

    private fun <T> T.local() = DataResult.Success(ResponseSource.Local, this)
    private fun <T> T.remote() = DataResult.Success(ResponseSource.Remote, this)
}
