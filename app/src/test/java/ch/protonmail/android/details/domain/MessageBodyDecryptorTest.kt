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

package ch.protonmail.android.details.domain

import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.testdata.KeyInformationTestData
import ch.protonmail.android.testdata.MessageTestData
import ch.protonmail.android.testdata.UserIdTestData
import ch.protonmail.android.utils.crypto.KeyInformation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageBodyDecryptorTest {

    private val userManagerMock = mockk<UserManager> {
        every { requireCurrentUserId() } returns UserIdTestData.userId
    }
    private val messageBodyDecryptor = MessageBodyDecryptor(userManagerMock)

    @Test
    fun `should decrypt message and return true if decryption succeeded`() {
        // given
        val decryptedMessageSpy = MessageTestData.messageSpy()

        // when
        val decryptionSucceeded = messageBodyDecryptor(
            message = decryptedMessageSpy,
            publicKeys = KeyInformationTestData.listWithValidKey
        )

        // then
        assertTrue(decryptionSucceeded)
        verify {
            decryptedMessageSpy.decrypt(
                userManagerMock,
                UserIdTestData.userId,
                KeyInformationTestData.listWithValidKey
            )
        }
    }

    @Test
    fun `should try to decrypt the message without keys and return true when initial attempt failed`() {
        // given
        val decryptedMessageSpy = MessageTestData.messageSpy().throwingException()

        // when
        val decryptionSucceeded = messageBodyDecryptor(
            message = decryptedMessageSpy,
            publicKeys = KeyInformationTestData.listWithValidKey
        )

        // then
        assertTrue(decryptionSucceeded)
        assertFalse(decryptedMessageSpy.hasValidSignature)
        assertTrue(decryptedMessageSpy.hasInvalidSignature)
        verify {
            decryptedMessageSpy.decrypt(
                userManagerMock,
                UserIdTestData.userId
            )
        }
    }

    @Test
    fun `should return false when initial attempt failed and public keys are null`() {
        // given
        val publicKeys = null
        val decryptedMessageSpy = MessageTestData.messageSpy().throwingException(publicKeys)

        // when
        val decryptionSucceeded = messageBodyDecryptor(
            message = decryptedMessageSpy,
            publicKeys = publicKeys
        )

        // then
        assertFalse(decryptionSucceeded)
    }

    @Test
    fun `should return false when initial attempt failed and public keys are empty`() {
        // given
        val publicKeys = emptyList<KeyInformation>()
        val decryptedMessageSpy = MessageTestData.messageSpy().throwingException(publicKeys)

        // when
        val decryptionSucceeded = messageBodyDecryptor(
            message = decryptedMessageSpy,
            publicKeys = publicKeys
        )

        // then
        assertFalse(decryptionSucceeded)
    }

    @Test
    fun `should return true and not try to decrypt if the message already has a decrypted html`() {
        // given
        val decryptedMessageSpy = MessageTestData.messageSpy().apply {
            decryptedHTML = "I am decrypted"
        }

        // when
        val decryptionSucceeded = messageBodyDecryptor(
            message = decryptedMessageSpy,
            publicKeys = KeyInformationTestData.listWithValidKey
        )

        // then
        assertTrue(decryptionSucceeded)
        verify(exactly = 0) { decryptedMessageSpy.decrypt(any(), any(), any()) }
    }

    private fun Message.throwingException(
        publicKeys: List<KeyInformation>? = KeyInformationTestData.listWithValidKey
    ) = apply {
        every {
            decrypt(userManagerMock, UserIdTestData.userId, publicKeys)
        } throws Exception(SIGNATURE_VERIFICATION_ERROR)
    }
}
