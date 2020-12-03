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
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.DRAFT
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.domain.entity.Id
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SaveDraftTest : CoroutinesTest {

    @MockK
    private lateinit var addressCryptoFactory: AddressCrypto.Factory

    @RelaxedMockK
    lateinit var messageDetailsRepository: MessageDetailsRepository

    @InjectMockKs
    lateinit var saveDraft: SaveDraft

    @Test
    fun saveDraftSavesEncryptedDraftMessageToDb() =
        runBlockingTest {
            // Given
            val decryptedMessageBody = "Message body in plain text"
            val addressId = "addressId"
            val messageDbId = 123L
            val message = Message().apply {
                dbId = messageDbId
                addressID = addressId
                decryptedBody = decryptedMessageBody
            }
            val encryptedArmoredBody = "encrypted armored content"
            val addressCrypto = mockk<AddressCrypto> {
                every { encrypt(decryptedMessageBody, true).armored } returns encryptedArmoredBody
            }
            every { addressCryptoFactory.create(Id(addressId)) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns messageDbId

            // When
            saveDraft(message)

            // Then
            val expectedMessage = message.copy(messageBody = encryptedArmoredBody)
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
            val decryptedMessageBody = "Message body in plain text"
            val addressId = "addressId"
            val messageDbId = 123L
            val message = Message().apply {
                dbId = messageDbId
                addressID = addressId
                decryptedBody = decryptedMessageBody
            }
            val encryptedArmoredBody = "encrypted armored content"
            val addressCrypto = mockk<AddressCrypto> {
                every { encrypt(decryptedMessageBody, true).armored } returns encryptedArmoredBody
            }
            every { addressCryptoFactory.create(Id(addressId)) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns messageDbId

            // When
            saveDraft(message)

            // Then
            coVerify { messageDetailsRepository.insertPendingDraft(messageDbId) }
        }

}

