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

package ch.protonmail.android.compose.send

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.factories.MessageSecurityOptions
import ch.protonmail.android.core.Constants.MessageActionType.NONE
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.pendingaction.data.PendingActionDao
import ch.protonmail.android.pendingaction.data.model.PendingSend
import ch.protonmail.android.pendingaction.domain.repository.PendingSendRepository
import ch.protonmail.android.testdata.MessageTestData
import ch.protonmail.android.testdata.UserTestData.userId
import ch.protonmail.android.utils.ServerTime
import ch.protonmail.android.utils.UuidProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.user.domain.entity.AddressId
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SendMessageTest : CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private val addressCryptoFactory = mockk<AddressCrypto.Factory>(relaxed = true)
    private val sendMessageScheduler = mockk<SendMessageWorker.Enqueuer> {
        every { enqueue(any(), any(), any(), any(), any(), any(), any()) } returns flowOf(mockk())
    }
    private val pendingActionDao = mockk<PendingActionDao>(relaxUnitFun = true)
    private val messageDetailsRepository = mockk<MessageDetailsRepository> {
        coEvery { saveMessage(any()) } returns 1L
    }
    private val uuidProvider = mockk<UuidProvider> {
        every { randomUuid() } returns RANDOM_UUID
    }

    private val databaseProvider: DatabaseProvider = mockk {
        every { providePendingActionDao(any()) } returns pendingActionDao
    }

    private val pendingSendRepository = mockk<PendingSendRepository>(relaxUnitFun = true)
    private lateinit var sendMessage: SendMessage

    @Before
    fun setUp() {
        sendMessage = SendMessage(
            messageDetailsRepository,
            dispatchers,
            databaseProvider,
            sendMessageScheduler,
            pendingSendRepository,
            addressCryptoFactory,
            uuidProvider
        )
    }

    @Test
    fun saveMessageEncryptsMessageBodyAndSavesItLocally() = runTest(dispatchers.Main) {
        // Given
        val senderAddressId = "addressId"
        val message = Message(messageId = "9823472", addressID = senderAddressId)
        val decryptedBody = "Message body in plain text"
        message.decryptedBody = decryptedBody
        val securityOptions = MessageSecurityOptions("", "", -1L)
        val addressCrypto = mockk<AddressCrypto> {
            every { encrypt(decryptedBody, true).armored } returns "encrypted armored content"
        }
        every { addressCryptoFactory.create(userId, AddressId(senderAddressId)) } returns addressCrypto

        // When
        val parameters = SendMessage.SendMessageParameters(
            userId = userId,
            message = message,
            newAttachmentIds = listOf(),
            parentId = "",
            actionType = NONE,
            previousSenderAddressId = "",
            securityOptions = securityOptions
        )
        sendMessage(parameters)

        // Then

        val messageCaptor = slot<Message>()
        verify { addressCrypto.encrypt(decryptedBody, true) }
        coVerify(exactly = 1) { messageDetailsRepository.saveMessage(capture(messageCaptor)) }
        assertEquals("encrypted armored content", messageCaptor.captured.messageBody)
    }

    @Test
    fun saveMessageAsNotDownloadedWithAllDraftsLocationAndCurrentTime() = runTest(dispatchers.Main) {
        // Given
        val message = Message(messageId = "9823472", addressID = "addressId")
        val currentTimeMs = 23_847_233_000L
        val securityOptions = MessageSecurityOptions("", "", -1L)
        mockkStatic(ServerTime::class)
        every { ServerTime.currentTimeMillis() } returns currentTimeMs

        // When
        val parameters = SendMessage.SendMessageParameters(
            userId = userId,
            message = message,
            newAttachmentIds = listOf(),
            parentId = "",
            actionType = NONE,
            previousSenderAddressId = "",
            securityOptions = securityOptions
        )
        sendMessage(parameters)

        // Then
        val currentTimeSeconds = 23_847_233L
        val expectedMessage = message.copy(
            location = MessageLocationType.ALL_DRAFT.messageLocationTypeValue,
            time = currentTimeSeconds,
            isDownloaded = false
        )
        coVerify { messageDetailsRepository.saveMessage(expectedMessage) }
        unmockkStatic(ServerTime::class)
    }

    @Test
    fun insertMessageAsPendingForSending() = runTest(dispatchers.Main) {
        // Given
        val messageId = "82347"
        val messageDbId = 82_372L
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            addressID = "AddressId"
        }
        val securityOptions = MessageSecurityOptions("", "", -1L)

        // When
        val parameters = SendMessage.SendMessageParameters(
            userId = userId,
            message = message,
            newAttachmentIds = listOf(),
            parentId = "",
            actionType = NONE,
            previousSenderAddressId = "",
            securityOptions = securityOptions
        )
        sendMessage(parameters)

        // Then
        val expectedPendingSend = PendingSend(id = RANDOM_UUID, messageId = messageId, localDatabaseId = messageDbId)
        coVerify { pendingActionDao.insertPendingForSend(expectedPendingSend) }
    }

    @Test
    fun `should schedule a send and a pending send cleanup when message sent`() = runTest(dispatchers.Main) {
        // Given
        val decryptedMessageBody = "Message body in plain text"
        val message = Message().apply {
            dbId = MessageTestData.MESSAGE_DATABASE_ID
            messageId = MessageTestData.MESSAGE_ID_RAW
            subject = MessageTestData.MESSAGE_SUBJECT
            addressID = "addressId"
            decryptedBody = decryptedMessageBody
        }
        val securityOptions = MessageSecurityOptions("secretPassword", "hint", 237_237L)

        // When
        val attachmentIds = listOf("23364382")
        val parameters = SendMessage.SendMessageParameters(
            userId = userId,
            message = message,
            newAttachmentIds = attachmentIds,
            parentId = "parentId82346",
            actionType = NONE,
            previousSenderAddressId = "previousSenderId8372",
            securityOptions = securityOptions
        )
        sendMessage(parameters)

        // Then
        verify {
            sendMessageScheduler.enqueue(
                userId,
                message,
                attachmentIds,
                "parentId82346",
                NONE,
                "previousSenderId8372",
                securityOptions
            )
        }
        verify {
            pendingSendRepository.schedulePendingSendCleanupByMessageId(
                MessageTestData.MESSAGE_ID_RAW,
                MessageTestData.MESSAGE_SUBJECT,
                MessageTestData.MESSAGE_DATABASE_ID,
                userId
            )
        }
    }

    private companion object TestData {

        const val RANDOM_UUID = "UUID"
    }
}
