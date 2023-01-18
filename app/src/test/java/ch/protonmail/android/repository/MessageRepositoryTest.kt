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

package ch.protonmail.android.repository

import app.cash.turbine.test
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.api.models.messages.receive.ServerMessage
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.CounterDao
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.MessagePreferenceDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessagePreferenceEntity
import ch.protonmail.android.domain.entity.user.User
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.mailbox.data.local.UnreadCounterDao
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterEntity
import ch.protonmail.android.mailbox.data.mapper.ApiToDatabaseUnreadCounterMapper
import ch.protonmail.android.mailbox.data.mapper.DatabaseToDomainUnreadCounterMapper
import ch.protonmail.android.mailbox.data.mapper.MessagesResponseToMessagesMapper
import ch.protonmail.android.mailbox.data.remote.model.CountsApiModel
import ch.protonmail.android.mailbox.data.remote.model.CountsResponse
import ch.protonmail.android.mailbox.data.remote.worker.MoveMessageToLocationWorker
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import ch.protonmail.android.mailbox.domain.model.UnreadCounter
import ch.protonmail.android.utils.MessageBodyFileManager
import ch.protonmail.android.worker.EmptyFolderRemoteWorker
import com.birbit.android.jobqueue.JobManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.UnconfinedCoroutinesTest
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.random.Random.Default.nextBytes
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class MessageRepositoryTest: CoroutinesTest by UnconfinedCoroutinesTest() {

    private val messageDao: MessageDao = mockk(relaxUnitFun = true) {
        coEvery { saveMessage(any()) } returns 123
    }

    private val unreadCounterDao: UnreadCounterDao = mockk {
        every { observeMessagesUnreadCounters(any()) } returns flowOf(emptyList())
        coEvery { insertOrUpdate(any<Collection<UnreadCounterEntity>>()) } just Runs
    }

    private val contactDao: ContactDao = mockk {
        every { findAllContactsEmails() } returns flowOf(emptyList())
    }

    private val counterDao: CounterDao = mockk {
        coEvery { insertUnreadLocation(any()) } just runs
    }

    private val messagePreferenceDao: MessagePreferenceDao = mockk {
        coEvery { saveMessagePreference(any()) } returns 123
    }

    private val databaseProvider: DatabaseProvider = mockk {
        every { provideContactDao(any()) } returns contactDao
        every { provideCounterDao(any()) } returns counterDao
        every { provideMessageDao(any()) } returns messageDao
        every { provideMessagePreferenceDao(any()) } returns messagePreferenceDao
        every { provideUnreadCounterDao(any()) } returns unreadCounterDao
    }

    private val messageBodyFileManager: MessageBodyFileManager = mockk()

    private val protonMailApiManager: ProtonMailApiManager = mockk {
        coEvery { fetchMessagesCounts(any()) } returns CountsResponse(emptyList())
    }

    private val newUser = mockk<User> {
        every { id } returns testUserId
    }
    private val userManager: UserManager = mockk {
        every { currentUser } returns newUser
    }
    private val jobManager: JobManager = mockk()

    private val networkConnectivityManager: NetworkConnectivityManager = mockk {
        every { isInternetConnectionPossible() } returns true
    }

    private val labelRepository = mockk<LabelRepository>()

    private val postToLocationWorker = mockk<MoveMessageToLocationWorker.Enqueuer>() {
        every { enqueue(any(), any(), any(), any()) } returns mockk()
    }

    private val emptyFolderRemoteWorker = mockk<EmptyFolderRemoteWorker.Enqueuer> {
        coEvery { enqueue(any(), any()) } returns mockk()
    }

    private val testUserName = "userName1"
    private val testUserId = UserId(testUserName)
    private val messageId = "messageId"
    private val message1 = Message(messageId = "1")
    private val message2 = Message(messageId = "2")
    private val message3 = Message(messageId = "3")
    private val message4 = Message(messageId = "4")
    private val allMessages = listOf(message1, message2, message3, message4)
    private val serverMessage1 = ServerMessage(id = "1", ConversationID = EMPTY_STRING)
    private val serverMessage2 = ServerMessage(id = "2", ConversationID = EMPTY_STRING)
    private val serverMessage3 = ServerMessage(id = "3", ConversationID = EMPTY_STRING)
    private val serverMessage4 = ServerMessage(id = "4", ConversationID = EMPTY_STRING)
    private val allServerMessages = listOf(serverMessage1, serverMessage2, serverMessage3, serverMessage4)
    private val inboxLabelId = LabelId("0")
    private val trashLabelId = LabelId("3")
    private val allMailLabelId = LabelId("5")
    private val scheduledMailLabelId = LabelId("12")
    private val allSendLabelId = LabelId("2")
    private val customLabelId = LabelId("customLabelId")
    private val customFolderId = LabelId("customFolderId")

    private lateinit var messageRepository: MessageRepository

    @Before
    fun setUp() {
        messageRepository = MessageRepository(
            dispatcherProvider = dispatchers,
            databaseProvider = databaseProvider,
            protonMailApiManager = protonMailApiManager,
            databaseToDomainUnreadCounterMapper = DatabaseToDomainUnreadCounterMapper(),
            apiToDatabaseUnreadCounterMapper = ApiToDatabaseUnreadCounterMapper(),
            messagesResponseToMessagesMapper = MessagesResponseToMessagesMapper(),
            messageBodyFileManager = messageBodyFileManager,
            userManager = userManager,
            jobManager = jobManager,
            connectivityManager = networkConnectivityManager,
            labelRepository = labelRepository,
            moveMessageToLocationWorker = postToLocationWorker,
            emptyFolderRemoteWorker = emptyFolderRemoteWorker
        )
    }

    @Test
    fun verifyMessageIsFetchedAndSavedIfMessageDoesNotExistInDbWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOn() {
        coroutinesTest {
            // given
            val messageId = "messageId"
            val messageInDb = Message(messageId)
            messageInDb.messageBody = "messageBody"
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns true
            }
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns null
            coEvery { messageDao.saveMessage(messageInDb) } returns 123
            coEvery { protonMailApiManager.fetchMessageDetails(messageId, any()) } returns mockk {
                every { message } returns messageInDb
            }

            // when
            val result = messageRepository.getMessage(testUserId, messageId, false)

            // then
            assertEquals(messageInDb, result)
            coVerify { messageDao.saveMessage(messageInDb) }
        }
    }

    @Test
    fun verifyMessageIsFetchedAndSavedIfMessageExistsInDbButMessageBodyIsNullWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOn() {
        coroutinesTest {
            // given
            val messageId = "messageId"
            val messageInDb = Message(messageId)
            messageInDb.messageBody = null
            val messageFetched = Message(messageId)
            messageFetched.messageBody = "messageBody"
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns true
            }
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns messageInDb
            coEvery { messageDao.saveMessage(messageFetched) } returns 123
            coEvery { protonMailApiManager.fetchMessageDetails(messageId, any()) } returns mockk {
                every { message } returns messageFetched
            }

            // when
            val result = messageRepository.getMessage(testUserId, messageId, false)

            // then
            assertEquals(messageFetched, result)
            coVerify { messageDao.saveMessage(messageFetched) }
        }
    }

    @Test
    fun verifyMessageIsFetchedAndSavedIfMessageDoesNotExistInDbWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOff() {
        coroutinesTest {
            // given
            val messageId = "messageId"
            val message = Message(messageId)
            message.messageBody = "messageBody"
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns false
            }
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns null
            coEvery { messageDao.saveMessage(message) } returns 123
            coEvery { protonMailApiManager.fetchMessageMetadata(messageId, any()) } returns mockk {
                every { messages } returns listOf(message)
            }

            // when
            val result = messageRepository.getMessage(testUserId, messageId, false)

            // then
            assertEquals(message, result)
            coVerify { messageDao.saveMessage(message) }
        }
    }

    @Test
    fun verifyMessageFromDbIsReturnedIfMessageExistsInDbWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOn() {
        coroutinesTest {
            // given
            val messageId = "messageId"
            val mockMessage = mockk<Message> {
                every { this@mockk getProperty "messageBody" } returns "messageBody"
                every { this@mockk setProperty "messageBody" value any<String>() } just runs
            }
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns true
            }
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns mockMessage

            // when
            val result = messageRepository.findMessage(testUserId, messageId)

            // then
            assertEquals(mockMessage, result)
        }
    }

    @Test
    fun verifyMessageFromDbIsReturnedIfMessageExistsInDbWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOff() {
        coroutinesTest {
            // given
            val messageId = "messageId"
            val mockMessage = mockk<Message> {
                every { this@mockk getProperty "messageBody" } returns "messageBody"
                every { this@mockk setProperty "messageBody" value any<String>() } just runs
            }
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns false
            }
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns mockMessage

            // when
            val result = messageRepository.findMessage(testUserId, messageId)

            // then
            assertEquals(mockMessage, result)
        }
    }

    @Test
    fun verifyMessageBodyInMessageReturnedIfMessageExistsInDbAndMessageBodyIsReadFromFileWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOn() {
        coroutinesTest {
            // given
            val messageId = "messageId"
            val mockMessage = spyk(Message(messageBody = "file://messageBody"))
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns true
            }
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns mockMessage
            every { messageBodyFileManager.readMessageBodyFromFile(mockMessage) } returns "messageBody"

            // when
            val result = messageRepository.findMessage(testUserId, messageId)

            // then
            assertEquals("messageBody", result?.messageBody)
        }
    }

    @Test
    fun verifyMessageIsSavedIfMessageDoesNotExistInDbAndMessageBodyIsLargeWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOn() {
        coroutinesTest {
            // given
            val messageId = "messageId"
            val mockMessage = spyk(Message(messageBody = nextBytes(MAX_BODY_SIZE_IN_DB + 1024).contentToString()))
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns true
            }
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns null
            coEvery { messageDao.saveMessage(any()) } returns 123
            coEvery { protonMailApiManager.fetchMessageDetails(messageId, any()) } returns mockk {
                every { message } returns mockMessage
            }
            coEvery { messageBodyFileManager.saveMessageBodyToFile(mockMessage) } returns "file://messageBody"

            // when
            val result = messageRepository.getMessage(testUserId, messageId, false)

            // then
            val savedMessageCaptor = slot<Message>()
            assertEquals("file://messageBody", result!!.messageBody)
            coVerify { messageBodyFileManager.saveMessageBodyToFile(mockMessage) }
            coVerify { messageDao.saveMessage(capture(savedMessageCaptor)) }
            assertEquals("file://messageBody", savedMessageCaptor.captured.messageBody)
        }
    }

    @Test
    fun verifyNullIsReturnedIfMessageDoesNotExistInDbAndTheApiCallThrowsAnExceptionWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOn() {
        coroutinesTest {
            // given
            val messageId = "messageId"
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns true
            }
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns null
            coEvery { protonMailApiManager.fetchMessageDetails(messageId, UserIdTag(testUserId)) } throws Exception()

            // when
            val result = messageRepository.findMessage(testUserId, messageId)

            // then
            assertNull(result)
        }
    }

    @Test
    fun verifyNullIsReturnedIfMessageDoesNotExistInDbAndTheApiCallThrowsAnExceptionWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOff() {
        coroutinesTest {
            // given
            val messageId = "messageId"
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns false
            }
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns null
            coEvery { protonMailApiManager.fetchMessageMetadata(messageId, UserIdTag(testUserId)) } throws Exception()

            // when
            val result = messageRepository.findMessage(testUserId, messageId)

            // then
            assertNull(result)
        }
    }

    @Test
    fun verifyMessageDetailsAreFetchedIfShouldFetchMessageDetailsIsTrueWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOff() {
        coroutinesTest {
            // given
            val messageId = "messageId"
            val message = Message(messageId)
            message.messageBody = "messageBody"
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns false
            }
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns null
            coEvery { messageDao.saveMessage(message) } returns 123
            coEvery { protonMailApiManager.fetchMessageDetails(messageId, any()) } returns mockk {
                every { this@mockk.message } returns message
            }

            // when
            val result = messageRepository.getMessage(testUserId, messageId, shouldFetchMessageDetails = true)

            // then
            assertEquals(message, result)
        }
    }

    @Test
    fun verifyGetMessageSavesAndReturnMessageWithMessageBodyWhenMessageBodyIsNotTooBigToBeSavedInTheDatabase() {
        coroutinesTest {
            // given
            val messageId = "messageId1"
            val apiMessage = Message(messageId = messageId)
            apiMessage.messageBody = "Any message body returned by the API"
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns false
            }
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns null
            coEvery { messageDao.saveMessage(any()) } returns 1234
            coEvery { protonMailApiManager.fetchMessageDetails(messageId, any()) } returns mockk {
                every { message } returns apiMessage
            }
            // when
            val result = messageRepository.getMessage(testUserId, messageId, shouldFetchMessageDetails = true)

            // then
            coVerify { messageDao.saveMessage(apiMessage) }
            assertEquals("Any message body returned by the API", result!!.messageBody)
        }
    }

    @Test
    fun verifyThatInboxMessagesFromNetAndDbAreFetched() = coroutinesTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.INBOX
        val initialDatabaseMessages = allMessages.take(2)
        val apiMessages = allMessages.drop(2)

        val apiResponse = buildMockMessageResponse(
            messages = apiMessages
        )
        val params = GetAllMessagesParameters(testUserId, labelId = mailboxLocation.asLabelId())
        coEvery { protonMailApiManager.getMessages(params) } returns apiResponse

        val databaseMessages = MutableStateFlow(initialDatabaseMessages)
        every { messageDao.observeMessages(mailboxLocation.asLabelIdString()) } returns
            databaseMessages
        coEvery { messageDao.saveMessages(apiMessages) } answers {
            databaseMessages.tryEmit(databaseMessages.value + firstArg<List<Message>>())
        }

        // when
        messageRepository.observeMessages(params).test {

            // then
            // verify messages from database
            assertEquals(initialDatabaseMessages.local(), awaitItem())

            // verify api fetch
            coVerify {
                protonMailApiManager.getMessages(params)
            }
            coVerify { messageDao.saveMessages(apiMessages) }

            // verify message from api
            assertEquals(apiMessages.remote(), awaitItem())
            assertEquals(allMessages.local(), awaitItem())
        }
    }

    @Test
    fun verifyThatInboxMessagesGetFromNetFailsAndOnlyDbDataIsReturned() = coroutinesTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.INBOX
        val dbMessages = allMessages.take(2)
        val netMessages = allMessages
        val dbFlow = MutableSharedFlow<List<Message>>(replay = 2, onBufferOverflow = BufferOverflow.SUSPEND)
        coEvery { messageDao.observeMessages(mailboxLocation.asLabelIdString()) } returns dbFlow
        val exceptionMessage = "NetworkError!"
        val testException = IOException(exceptionMessage)
        val params = GetAllMessagesParameters(
            testUserId,
            labelId = mailboxLocation.asLabelId()
        )
        coEvery { protonMailApiManager.getMessages(params) } throws testException
        coEvery { messageDao.saveMessages(netMessages) } answers { dbFlow.tryEmit(netMessages) }

        // when
        messageRepository.observeMessages(params).test {
            dbFlow.emit(dbMessages)

            // then
            assertEquals(dbMessages.local(), awaitItem())
            assertEquals(DataResult.Error.Remote(exceptionMessage, testException), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun verifyThatLabeledMessagesFromDbAndNetAreFetched() = coroutinesTest {
        // given
        val dbMessages = listOf(message1, message2)
        val netMessages = listOf(message1, message2, message3, message4)
        val netResponse = mockk<MessagesResponse> {
            every { serverMessages } returns allServerMessages
            every { messages } returns netMessages
            every { code } returns Constants.RESPONSE_CODE_OK
        }
        val dbFlow = MutableSharedFlow<List<Message>>(replay = 2, onBufferOverflow = BufferOverflow.SUSPEND)
        coEvery { messageDao.observeMessages(customLabelId.id) } returns dbFlow
        val params = GetAllMessagesParameters(testUserId, labelId = customLabelId)
        coEvery { protonMailApiManager.getMessages(params) } returns netResponse
        coEvery { messageDao.saveMessages(netMessages) } answers { dbFlow.tryEmit(netMessages) }

        // when
        messageRepository.observeMessages(params).test {
            dbFlow.emit(dbMessages)

            // then
            assertEquals(dbMessages.local(), awaitItem())
            coVerify { messageDao.saveMessages(netMessages) }
            assertEquals(netMessages.remote(), awaitItem())
            assertEquals(netMessages.local(), awaitItem())
        }
    }

    @Test
    fun verifyThatLabeledMessagesGetFromNetFailsAndOnlyDbDataIsReturned() = coroutinesTest {
        // given
        val dbMessages = listOf(message1, message2)
        val netMessages = listOf(message1, message2, message3, message4)
        val dbFlow = MutableSharedFlow<List<Message>>(replay = 2, onBufferOverflow = BufferOverflow.SUSPEND)
        coEvery { messageDao.observeMessages(customLabelId.id) } returns dbFlow
        val testExceptionMessage = "NetworkError!"
        val testException = IOException(testExceptionMessage)
        val params = GetAllMessagesParameters(testUserId, labelId = customLabelId)
        coEvery { protonMailApiManager.getMessages(params) } throws testException
        coEvery { messageDao.saveMessages(netMessages) } answers { dbFlow.tryEmit(netMessages) }

        // when
        messageRepository.observeMessages(params).test {
            dbFlow.emit(dbMessages)

            // then
            assertEquals(DataResult.Success(ResponseSource.Local, dbMessages), awaitItem())
            assertEquals(DataResult.Error.Remote(testExceptionMessage, testException), awaitItem())
        }
    }

    @Test
    fun verifyThatAllMessagesFromDbAndNetworkAreFetched() = coroutinesTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.ALL_MAIL
        val databaseMessages = allMessages.take(2)
        val databaseMessagesFlow = MutableStateFlow(databaseMessages)
        val netMessages = allMessages
        val netResponse = mockk<MessagesResponse> {
            every { serverMessages } returns allServerMessages
            every { messages } returns netMessages
            every { code } returns Constants.RESPONSE_CODE_OK
        }
        coEvery { messageDao.observeMessages(mailboxLocation.asLabelIdString()) } returns databaseMessagesFlow
        val params = GetAllMessagesParameters(
            testUserId,
            labelId = mailboxLocation.asLabelId()
        )
        coEvery { protonMailApiManager.getMessages(params) } returns netResponse
        coEvery { messageDao.saveMessages(netMessages) } answers {
            val messages = firstArg<List<Message>>()
            databaseMessagesFlow.value = messages
        }

        // when
        messageRepository.observeMessages(params).test {

            // then
            assertEquals(databaseMessages.local(), awaitItem())
            assertEquals(netMessages.remote(), awaitItem())
            assertEquals(netMessages.local(), awaitItem())

            coVerify(exactly = 1) { messageDao.saveMessages(netMessages) }
        }
    }

    @Test
    fun verifyThatAllMessagesFromDbAndNetworkAreNotFetchedDueToLackOfConnectivity() = coroutinesTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.ALL_MAIL
        val dbMessages = listOf(message1, message2)
        val netMessages = listOf(message1, message2, message3, message4)
        val netResponse = mockk<MessagesResponse> {
            every { messages } returns netMessages
            every { code } returns Constants.RESPONSE_CODE_OK
        }
        coEvery { messageDao.observeMessages(mailboxLocation.asLabelIdString()) } returns flowOf(dbMessages)
        val params = GetAllMessagesParameters(testUserId, labelId = mailboxLocation.asLabelId())
        coEvery { protonMailApiManager.getMessages(params) } returns netResponse
        coEvery { messageDao.saveMessages(netMessages) } just Runs
        coEvery { networkConnectivityManager.isInternetConnectionPossible() } returns false

        // when
        val resultsList = messageRepository.observeMessages(params).take(1).toList()

        // then
        coVerify(exactly = 0) { messageDao.saveMessages(netMessages) }
        assertEquals(DataResult.Success(ResponseSource.Local, dbMessages), resultsList[0])
    }

    @Test
    fun verifyThatAllStaredFromDbAndNetAreFetched() = coroutinesTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.STARRED
        val initialDatabaseMessages = allMessages.take(2)
        val apiMessages = allMessages.drop(2)

        val apiResponse = buildMockMessageResponse(
            messages = apiMessages
        )
        val params = GetAllMessagesParameters(
            testUserId,
            labelId = mailboxLocation.asLabelId()
        )
        coEvery { protonMailApiManager.getMessages(params) } returns apiResponse

        val databaseMessages = MutableStateFlow(initialDatabaseMessages)
        every { messageDao.observeMessages(mailboxLocation.asLabelIdString()) } returns databaseMessages
        coEvery { messageDao.saveMessages(apiMessages) } answers {
            databaseMessages.tryEmit(databaseMessages.value + firstArg<List<Message>>())
        }

        // when
        messageRepository.observeMessages(params).test {

            // then
            // verify messages from database
            assertEquals(initialDatabaseMessages.local(), awaitItem())

            // verify api fetch
            coVerify { protonMailApiManager.getMessages(params) }
            coVerify { messageDao.saveMessages(apiMessages) }

            // verify message from api
            assertEquals(apiMessages.remote(), awaitItem())
            assertEquals(allMessages.local(), awaitItem())
        }
    }

    @Test
    fun unreadCountersAreCorrectlyFetchedFromDatabase() = coroutinesTest {
        // given
        val labelId = "inbox"
        val unreadCount = 15
        val databaseModel = UnreadCounterEntity(
            userId = testUserId,
            type = UnreadCounterEntity.Type.MESSAGES,
            labelId = labelId,
            unreadCount = unreadCount
        )
        every { unreadCounterDao.observeMessagesUnreadCounters(testUserId) } returns flowOf(listOf(databaseModel))
        val expected = DataResult.Success(ResponseSource.Local, listOf(UnreadCounter(labelId, unreadCount)))

        // when
        messageRepository.getUnreadCounters(testUserId).test {

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
        coEvery { protonMailApiManager.fetchMessagesCounts(testUserId) } returns apiResponse

        val expectedList = DataResult.Success(ResponseSource.Local, listOf(UnreadCounter(labelId, unreadCount)))

        // when
        messageRepository.getUnreadCounters(testUserId).test {

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

        setupUnreadCounterDaoToSimulateReplace()

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

        var apiCounter = 0
        coEvery { protonMailApiManager.fetchMessagesCounts(testUserId) } answers {
            allApiResponses[apiCounter++]
        }

        val firstExpected = DataResult.Success(ResponseSource.Local, listOf(UnreadCounter(labelId, firstUnreadCount)))
        val secondExpected = DataResult.Success(ResponseSource.Local, listOf(UnreadCounter(labelId, secondUnreadCount)))
        val thirdExpected = DataResult.Success(ResponseSource.Local, listOf(UnreadCounter(labelId, thirdUnreadCount)))

        // when
        messageRepository.getUnreadCounters(testUserId).test {

            // then
            assertEquals(firstExpected, awaitItem())

            messageRepository.refreshUnreadCounters()
            assertEquals(secondExpected, awaitItem())

            messageRepository.refreshUnreadCounters()
            assertEquals(thirdExpected, awaitItem())
        }
    }

    @Test
    fun handlesExceptionDuringUnreadCountersRefreshAndContinuesObserving() = coroutinesTest {
        // given
        val expectedMessage = "Invalid username!"
        coEvery { protonMailApiManager.fetchMessagesCounts(testUserId) } answers {
            throw IllegalArgumentException(expectedMessage)
        }

        // when
        messageRepository.getUnreadCounters(testUserId).test {

            // then
            val actual = awaitItem() as DataResult.Error.Remote
            assertEquals(expectedMessage, actual.message)
        }
    }

    @Test
    fun getCountersIsCancelledWhenApiCallIsCancelled() = coroutinesTest {
        // given
        coEvery { protonMailApiManager.fetchMessagesCounts(testUserId) } throws(CancellationException("Cancelled"))

        // when
        messageRepository.getUnreadCounters(testUserId).test(timeout = 1.seconds) {
            expectNoEvents()
        }
    }

    @Test
    fun `verify worker is enqueued and messages are deleted locally when emptying folder`() =
        coroutinesTest {
            // given
            val labelId = LabelId("labelId")

            // when
            messageRepository.emptyFolder(testUserId, labelId)

            // then
            coVerify {
                emptyFolderRemoteWorker.enqueue(testUserId, labelId)
                messageDao.deleteMessagesByLabel(labelId.id)
            }
        }

    @Test
    fun `verify correct labels are added and removed when message is moved to custom location`() =
        coroutinesTest {
            // given
            val messageIds = listOf(messageId)
            val labelIds = listOf(inboxLabelId, allMailLabelId, customLabelId)
            val expectedLabelIds = listOf(allMailLabelId, customFolderId, customLabelId).ids()
            val message = Message(
                messageId = messageId,
                allLabelIDs = labelIds.ids()
            )
            val customLabel = buildLabel(id = customLabelId)
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns message
            coEvery { counterDao.findUnreadLocationById(any()) } returns mockk(relaxed = true)
            coEvery { labelRepository.findLabel(customLabelId) } returns customLabel

            // when
            messageRepository.moveToCustomFolderLocation(messageIds, customFolderId.id, testUserId)

            // then
            val savedMessageCaptor = slot<Message>()
            coVerify { messageDao.saveMessage(capture(savedMessageCaptor)) }
            assertEquals(expectedLabelIds, savedMessageCaptor.captured.allLabelIDs)
        }

    @Test
    fun `verify correct labels are added and removed when message is moved to trash`() = coroutinesTest {
        // given
        val messageIds = listOf(messageId)
        val labelIds = listOf(inboxLabelId, allMailLabelId, customLabelId)
        val expectedLabelIds = listOf(trashLabelId, allMailLabelId).ids()
        val message = Message(
            messageId = messageId,
            allLabelIDs = labelIds.ids()
        )
        val customLabel = buildLabel(id = customLabelId)
        coEvery { messageDao.findMessageByIdOnce(messageId) } returns message
        coEvery { counterDao.findUnreadLocationById(any()) } returns mockk(relaxed = true)
        coEvery { labelRepository.findLabel(customLabelId) } returns customLabel

        // when
        messageRepository.moveToTrash(messageIds, testUserId)

        // then
        val savedMessageCaptor = slot<Message>()
        coVerify { messageDao.saveMessage(capture(savedMessageCaptor)) }
        assertEquals(expectedLabelIds, savedMessageCaptor.captured.allLabelIDs)
    }

    @Test
    fun `verify correct labels are added and removed when scheduled message is moved to trash`() =
        coroutinesTest {
            // given
            val messageIds = listOf(messageId)
            val labelIds = listOf(scheduledMailLabelId, allSendLabelId, allMailLabelId)
            val expectedLabelIds = listOf(trashLabelId, allMailLabelId).ids()
            val message = Message(
                messageId = messageId,
                allLabelIDs = labelIds.ids()
            )
            val customLabel = buildLabel(id = customLabelId)
            coEvery { messageDao.findMessageByIdOnce(messageId) } returns message
            coEvery { counterDao.findUnreadLocationById(any()) } returns mockk(relaxed = true)
            coEvery { labelRepository.findLabel(customLabelId) } returns customLabel

            // when
            messageRepository.moveToTrash(messageIds, testUserId)

            // then
            val savedMessageCaptor = slot<Message>()
            coVerify { messageDao.saveMessage(capture(savedMessageCaptor)) }
            assertEquals(expectedLabelIds, savedMessageCaptor.captured.allLabelIDs)
        }

    @Test
    fun `should save message preference if it does not exist in DB when saving view in dark mode preference`() =
        coroutinesTest {
            // given
            val expectedResult = buildMessagePreference(viewInDarkMode = true)
            coEvery { messagePreferenceDao.findMessagePreference(messageId) } returns null

            // when
            messageRepository.saveViewInDarkModeMessagePreference(testUserId, messageId, viewInDarkMode = true)

            // then
            val result = slot<MessagePreferenceEntity>()
            coVerify { messagePreferenceDao.saveMessagePreference(capture(result)) }
            assertEquals(expectedResult, result.captured)
    }

    @Test
    fun `should update message preference if it already exists in DB when saving view in dark mode preference`() =
        coroutinesTest {
            // given
            val messagePreference = buildMessagePreference(viewInDarkMode = true)
            val expectedResult = buildMessagePreference(viewInDarkMode = false)
            coEvery { messagePreferenceDao.findMessagePreference(messageId) } returns messagePreference

            // when
            messageRepository.saveViewInDarkModeMessagePreference(testUserId, messageId, viewInDarkMode = false)

            // then
            val result = slot<MessagePreferenceEntity>()
            coVerify { messagePreferenceDao.saveMessagePreference(capture(result)) }
            assertEquals(expectedResult, result.captured)
        }

    @Test
    fun `should return a count of messages withing the given location`() =
        coroutinesTest {
            // given
            val mailboxLocation = Constants.MessageLocationType.ALL_SCHEDULED.asLabelIdString()
            coEvery { messageDao.observeMessagesCountByLocation(mailboxLocation) } returns flowOf(1)

            // when
            messageRepository.observeMessagesCountByLocationFromDatabase(
                testUserId, Constants.MessageLocationType.ALL_SCHEDULED.asLabelIdString()
            ).test {

                // then
                coVerify { messageDao.observeMessagesCountByLocation(mailboxLocation) }
                assertEquals(1, awaitItem())
                awaitComplete()
            }
        }

    private fun setupUnreadCounterDaoToSimulateReplace() {

        val counters = MutableStateFlow(emptyList<UnreadCounterEntity>())

        every { unreadCounterDao.observeMessagesUnreadCounters(testUserId) } returns counters
        coEvery { unreadCounterDao.insertOrUpdate(any<Collection<UnreadCounterEntity>>()) } answers {
            counters.value = firstArg<Collection<UnreadCounterEntity>>().toList()
        }
    }

    private fun <T> List<T>.local() = DataResult.Success(ResponseSource.Local, this)
    private fun <T> List<T>.remote() = DataResult.Success(ResponseSource.Remote, this)
    private fun List<LabelId>.ids() = map { it.id }

    private fun buildMockMessageResponse(
        messages: List<Message> = allMessages,
        serverMessages: List<ServerMessage> = allServerMessages,
        code: Int = Constants.RESPONSE_CODE_OK
    ): MessagesResponse = mockk {
        every { this@mockk.messages } returns messages
        every { this@mockk.serverMessages } returns serverMessages
        every { this@mockk.code } returns code
    }

    private fun buildLabel(
        id: LabelId,
        name: String = "",
        color: String = "",
        order: Int = 0,
        type: LabelType = LabelType.MESSAGE_LABEL,
        path: String = "",
        parentId: String = ""
    ) = Label(
        id = id,
        name = name,
        color = color,
        order = order,
        type = type,
        path = path,
        parentId = parentId
    )

    private fun buildMessagePreference(
        id: Long = 0,
        messageId: String = this.messageId,
        viewInDarkMode: Boolean = true
    ) = MessagePreferenceEntity(
        id = id,
        messageId = messageId,
        viewInDarkMode = viewInDarkMode
    )
}
