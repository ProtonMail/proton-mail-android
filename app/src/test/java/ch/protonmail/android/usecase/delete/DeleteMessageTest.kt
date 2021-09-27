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

package ch.protonmail.android.usecase.delete

import androidx.work.Operation
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.worker.DeleteMessageWorker
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeleteMessageTest {

    @MockK
    private lateinit var workScheduler: DeleteMessageWorker.Enqueuer

    @MockK
    private lateinit var db: PendingActionsDao

    @MockK
    private lateinit var repository: MessageDetailsRepository

    private lateinit var deleteMessage: DeleteMessage

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        deleteMessage = DeleteMessage(TestDispatcherProvider, repository, db, workScheduler)
    }

    @Test
    fun verifyThatMessageIsSuccessfullyDeletedWithoutPendingMessagesInTheDb() {
        runBlockingTest {
            // given
            val messId = "Id1"
            val currentLabelId = "3"  //Constants.MessageLocationType.TRASH
            val message = mockk<Message>(relaxed = true)
            val operation = mockk<Operation>(relaxed = true)
            every { db.findPendingUploadByMessageId(any()) } returns null
            every { db.findPendingSendByMessageId(any()) } returns null
            coEvery { repository.findMessageById(messId) } returns message
            every { repository.saveMessageInDB(message) } returns 1L
            coEvery { repository.findSearchMessageById(messId) } returns null
            every { repository.saveMessagesInOneTransaction(any()) } returns Unit
            every { repository.saveSearchMessagesInOneTransaction(any()) } returns Unit
            every { workScheduler.enqueue(any(), any()) } returns operation

            // when
            val response = deleteMessage(listOf(messId), currentLabelId)

            // then
            verify { repository.saveMessagesInOneTransaction(listOf(message)) }
            verify { repository.saveSearchMessagesInOneTransaction(emptyList()) }
            verify { workScheduler.enqueue(listOf(messId), currentLabelId) }
            assertTrue(response.isSuccessfullyDeleted)
        }
    }

    @Test
    fun verifyThatSearchMessageIsSuccessfullyDeletedWithoutPendingMessagesInTheDb() {
        runBlockingTest {
            // given
            val messId = "Id1"
            val currentLabelId = "3"  //Constants.MessageLocationType.TRASH
            val operation = mockk<Operation>(relaxed = true)
            val searchMessage = mockk<Message>(relaxed = true)
            every { db.findPendingUploadByMessageId(any()) } returns null
            every { db.findPendingSendByMessageId(any()) } returns null
            coEvery { repository.findMessageById(messId) } returns null
            coEvery { repository.findSearchMessageById(messId) } returns searchMessage
            every { repository.saveSearchMessageInDB(searchMessage) } returns Unit
            every { repository.saveMessagesInOneTransaction(any()) } returns Unit
            every { repository.saveSearchMessagesInOneTransaction(any()) } returns Unit
            every { workScheduler.enqueue(any(), any()) } returns operation

            // when
            val response = deleteMessage(listOf(messId), currentLabelId)

            // then
            verify { repository.saveMessagesInOneTransaction(emptyList()) }
            verify { repository.saveSearchMessagesInOneTransaction(listOf(searchMessage)) }
            verify { workScheduler.enqueue(listOf(messId), currentLabelId) }
            assertTrue(response.isSuccessfullyDeleted)
        }
    }

    @Test
    fun verifyThatMessageIsSuccessfullyDeletedWithPendingUploadMessageInTheDb() {
        runBlockingTest {
            // given
            val messId = "Id1"
            val currentLabelId = "3"  //Constants.MessageLocationType.TRASH
            val message = mockk<Message>(relaxed = true)
            val pendingUpload = mockk<PendingUpload>(relaxed = true)
            val operation = mockk<Operation>(relaxed = true)
            every { db.findPendingUploadByMessageId(any()) } returns pendingUpload
            every { db.findPendingSendByMessageId(any()) } returns null
            every { repository.findMessageByIdBlocking(messId) } returns message
            every { repository.saveMessageInDB(message) } returns 1L
            coEvery { repository.findSearchMessageById(messId) } returns null
            every { repository.saveMessagesInOneTransaction(any()) } returns Unit
            every { repository.saveSearchMessagesInOneTransaction(any()) } returns Unit
            every { workScheduler.enqueue(any(), any()) } returns operation

            // when
            val response = deleteMessage(listOf(messId), currentLabelId)

            // then
            verify { repository.saveMessagesInOneTransaction(emptyList()) }
            verify { repository.saveSearchMessagesInOneTransaction(emptyList()) }
            verify { workScheduler.enqueue(emptyList(), currentLabelId) }
            assertFalse(response.isSuccessfullyDeleted)
        }
    }

    @Test
    fun verifyThatMessageIsSuccessfullyDeletedWithPendingSendMessageInTheDb() {
        runBlockingTest {
            // given
            val messId = "Id1"
            val currentLabelId = "3"  //Constants.MessageLocationType.TRASH
            val message = mockk<Message>(relaxed = true)
            val pendingSend = mockk<PendingSend>(relaxed = true) {
                every { sent } returns true
            }
            val operation = mockk<Operation>(relaxed = true)
            every { db.findPendingUploadByMessageId(any()) } returns null
            every { db.findPendingSendByMessageId(any()) } returns pendingSend
            every { repository.findMessageByIdBlocking(messId) } returns null
            every { repository.findSearchMessageByIdBlocking(messId) } returns message
            every { repository.saveMessageInDB(message) } returns 1L
            every { repository.saveSearchMessageInDB(message) } returns Unit
            every { repository.saveMessagesInOneTransaction(any()) } returns Unit
            every { repository.saveSearchMessagesInOneTransaction(any()) } returns Unit
            every { workScheduler.enqueue(any(), any()) } returns operation

            // when
            val response = deleteMessage(listOf(messId), currentLabelId)

            // then
            verify { repository.saveMessagesInOneTransaction(emptyList()) }
            verify { repository.saveSearchMessagesInOneTransaction(emptyList()) }
            verify { workScheduler.enqueue(emptyList(), currentLabelId) }
            assertFalse(response.isSuccessfullyDeleted)
        }
    }
}
