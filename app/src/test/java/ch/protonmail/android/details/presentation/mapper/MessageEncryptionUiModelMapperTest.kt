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

package ch.protonmail.android.details.presentation.mapper

import ch.protonmail.android.R
import ch.protonmail.android.api.models.enumerations.MessageEncryption
import ch.protonmail.android.details.domain.model.SignatureVerification
import ch.protonmail.android.details.presentation.model.MessageEncryptionUiModel
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageEncryptionUiModelMapperTest {

    private val messageEncryptionMapper = MessageEncryptionUiModelMapper()

    @Test
    fun internalMessageWithUnknownSignatureVerificationIsRepresentedByBluePadlockAndE2eEncryptedAndSignedTooltip() {
        val messageEncryption = MessageEncryption.INTERNAL

        val actual = messageEncryptionMapper.messageEncryptionToUiModel(
            messageEncryption,
            SignatureVerification.UNKNOWN,
            false
        )

        val expected = MessageEncryptionUiModel(
            R.string.lock_default,
            R.color.icon_purple,
            R.string.sender_lock_internal
        )
        assertEquals(expected, actual)
    }

    @Test
    fun internalMessageWithSuccessfulSignatureVerificationIsRepresentedByBluePadlockWithCheckmarkAndE2eEncryptedToVerifiedRecipientTooltip() {
        val messageEncryption = MessageEncryption.INTERNAL

        val actual = messageEncryptionMapper.messageEncryptionToUiModel(
            messageEncryption,
            SignatureVerification.SUCCESSFUL,
            false
        )

        val expected = MessageEncryptionUiModel(
            R.string.pgp_lock_check,
            R.color.icon_purple,
            R.string.sender_lock_internal_verified
        )
        assertEquals(expected, actual)
    }

    @Test
    fun internalMessageWithFailedSignatureVerificationIsRepresentedByBluePadlockWithWarningAndSenderVerificationFailedTooltip() {
        val messageEncryption = MessageEncryption.INTERNAL

        val actual = messageEncryptionMapper.messageEncryptionToUiModel(
            messageEncryption,
            SignatureVerification.FAILED,
            false
        )

        val expected = MessageEncryptionUiModel(
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

        val actual = messageEncryptionMapper.messageEncryptionToUiModel(
            messageEncryption,
            SignatureVerification.UNKNOWN,
            true
        )

        val expected = MessageEncryptionUiModel(
            R.string.lock_default,
            R.color.icon_green,
            R.string.sender_lock_sent_end_to_end
        )
        assertEquals(expected, actual)
    }

    @Test
    fun externalSentMessageWithoutE2eeIsRepresentedByGrayPadlockAndStoredWithZeroAccessEncryptionTooltip() {
        val messageEncryption = MessageEncryption.EXTERNAL

        val actual = messageEncryptionMapper.messageEncryptionToUiModel(
            messageEncryption,
            SignatureVerification.UNKNOWN,
            true
        )

        val expected = MessageEncryptionUiModel(
            R.string.lock_default,
            R.color.icon_gray,
            R.string.sender_lock_zero_access
        )
        assertEquals(expected, actual)
    }

    @Test
    fun autoResponseMessageIsRepresentedByBluePadlockAndSentByProtonmailWithZeroAccessEncryptionTooltip() {
        val messageEncryption = MessageEncryption.AUTO_RESPONSE

        val actual = messageEncryptionMapper.messageEncryptionToUiModel(
            messageEncryption,
            SignatureVerification.UNKNOWN,
            false
        )

        val expected = MessageEncryptionUiModel(
            R.string.lock_default,
            R.color.icon_purple,
            R.string.sender_lock_sent_autoresponder
        )
        assertEquals(expected, actual)
    }

    @Test
    fun externalPgpMessageWithSuccessfulSignatureVerificationIsRepresentedByGreenCheckedPadlockAndPgpEncryptedMessageFromVerifiedAddressTooltip() {
        val messageEncryption = MessageEncryption.EXTERNAL_PGP

        val actual = messageEncryptionMapper.messageEncryptionToUiModel(
            messageEncryption,
            SignatureVerification.SUCCESSFUL,
            false
        )

        val expected = MessageEncryptionUiModel(
            R.string.pgp_lock_check,
            R.color.icon_green,
            R.string.sender_lock_pgp_encrypted_verified
        )
        assertEquals(expected, actual)
    }

    @Test
    fun externalPgpMessageWithUnknownSignatureVerificationIsRepresentedByGreenPadlockAndPgpEncryptedMessageTooltip() {
        val messageEncryption = MessageEncryption.EXTERNAL_PGP

        val actual = messageEncryptionMapper.messageEncryptionToUiModel(
            messageEncryption,
            SignatureVerification.UNKNOWN,
            false
        )

        val expected = MessageEncryptionUiModel(
            R.string.lock_default,
            R.color.icon_green,
            R.string.sender_lock_pgp_encrypted
        )
        assertEquals(expected, actual)
    }

    @Test
    fun externalPgpMessageWithFailedSignatureVerificationIsRepresentedByGreenPadlockWithWarningAndVerificationFailedTooltip() {
        val messageEncryption = MessageEncryption.EXTERNAL_PGP

        val actual = messageEncryptionMapper.messageEncryptionToUiModel(
            messageEncryption,
            SignatureVerification.FAILED,
            false
        )

        val expected = MessageEncryptionUiModel(
            R.string.pgp_lock_warning,
            R.color.icon_green,
            R.string.sender_lock_verification_failed
        )
        assertEquals(expected, actual)
    }

}
