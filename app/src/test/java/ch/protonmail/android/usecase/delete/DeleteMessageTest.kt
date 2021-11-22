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
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.data.local.PendingActionDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.PendingSend
import ch.protonmail.android.data.local.model.PendingUpload
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.worker.DeleteMessageWorker
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeleteMessageTest {

    private val workScheduler: DeleteMessageWorker.Enqueuer = mockk()

    private val messageDetailsRepository: MessageDetailsRepository = mockk()
    private val messageDetailsRepositoryFactory: MessageDetailsRepository.AssistedFactory = mockk() {
        every { create(any()) } returns messageDetailsRepository
    }

    private val pendingActionDao: PendingActionDao = mockk()
    private val conversationsRepository: ConversationsRepository = mockk()

    private val databaseProvider: DatabaseProvider = mockk {
        every { providePendingActionDao(any()) } returns pendingActionDao
    }

    private lateinit var deleteMessage: DeleteMessage

    private val messId = "Id1"

    private val currentLabelId = "3"  // Constants.MessageLocationType.TRASH

    private val userId = UserId("userId")

    private val message = mockk<Message>(relaxed = true)

    private val operation = mockk<Operation>(relaxed = true)

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        deleteMessage = DeleteMessage(
            databaseProvider,
            conversationsRepository,
            TestDispatcherProvider,
            messageDetailsRepositoryFactory,
            workScheduler
        )

        coEvery {
            messageDetailsRepository.saveMessagesInOneTransaction(any())
        } just runs
        coEvery {
            conversationsRepository.updateConversationsWhenDeletingMessages(userId, any())
        } just runs
        every { workScheduler.enqueue(any(), any()) } returns operation
    }

    @Test
    fun verifyThatMessageIsSuccessfullyDeletedWithoutPendingMessagesInTheDb() {
        runBlockingTest {
            // given
            every { pendingActionDao.findPendingUploadByMessageId(any()) } returns null
            every { pendingActionDao.findPendingSendByMessageId(any()) } returns null
            every { messageDetailsRepository.findMessageById(messId) } returns flowOf(message)

            // when
            val response = deleteMessage(listOf(messId), currentLabelId, userId)

            // then
            coVerify {
                messageDetailsRepository.saveMessagesInOneTransaction(listOf(message))
                conversationsRepository.updateConversationsWhenDeletingMessages(userId, listOf(messId))
            }
            verify { workScheduler.enqueue(listOf(messId), currentLabelId) }
            assertTrue(response.isSuccessfullyDeleted)
        }
    }

    @Test
    fun verifyThatMessageIsSuccessfullyDeletedWithPendingUploadMessageInTheDb() {
        runBlockingTest {
            // given
            val pendingUpload = mockk<PendingUpload>(relaxed = true)
            every { pendingActionDao.findPendingUploadByMessageId(any()) } returns pendingUpload
            every { pendingActionDao.findPendingSendByMessageId(any()) } returns null
            every { messageDetailsRepository.findMessageByIdBlocking(messId) } returns message
            coEvery { messageDetailsRepository.saveMessage(message) } returns 1L

            // when
            val response = deleteMessage(listOf(messId), currentLabelId, userId)

            // then
            coVerify {
                messageDetailsRepository.saveMessagesInOneTransaction(emptyList())
                conversationsRepository.updateConversationsWhenDeletingMessages(userId, emptyList())
            }
            verify { workScheduler.enqueue(emptyList(), currentLabelId) }
            assertFalse(response.isSuccessfullyDeleted)
        }
    }

    @Test
    fun verifyThatMessageIsSuccessfullyDeletedWithPendingSendMessageInTheDb() {
        runBlockingTest {
            // given
            val pendingSend = mockk<PendingSend>(relaxed = true) {
                every { sent } returns true
            }
            every { pendingActionDao.findPendingUploadByMessageId(any()) } returns null
            every { pendingActionDao.findPendingSendByMessageId(any()) } returns pendingSend
            every { messageDetailsRepository.findMessageByIdBlocking(messId) } returns null
            coEvery { messageDetailsRepository.saveMessage(message) } returns 1L

            // when
            val response = deleteMessage(listOf(messId), currentLabelId, userId)

            // then
            coVerify {
                messageDetailsRepository.saveMessagesInOneTransaction(emptyList())
                conversationsRepository.updateConversationsWhenDeletingMessages(userId, emptyList())
            }
            verify { workScheduler.enqueue(emptyList(), currentLabelId) }
            assertFalse(response.isSuccessfullyDeleted)
        }
    }
}
