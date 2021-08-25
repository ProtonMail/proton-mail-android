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

package ch.protonmail.android.repository

import app.cash.turbine.test
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Message
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.domain.entity.user.User
import ch.protonmail.android.utils.MessageBodyFileManager
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test
import java.io.IOException
import kotlin.random.Random.Default.nextBytes
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the functionality of [MessageRepository].
 */
class MessageRepositoryTest {

    private val messageDao: MessageDao = mockk()
    private val databaseProvider: DatabaseProvider = mockk {
        every { provideMessageDao(any()) } returns messageDao
    }

    private val messageBodyFileManager: MessageBodyFileManager = mockk()

    private val protonMailApiManager: ProtonMailApiManager = mockk()

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

    private val testUserName = "userName1"
    private val testUserId = UserId(testUserName)
    private val message1 = mockk<Message>(relaxed = true) { every { messageId } returns "1" }
    private val message2 = mockk<Message>(relaxed = true) { every { messageId } returns "2" }
    private val message3 = mockk<Message>(relaxed = true) { every { messageId } returns "3" }
    private val message4 = mockk<Message>(relaxed = true) { every { messageId } returns "4" }

    private val messageRepository = MessageRepository(
        TestDispatcherProvider,
        databaseProvider,
        protonMailApiManager,
        messageBodyFileManager,
        userManager,
        jobManager,
        networkConnectivityManager
    )

    @Test
    fun verifyMessageIsFetchedAndSavedIfMessageDoesNotExistInDbWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOn() {
        runBlockingTest {
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
        runBlockingTest {
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
        runBlockingTest {
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
        runBlockingTest {
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
        runBlockingTest {
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
        runBlockingTest {
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
        runBlockingTest {
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
        runBlockingTest {
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
        runBlockingTest {
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
        runBlockingTest {
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
        runBlockingTest {
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
    fun verifyThatInboxMessagesFromNetAndDbAreFetched() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.INBOX
        val initialDatabaseMessages = listOf(message1, message2)
        val apiMessages = listOf(message1, message2, message3, message4)

        // region API
        val apiResponse = mockk<MessagesResponse> {
            every { messages } returns apiMessages
            every { code } returns Constants.RESPONSE_CODE_OK
        }
        coEvery {
            protonMailApiManager.getMessages(
                UserIdTag(testUserId),
                mailboxLocation.messageLocationTypeValue,
                null,
                null
            )
        } returns apiResponse
        // endregion

        // region Dao
        val databaseMessages = MutableStateFlow(initialDatabaseMessages)
        every { messageDao.observeMessagesByLocation(mailboxLocation.messageLocationTypeValue) } returns
            databaseMessages
        coEvery { messageDao.saveMessages(apiMessages) } answers {
            val oldMessages = databaseMessages.value
            val newMessages = (firstArg<List<Message>>() + oldMessages).distinctBy { it.messageId }
            // We just compare sized of the lists, as the messages are mock
            if (newMessages.size > oldMessages.size) {
                databaseMessages.tryEmit(newMessages)
            }
        }
        // endregion

        // when
        messageRepository.observeMessagesByLocation(testUserId, mailboxLocation).test {

            // then
            // verify messages from database
            assertEquals(initialDatabaseMessages, expectItem())

            // verify api fetch
            coVerify {
                protonMailApiManager.getMessages(
                    UserIdTag(testUserId), mailboxLocation.messageLocationTypeValue, null, null
                )
            }
            coVerify { messageDao.saveMessages(apiMessages) }

            // verify message from api
            assertEquals(apiMessages, expectItem())
        }
    }

    @Test
    fun verifyThatInboxMessagesGetFromNetFailsAndOnlyDbDataIsReturned() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.INBOX
        val dbMessages = listOf(message1, message2)
        val netMessages = listOf(message1, message2, message3, message4)
        val dbFlow = MutableSharedFlow<List<Message>>(replay = 2, onBufferOverflow = BufferOverflow.SUSPEND)
        coEvery {
            messageDao.observeMessagesByLocation(
                mailboxLocation.messageLocationTypeValue
            )
        } returns dbFlow
        val testException = IOException("NetworkError!")
        coEvery {
            protonMailApiManager.getMessages(
                UserIdTag(testUserId),
                mailboxLocation.messageLocationTypeValue,
                null,
                null
            )
        } throws testException
        coEvery { messageDao.saveMessages(netMessages) } answers { dbFlow.tryEmit(netMessages) }

        // when
        messageRepository.observeMessagesByLocation(testUserId, mailboxLocation).test {
            dbFlow.emit(dbMessages)

            // then
            assertEquals(dbMessages, expectItem())
            expectNoEvents()
        }
    }

    @Test
    fun verifyThatLabeledMessagesFromDbAndNetAreFetched() = runBlockingTest {
        // given
        val label1 = "label1"
        val dbMessages = listOf(message1, message2)
        val netMessages = listOf(message1, message2, message3, message4)
        val netResponse = mockk<MessagesResponse> {
            every { messages } returns netMessages
            every { code } returns Constants.RESPONSE_CODE_OK
        }
        val dbFlow = MutableSharedFlow<List<Message>>(replay = 2, onBufferOverflow = BufferOverflow.SUSPEND)
        coEvery { messageDao.observeMessagesByLabelId(label1) } returns dbFlow
        coEvery {
            protonMailApiManager.searchByLabelAndPage(
                label1,
                0
            )
        } returns netResponse
        coEvery { messageDao.saveMessages(netMessages) } answers { dbFlow.tryEmit(netMessages) }

        // when
        messageRepository.observeMessagesByLabelId(label1, testUserId).test {
            dbFlow.emit(dbMessages)

            // then
            coVerify { messageDao.saveMessages(netMessages) }
            assertEquals(netMessages, expectItem())
            assertEquals(dbMessages, expectItem())
        }
    }

    @Test
    fun verifyThatLabeledMessagesGetFromNetFailsAndOnlyDbDataIsReturned() = runBlockingTest {
        // given
        val label1 = "label1"
        val dbMessages = listOf(message1, message2)
        val netMessages = listOf(message1, message2, message3, message4)
        val dbFlow = MutableSharedFlow<List<Message>>(replay = 2, onBufferOverflow = BufferOverflow.SUSPEND)
        coEvery { messageDao.observeMessagesByLabelId(label1) } returns dbFlow
        val testException = IOException("NetworkError!")
        coEvery {
            protonMailApiManager.searchByLabelAndPage(
                label1,
                0
            )
        } throws testException
        coEvery { messageDao.saveMessages(netMessages) } answers { dbFlow.tryEmit(netMessages) }

        // when
        messageRepository.observeMessagesByLabelId(label1, testUserId).test {
            dbFlow.emit(dbMessages)

            // then
            assertEquals(dbMessages, expectItem())
            expectError()
        }
    }

    @Test
    fun verifyThatAllMessagesFromDbAndNetworkAreFetched() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.ALL_MAIL
        val dbMessages = listOf(message1, message2)
        val netMessages = listOf(message1, message2, message3, message4)
        val netResponse = mockk<MessagesResponse> {
            every { messages } returns netMessages
            every { code } returns Constants.RESPONSE_CODE_OK
        }
        coEvery { messageDao.observeAllMessages() } returns flowOf(dbMessages)
        coEvery {
            protonMailApiManager.getMessages(
                UserIdTag(testUserId),
                mailboxLocation.messageLocationTypeValue,
                null,
                null
            )
        } returns netResponse
        coEvery { messageDao.saveMessages(netMessages) } just Runs

        // when
        val resultsList = messageRepository.observeMessagesByLocation(testUserId, mailboxLocation).take(2).toList()

        // then
        coVerify(exactly = 1) { messageDao.saveMessages(netMessages) }
        assertEquals(dbMessages, resultsList[0])
    }

    @Test
    fun verifyThatAllMessagesFromDbAndNetworkAreNotFetchedDueToLackOfConnectivity() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.ALL_MAIL
        val dbMessages = listOf(message1, message2)
        val netMessages = listOf(message1, message2, message3, message4)
        val netResponse = mockk<MessagesResponse> {
            every { messages } returns netMessages
            every { code } returns Constants.RESPONSE_CODE_OK
        }
        coEvery { messageDao.observeAllMessages() } returns flowOf(dbMessages)
        coEvery {
            protonMailApiManager.getMessages(
                UserIdTag(testUserId),
                mailboxLocation.messageLocationTypeValue,
                null,
                null
            )
        } returns netResponse
        coEvery { messageDao.saveMessages(netMessages) } just Runs
        coEvery { networkConnectivityManager.isInternetConnectionPossible() } returns false

        // when
        val resultsList = messageRepository.observeMessagesByLocation(testUserId, mailboxLocation).take(1).toList()

        // then
        coVerify(exactly = 0) { messageDao.saveMessages(netMessages) }
        assertEquals(dbMessages, resultsList[0])
    }

    @Test
    fun verifyThatAllStaredFromDbAndNetAreFetched() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.STARRED
        val initialDatabaseMessages = listOf(message1, message2)
        val apiMessages = listOf(message1, message2, message3, message4)

        // region API
        val apiResponse = mockk<MessagesResponse> {
            every { messages } returns apiMessages
            every { code } returns Constants.RESPONSE_CODE_OK
        }
        coEvery {
            protonMailApiManager.getMessages(
                UserIdTag(testUserId),
                mailboxLocation.messageLocationTypeValue,
                null,
                null
            )
        } returns apiResponse
        // endregion

        // region Dao
        val databaseMessages = MutableStateFlow(initialDatabaseMessages)
        every { messageDao.observeStarredMessages() } returns databaseMessages
        coEvery { messageDao.saveMessages(apiMessages) } answers {
            val oldMessages = databaseMessages.value
            val newMessages = (firstArg<List<Message>>() + oldMessages).distinctBy { it.messageId }
            // We just compare sized of the lists, as the messages are mock
            if (newMessages.size > oldMessages.size) {
                databaseMessages.tryEmit(newMessages)
            }
        }
        // endregion

        // when
        messageRepository.observeMessagesByLocation(testUserId, mailboxLocation).test {

            // then
            // verify messages from database
            assertEquals(initialDatabaseMessages, expectItem())

            // verify api fetch
            coVerify {
                protonMailApiManager.getMessages(
                    UserIdTag(testUserId), mailboxLocation.messageLocationTypeValue, null, null
                )
            }
            coVerify { messageDao.saveMessages(apiMessages) }

            // verify message from api
            assertEquals(apiMessages, expectItem())
        }
    }
}
