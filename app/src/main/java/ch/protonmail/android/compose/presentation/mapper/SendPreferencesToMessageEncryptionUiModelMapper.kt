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

package ch.protonmail.android.compose.presentation.mapper

import ch.protonmail.android.R
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.enumerations.PackageType
import ch.protonmail.android.details.presentation.model.MessageEncryptionUiModel
import me.proton.core.domain.arch.Mapper
import javax.inject.Inject

class SendPreferencesToMessageEncryptionUiModelMapper @Inject constructor() :
    Mapper<SendPreference, MessageEncryptionUiModel> {

    fun toMessageEncryptionUiModel(
        sendPreference: SendPreference,
        isMessagePasswordEncrypted: Boolean
    ) = MessageEncryptionUiModel(
        getIcon(sendPreference, isMessagePasswordEncrypted),
        getColor(sendPreference, isMessagePasswordEncrypted),
        getTooltip(sendPreference, isMessagePasswordEncrypted)
    )

    private fun getIcon(sendPreference: SendPreference, isMessagePasswordEncrypted: Boolean): Int {
        if (sendPreference.isEncryptionEnabled) {
            return if (sendPreference.isVerified && sendPreference.isPublicKeyPinned) {
                R.string.pgp_lock_check
            } else {
                R.string.lock_default
            }
        }
        if (isMessagePasswordEncrypted) {
            return R.string.lock_default
        }
        return if (sendPreference.isSignatureEnabled) {
            R.string.pgp_lock_pen
        } else {
            R.string.no_icon
        }
    }

    private fun getColor(sendPreference: SendPreference, isMessagePasswordEncrypted: Boolean): Int {
        if (sendPreference.encryptionScheme == PackageType.PM ||
            !sendPreference.isEncryptionEnabled && isMessagePasswordEncrypted
        ) {
            return R.color.icon_purple
        }
        return if (sendPreference.isPGP) {
            R.color.icon_green
        } else {
            R.color.icon_warning
        }
    }

    private fun getTooltip(sendPreference: SendPreference, isMessagePasswordEncrypted: Boolean): Int {
        if (sendPreference.encryptionScheme == PackageType.PM) {
            return if (sendPreference.isVerified && sendPreference.isPublicKeyPinned) {
                R.string.composer_lock_internal_pinned
            } else {
                R.string.composer_lock_internal
            }
        }
        if (!sendPreference.isEncryptionEnabled && isMessagePasswordEncrypted) {
            return R.string.composer_lock_internal
        }
        return if (sendPreference.isPGP) {
            if (sendPreference.isVerified && sendPreference.isEncryptionEnabled) {
                R.string.composer_lock_pgp_encrypted_pinned
            } else {
                R.string.composer_lock_pgp_signed
            }
        } else {
            0
        }
    }
}