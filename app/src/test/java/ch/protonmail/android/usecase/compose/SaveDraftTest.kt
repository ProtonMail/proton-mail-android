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

package ch.protonmail.android.usecase.compose

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.DRAFT
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.worker.CreateDraftWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SaveDraftTest : CoroutinesTest {

    @RelaxedMockK
    private lateinit var createDraftScheduler: CreateDraftWorker.Enqueuer

    @RelaxedMockK
    private lateinit var pendingActionsDao: PendingActionsDao

    @RelaxedMockK
    private lateinit var addressCryptoFactory: AddressCrypto.Factory

    @RelaxedMockK
    lateinit var messageDetailsRepository: MessageDetailsRepository

    @InjectMockKs
    lateinit var saveDraft: SaveDraft

    private val currentUsername = "username"

    @Test
    fun saveDraftSavesEncryptedDraftMessageToDb() =
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            val addressCrypto = mockk<AddressCrypto> {
                every { encrypt("Message body in plain text", true).armored } returns "encrypted armored content"
            }
            every { addressCryptoFactory.create(Id("addressId"), Name(currentUsername)) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 123L

            // When
            saveDraft(message, emptyList(), null)

            // Then
            val expectedMessage = message.copy(messageBody = "encrypted armored content")
            expectedMessage.setLabelIDs(
                listOf(
                    ALL_DRAFT.messageLocationTypeValue.toString(),
                    ALL_MAIL.messageLocationTypeValue.toString(),
                    DRAFT.messageLocationTypeValue.toString()
                )
            )
            coVerify { messageDetailsRepository.saveMessageLocally(expectedMessage) }
        }

    @Test
    fun saveDraftInsertsPendingDraftInPendingActionsDatabase() =
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            val addressCrypto = mockk<AddressCrypto> {
                every { encrypt("Message body in plain text", true).armored } returns "encrypted armored content"
            }
            every { addressCryptoFactory.create(Id("addressId"), Name(currentUsername)) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 123L

            // When
            saveDraft(message, emptyList(), null)

            // Then
            coVerify { messageDetailsRepository.insertPendingDraft(123L) }
        }

    @Test
    fun saveDraftsInsertsPendingUploadWhenThereAreNewAttachments() =
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            val addressCrypto = mockk<AddressCrypto> {
                every { encrypt("Message body in plain text", true).armored } returns "encrypted armored content"
            }
            every { addressCryptoFactory.create(Id("addressId"), Name(currentUsername)) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 123L

            // When
            val newAttachments = listOf("attachmentId")
            saveDraft.invoke(message, newAttachments, "parentId")

            // Then
            verify { pendingActionsDao.insertPendingForUpload(PendingUpload("456")) }
        }

    @Test
    fun saveDraftsDoesNotInsertsPendingUploadWhenThereAreNoNewAttachments() =
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L

            // When
            saveDraft.invoke(message, emptyList(), "parentId")

            // Then
            verify(exactly = 0) { pendingActionsDao.insertPendingForUpload(any()) }
        }

    @Test
    fun saveDraftsSchedulesCreateDraftWorker() =
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L

            // When
            saveDraft.invoke(message, emptyList(), "parentId123")

            // Then
            verify { createDraftScheduler.enqueue(message, "parentId123") }
        }

}

