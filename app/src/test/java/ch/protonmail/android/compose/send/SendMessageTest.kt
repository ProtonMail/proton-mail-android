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

package ch.protonmail.android.compose.send

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.factories.MessageSecurityOptions
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import ch.protonmail.android.core.Constants.MessageActionType.NONE
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.utils.ServerTime
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Before
import org.junit.Test

class SendMessageTest : CoroutinesTest {

    @RelaxedMockK
    private lateinit var sendMessageScheduler: SendMessageWorker.Enqueuer

    @RelaxedMockK
    private lateinit var pendingActionsDao: PendingActionsDao

    @RelaxedMockK
    lateinit var messageDetailsRepository: MessageDetailsRepository

    @InjectMockKs
    lateinit var sendMessage: SendMessage

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun saveMessageAsNotDownloadedWithAllDraftsLocationAndCurrentTime() = runBlockingTest {
        // Given
        val message = Message(messageId = "9823472")
        val currentTimeMs = 23847233000L
        val securityOptions = MessageSecurityOptions("", "", -1L)
        mockkStatic(ServerTime::class)
        every { ServerTime.currentTimeMillis() } returns currentTimeMs

        // When
        val parameters = SendMessage.SendMessageParameters(message, listOf(), "", NONE, "", securityOptions)
        sendMessage(parameters)

        // Then
        val currentTimeSeconds = 23847233L
        val expectedMessage = message.copy(
            location = MessageLocationType.ALL_DRAFT.messageLocationTypeValue,
            time = currentTimeSeconds,
            isDownloaded = false
        )
        coVerify { messageDetailsRepository.saveMessageLocally(expectedMessage) }
        unmockkStatic(ServerTime::class)
    }

    @Test
    fun insertMessageAsPendingForSending() = runBlockingTest {
        // Given
        val messageId = "82347"
        val messageDbId = 82372L
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
        }
        val securityOptions = MessageSecurityOptions("", "", -1L)

        // When
        val parameters = SendMessage.SendMessageParameters(message, listOf(), "", NONE, "", securityOptions)
        sendMessage(parameters)

        // Then
        val pendingSend = PendingSend(messageId = messageId, localDatabaseId = messageDbId)
        coVerify { pendingActionsDao.insertPendingForSend(pendingSend) }
    }

    @Test
    fun sendMessageSchedulesSendMessageWorkerToPerformOperationInBackground() = runBlockingTest {
        // Given
        val message = Message().apply {
            dbId = 992376L
            this.messageId = "823742"
            addressID = "addressId"
            decryptedBody = "Message body in plain text"
        }
        val securityOptions = MessageSecurityOptions("secretPassword", "hint", 237237L)

        // When
        val attachmentIds = listOf("23364382")
        val parameters = SendMessage.SendMessageParameters(
            message,
            attachmentIds,
            "parentId82346",
            NONE,
            "previousSenderId8372",
            securityOptions
        )
        sendMessage(parameters)

        // Then
        verify {
            sendMessageScheduler.enqueue(
                message,
                attachmentIds,
                "parentId82346",
                NONE,
                "previousSenderId8372",
                securityOptions
            )
        }
    }
}
