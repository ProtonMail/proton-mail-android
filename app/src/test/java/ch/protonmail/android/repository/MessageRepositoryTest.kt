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

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.utils.MessageBodyFileManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Before
import org.junit.Test
import kotlin.random.Random.Default.nextBytes
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the functionality of [MessageRepository].
 */

class MessageRepositoryTest {

    private val testUserId = Id("id")

    @MockK
    private lateinit var messageDao: MessageDao
    @MockK
    private lateinit var messageBodyFileManager: MessageBodyFileManager
    @MockK
    private lateinit var protonMailApiManager: ProtonMailApiManager
    @MockK
    private lateinit var userManager: UserManager

    private lateinit var messageRepository: MessageRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        messageRepository =
            MessageRepository(
                TestDispatcherProvider,
                messageDao,
                protonMailApiManager,
                messageBodyFileManager,
                userManager
            )
    }

    @Test
    fun verifyMessageIsFetchedAndSavedIfMessageDoesNotExistInDbWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOn() {
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
            coEvery { messageDao.findMessageById(messageId) } returns flowOf(null)
            coEvery { messageDao.saveMessage(mockMessage) } returns 123
            coEvery { protonMailApiManager.fetchMessageDetails(messageId, any()) } returns mockk {
                every { message } returns mockMessage
            }

            // when
            val result = messageRepository.getMessage(messageId, testUserId)

            // then
            assertEquals(mockMessage, result)
            coVerify { messageDao.saveMessage(mockMessage) }
        }
    }

    @Test
    fun verifyMessageIsFetchedAndSavedIfMessageExistsInDbButMessageBodyIsNullWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOn() {
        runBlockingTest {
            // given
            val messageId = "messageId"
            val mockMessageInDb = mockk<Message> {
                every { this@mockk getProperty "messageBody" } returns null
                every { this@mockk setProperty "messageBody" value any<String>() } just runs
            }
            val mockMessageFetched = mockk<Message> {
                every { this@mockk getProperty "messageBody" } returns "messageBody"
                every { this@mockk setProperty "messageBody" value any<String>() } just runs
            }
            coEvery { userManager.getLegacyUser(testUserId) } returns mockk {
                every { isGcmDownloadMessageDetails } returns true
            }
            coEvery { messageDao.findMessageById(messageId) } returns flowOf(mockMessageInDb)
            coEvery { messageDao.saveMessage(mockMessageFetched) } returns 123
            coEvery { protonMailApiManager.fetchMessageDetails(messageId, any()) } returns mockk {
                every { message } returns mockMessageFetched
            }

            // when
            val result = messageRepository.getMessage(messageId, testUserId)

            // then
            assertEquals(mockMessageFetched, result)
            coVerify { messageDao.saveMessage(mockMessageFetched) }
        }
    }

    @Test
    fun verifyMessageIsFetchedAndSavedIfMessageDoesNotExistInDbWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOff() {
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
            coEvery { messageDao.findMessageById(messageId) } returns flowOf(null)
            coEvery { messageDao.saveMessage(mockMessage) } returns 123
            coEvery { protonMailApiManager.fetchMessageMetadata(messageId, any()) } returns mockk {
                every { messages } returns listOf(mockMessage)
            }

            // when
            val result = messageRepository.getMessage(messageId, testUserId)

            // then
            assertEquals(mockMessage, result)
            coVerify { messageDao.saveMessage(mockMessage) }
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
            coEvery { messageDao.findMessageById(messageId) } returns flowOf(mockMessage)

            // when
            val result = messageRepository.getMessage(messageId, testUserId)

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
            coEvery { messageDao.findMessageById(messageId) } returns flowOf(mockMessage)

            // when
            val result = messageRepository.getMessage(messageId, testUserId)

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
            coEvery { messageDao.findMessageById(messageId) } returns flowOf(mockMessage)
            coEvery { messageBodyFileManager.readMessageBodyFromFile(mockMessage) } returns "messageBody"

            // when
            val result = messageRepository.getMessage(messageId, testUserId)

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
            coEvery { messageDao.findMessageById(messageId) } returns flowOf(null)
            coEvery { messageDao.saveMessage(mockMessage) } returns 123
            coEvery { protonMailApiManager.fetchMessageDetails(messageId, any()) } returns mockk {
                every { message } returns mockMessage
            }
            coEvery { messageBodyFileManager.saveMessageBodyToFile(mockMessage) } returns "file://messageBody"

            // when
            val result = messageRepository.getMessage(messageId, testUserId)

            // then
            assertEquals("file://messageBody", result?.messageBody)
            coVerify { messageBodyFileManager.saveMessageBodyToFile(mockMessage) }
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
            coEvery { messageDao.findMessageById(messageId) } returns flowOf(null)
            coEvery { protonMailApiManager.fetchMessageDetails(messageId, UserIdTag(testUserId)) } throws Exception()

            // when
            val result = messageRepository.getMessage(messageId, testUserId)

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
            coEvery { messageDao.findMessageById(messageId) } returns flowOf(null)
            coEvery { protonMailApiManager.fetchMessageMetadata(messageId, UserIdTag(testUserId)) } throws Exception()

            // when
            val result = messageRepository.getMessage(messageId, testUserId)

            // then
            assertNull(result)
        }
    }

    @Test
    fun verifyMessageDetailsAreFetchedIfShouldFetchMessageDetailsIsTrueWhenGetMessageIsCalledForUserWithAutoDownloadMessagesSettingTurnedOff() {
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
            coEvery { messageDao.findMessageById(messageId) } returns flowOf(null)
            coEvery { messageDao.saveMessage(mockMessage) } returns 123
            coEvery { protonMailApiManager.fetchMessageDetails(messageId, any()) } returns mockk {
                every { message } returns mockMessage
            }

            // when
            val result = messageRepository.getMessage(messageId, testUserId, shouldFetchMessageDetails = true)

            // then
            assertEquals(mockMessage, result)
        }
    }
}
