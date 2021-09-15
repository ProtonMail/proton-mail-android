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

import ch.protonmail.android.R
import ch.protonmail.android.api.models.enumerations.MessageEncryption
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.details.domain.model.MessageEncryptionStatus
import ch.protonmail.android.details.domain.model.SignatureVerification
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetEncryptionStatusTest {

    private val mockUserManager = mockk<ch.protonmail.android.core.UserManager> {
        every { currentLegacyUser } returns mockk {
            every { addresses } returns listOf()
        }
    }

    val getEncryptionStatus = GetEncryptionStatus()

    @BeforeTest
    fun setUp() {
        // This mock is only needed while covering the existing logic with tests.
        // Will be dropped once we migrate the logic from SenderLockIcon to GetEncryptedStatus
        mockkStatic(ProtonMailApplication::class)
        every { ProtonMailApplication.getApplication() } returns mockk {
            every { userManager } returns mockUserManager
        }
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(ProtonMailApplication::class)
    }

    @Test
    fun internalMessageWithUnknownSignatureVerificationIsRepresentedByBluePadlockAndE2eEncryptedAndSignedTooltip() {
        val messageEncryption = MessageEncryption.INTERNAL

        val actual = getEncryptionStatus(
            messageEncryption,
            SignatureVerification.UNKNOWN,
            false
        )

        val expected = MessageEncryptionStatus(
            R.string.lock_default,
            R.color.icon_purple,
            R.string.sender_lock_internal
        )
        assertEquals(expected, actual)
    }

    @Test
    fun internalMessageWithSuccessfulSignatureVerificationIsRepresentedByBluePadlockWithCheckmarkAndE2eEncryptedToVerifiedRecipientTooltip() {
        val messageEncryption = MessageEncryption.INTERNAL

        val actual = getEncryptionStatus(
            messageEncryption,
            SignatureVerification.SUCCESSFUL,
            false
        )

        val expected = MessageEncryptionStatus(
            R.string.pgp_lock_check,
            R.color.icon_purple,
            R.string.sender_lock_internal_verified
        )
        assertEquals(expected, actual)
    }

    @Test
    fun internalMessageWithFailedSignatureVerificationIsRepresentedByBluePadlockWithWarningAndSenderVerificationFailedTooltip() {
        val messageEncryption = MessageEncryption.INTERNAL

        val actual = getEncryptionStatus(
            messageEncryption,
            SignatureVerification.FAILED,
            false
        )

        val expected = MessageEncryptionStatus(
            R.string.pgp_lock_warning,
            R.color.icon_purple,
            R.string.sender_lock_verification_failed
        )
        assertEquals(expected, actual)
    }

    @Test
    fun externalSentMessageWithE2eeIsRepresentedByGreenPadlockAndSentByYouWithE2eeTooltip() {
        // The MessageEncryption values having e2ee are `INTERNAL` and `MIME_PGP`
        val messageEncryption = MessageEncryption.MIME_PGP

        val actual = getEncryptionStatus(
            messageEncryption,
            SignatureVerification.UNKNOWN,
            true
        )

        val expected = MessageEncryptionStatus(
            R.string.lock_default,
            R.color.icon_green,
            R.string.sender_lock_sent_end_to_end
        )
        assertEquals(expected, actual)
    }

    @Test
    fun externalSentMessageWithoutE2eeIsRepresentedByGrayPadlockAndStoredWithZeroAccessEncryptionTooltip() {
        val messageEncryption = MessageEncryption.EXTERNAL

        val actual = getEncryptionStatus(
            messageEncryption,
            SignatureVerification.UNKNOWN,
            true
        )

        val expected = MessageEncryptionStatus(
            R.string.lock_default,
            R.color.icon_gray,
            R.string.sender_lock_zero_access
        )
        assertEquals(expected, actual)
    }

    @Test
    fun autoResponseMessageIsRepresentedByBluePadlockAndSentByProtonmailWithZeroAccessEncryptionTooltip() {
        val messageEncryption = MessageEncryption.AUTO_RESPONSE

        val actual = getEncryptionStatus(
            messageEncryption,
            SignatureVerification.UNKNOWN,
            false
        )

        val expected = MessageEncryptionStatus(
            R.string.lock_default,
            R.color.icon_purple,
            R.string.sender_lock_sent_autoresponder
        )
        assertEquals(expected, actual)
    }

}
