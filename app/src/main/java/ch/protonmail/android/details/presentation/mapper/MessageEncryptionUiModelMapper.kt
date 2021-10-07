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

package ch.protonmail.android.details.presentation.mapper

import ch.protonmail.android.R
import ch.protonmail.android.api.models.enumerations.MessageEncryption
import ch.protonmail.android.details.domain.model.SignatureVerification
import ch.protonmail.android.details.presentation.model.MessageEncryptionUiModel
import me.proton.core.domain.arch.Mapper
import timber.log.Timber
import javax.inject.Inject

class MessageEncryptionUiModelMapper @Inject constructor() : Mapper<MessageEncryption, MessageEncryptionUiModel> {

    fun messageEncryptionToUiModel(
        messageEncryption: MessageEncryption,
        signatureVerification: SignatureVerification,
        isMessageSent: Boolean
    ): MessageEncryptionUiModel {
        return MessageEncryptionUiModel(
            getIcon(signatureVerification, isMessageSent),
            getColor(messageEncryption),
            getTooltip(messageEncryption, signatureVerification, isMessageSent)
        )
    }


    private fun getIcon(
        signatureVerification: SignatureVerification,
        isMessageSent: Boolean
    ): Int {
        if (signatureVerification == SignatureVerification.FAILED) {
            return R.string.pgp_lock_warning
        }

        return if (signatureVerification == SignatureVerification.SUCCESSFUL && !isMessageSent) {
            R.string.pgp_lock_check
        } else {
            R.string.lock_default
        }
    }

    private fun getColor(messageEncryption: MessageEncryption): Int {
        if (messageEncryption.isPGPEncrypted) {
            return R.color.icon_green
        }
        if (messageEncryption.isEndToEndEncrypted || messageEncryption.isInternalEncrypted) {
            return R.color.icon_purple
        }
        return R.color.icon_gray
    }

    private fun getTooltip(
        messageEncryption: MessageEncryption,
        signatureVerification: SignatureVerification,
        isMessageSent: Boolean
    ): Int {
        if (signatureVerification == SignatureVerification.FAILED) {
            return R.string.sender_lock_verification_failed
        }
        if (messageEncryption == MessageEncryption.AUTO_RESPONSE) {
            return R.string.sender_lock_sent_autoresponder
        }
        if (isMessageSent) {
            return sentTooltip(messageEncryption)
        }
        if (messageEncryption.isInternalEncrypted) {
            return internalTooltip(signatureVerification)
        }
        if (messageEncryption.isPGPEncrypted) {
            return pGPTooltip(signatureVerification)
        }
        // We only support EO (handled in sentTooltip), PGP and Internal for e2e-encryption
        // So this should never happen:
        if (messageEncryption.isEndToEndEncrypted) {
            Timber.w("Unhandled EndToEndEncryption tooltip!")
            return R.string.sender_lock_unknown_scheme
        }
        return zeroAccessTooltip(signatureVerification)
    }

    private fun sentTooltip(
        messageEncryption: MessageEncryption
    ): Int {
        if (messageEncryption.isEndToEndEncrypted) {
            return R.string.sender_lock_sent_end_to_end
        }
        return R.string.sender_lock_zero_access
    }

    private fun internalTooltip(signatureVerification: SignatureVerification) =
        if (signatureVerification == SignatureVerification.SUCCESSFUL) {
            R.string.sender_lock_internal_verified
        } else {
            R.string.sender_lock_internal
        }

    private fun pGPTooltip(signatureVerification: SignatureVerification) =
        if (signatureVerification == SignatureVerification.SUCCESSFUL) {
            R.string.sender_lock_pgp_encrypted_verified
        } else {
            R.string.sender_lock_pgp_encrypted
        }

    private fun zeroAccessTooltip(signatureVerification: SignatureVerification) =
        if (signatureVerification == SignatureVerification.SUCCESSFUL) {
            R.string.sender_lock_pgp_signed_verified_sender
        } else {
            R.string.sender_lock_zero_access
        }

}
