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

package ch.protonmail.android.activities.messageDetails.body

import androidx.annotation.VisibleForTesting
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.utils.crypto.KeyInformation
import timber.log.Timber
import javax.inject.Inject

@VisibleForTesting
const val SIGNATURE_VERIFICATION_ERROR = "Signature Verification Error: No matching signature"

@Suppress("Deprecation")
class MessageBodyDecryptor @Inject constructor(
    private val userManager: UserManager
) {

    operator fun invoke(
        message: Message,
        publicKeys: List<KeyInformation>?
    ): Boolean {
        return if (message.decryptedHTML.isNullOrEmpty()) {
            tryToDecrypt(message, publicKeys)
        } else {
            true
        }
    }

    private fun tryToDecrypt(message: Message, publicKeys: List<KeyInformation>?): Boolean {
        return try {
            message.decrypt(userManager, userManager.requireCurrentUserId(), publicKeys)
            true
        } catch (exception: Exception) {
            // signature verification failed with special case, try to decrypt again without verification
            // and hardcode verification error
            if (publicKeys != null && publicKeys.isNotEmpty() && exception.message == SIGNATURE_VERIFICATION_ERROR) {
                Timber.i(exception, "Decrypting message again without verkeys")
                message.decrypt(userManager, userManager.requireCurrentUserId())
                message.hasValidSignature = false
                message.hasInvalidSignature = true
                true
            } else {
                Timber.w(exception, "Cannot decrypt message")
                false
            }
        }
    }
}
