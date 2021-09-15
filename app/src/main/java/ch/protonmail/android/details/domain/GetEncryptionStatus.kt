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
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.domain.model.MessageEncryptionStatus
import ch.protonmail.android.details.domain.model.SignatureVerification
import timber.log.Timber
import javax.inject.Inject

class GetEncryptionStatus @Inject constructor() {

    operator fun invoke(
        messageEncryption: MessageEncryption,
        signatureVerification: SignatureVerification,
        isMessageSent: Boolean
    ): MessageEncryptionStatus {
        val message = Message(messageEncryption = messageEncryption).apply {
            hasValidSignature = signatureVerification == SignatureVerification.SUCCESSFUL
            hasInvalidSignature = signatureVerification == SignatureVerification.FAILED
            Type = if (isMessageSent) Message.MessageType.SENT else Message.MessageType.INBOX
        }
        return MessageEncryptionStatus(
            getIcon(message),
            getColor(message),
            getTooltip(message)
        )
    }


    private fun getIcon(message: Message): Int {
        if (!message.messageEncryption!!.isStoredEncrypted) {
            return R.string.pgp_lock_open
        }
        if (message.hasInvalidSignature) {
            return R.string.pgp_lock_warning
        }
        if (message.isSent) {
            return if (!message.hasValidSignature && message.time > Constants.PM_SIGNATURES_START) {
                R.string.pgp_lock_warning
            } else {
                R.string.lock_default
            }
        }
        return if (message.hasValidSignature) {
            R.string.pgp_lock_check
        } else {
            R.string.lock_default
        }
    }

    private fun getColor(message: Message): Int {
        val messageEncryption = message.messageEncryption
        if (messageEncryption!!.isPGPEncrypted) {
            return R.color.icon_green
        }
        return if (messageEncryption.isEndToEndEncrypted || messageEncryption.isInternalEncrypted) {
            R.color.icon_purple
        } else {
            R.color.icon_gray
        }
    }

    private fun getTooltip(message: Message): Int {
        if (message.hasInvalidSignature) {
            return R.string.sender_lock_verification_failed
        }
        val messageEncryption = message.messageEncryption
        if (messageEncryption == MessageEncryption.AUTO_RESPONSE) {
            return R.string.sender_lock_sent_autoresponder
        }
        if (message.isSent) {
            return sentTooltip(message)
        }
        if (messageEncryption!!.isInternalEncrypted) {
            return internalTooltip(message)
        }
        if (messageEncryption.isPGPEncrypted) {
            return pGPTooltip(message)
        }
        // We only support EO (handled in sentTooltip), PGP and Internal for e2e-encryption
        // So this should never happen:
        if (messageEncryption.isEndToEndEncrypted) {
            Timber.w("Unhandled EndToEndEncryption tooltip!")
            return R.string.sender_lock_unknown_scheme
        }
        return if (messageEncryption.isStoredEncrypted) {
            zeroAccessTooltip(message)
        } else {
            R.string.sender_lock_unencrypted
        }
    }

    /**
     * Special case, we don't show when signatures are actually valid, but signatures should always be here
     * so instead, when a signature is not available we display an error.
     * This is a special case, because address verification happens always for sent messages
     * So we don't want to show the regular verified sender case if the user does not know about
     * address verification.
     */
    private fun sentTooltip(message: Message): Int {
        val messageEncryption = message.messageEncryption
        if (!message.hasValidSignature && message.time > Constants.PM_SIGNATURES_START) {
            return R.string.sender_lock_verification_failed
        }
        if (messageEncryption!!.isEndToEndEncrypted) {
            return R.string.sender_lock_sent_end_to_end
        }
        return R.string.sender_lock_zero_access
    }

    private fun internalTooltip(message: Message) =
        if (message.hasValidSignature) {
            R.string.sender_lock_internal_verified
        } else {
            R.string.sender_lock_internal
        }

    private fun pGPTooltip(message: Message) =
        if (message.hasValidSignature) {
            R.string.sender_lock_pgp_encrypted_verified
        } else {
            R.string.sender_lock_pgp_encrypted
        }

    private fun zeroAccessTooltip(message: Message) =
        if (message.hasValidSignature) {
            R.string.sender_lock_pgp_signed_verified_sender
        } else {
            R.string.sender_lock_zero_access
        }

}
