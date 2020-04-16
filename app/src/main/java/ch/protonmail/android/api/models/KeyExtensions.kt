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
package ch.protonmail.android.api.models

import android.os.Build
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.crypto.OpenPGP
import com.proton.gopenpgp.armor.Armor
import io.sentry.Sentry
import io.sentry.event.EventBuilder
import timber.log.Timber

/**
 * This class offers some helper methods for User/AddressKey, which should be moved later to models
 * after we migrate to GopenPGP and rename "Keys" to something more meaningful.
 */

/**
 * AddressKeys in old format are encrypted with user mailbox password. New format is encrypted with Token which
 * has to be extracted from AddressKey.
 *
 * @return null if we couldn't determine passphrase for some reason
 */
fun Keys.getKeyPassphrase(openPgp: OpenPGP, userKeys: List<Keys>, userKeysPassphrase: ByteArray): ByteArray? {
    if (this.Token.isNullOrBlank() || this.Signature.isNullOrBlank()) {
        return userKeysPassphrase
    } else {
        // try decrypting Token with all UserKeys we have
        userKeys.forEach {
            try {
                val decryptedToken = openPgp.decryptMessage(this.token, it.privateKey, userKeysPassphrase)
                val validSignature = openPgp.verifyTextSignDetached(this.signature, decryptedToken, Armor.armorKey(openPgp.getPublicKey(it.privateKey)), 0)

                if (validSignature) return decryptedToken.toByteArray(Charsets.UTF_8)
            } catch (e: Exception) {
                Timber.d(e, "exception decrypting AddressKey's Token")
            }
            val message = "failed getting passphrase for key $id, primary = $isPrimary, signature.isNullOrBlank = ${signature.isNullOrBlank()}, token.isNullOrBlank = ${token.isNullOrBlank()}, activation.isNullOrBlank = ${activation.isNullOrBlank()}"
            val eventBuilder = EventBuilder()
                    .withTag(Constants.LogTags.SENDING_FAILED_TAG, AppUtil.getAppVersion())
                    .withTag(Constants.LogTags.SENDING_FAILED_DEVICE_TAG, Build.MODEL + " " + Build.VERSION.SDK_INT)
            Sentry.capture(eventBuilder.withMessage(message).build())
        }

        Timber.e("failed getting passphrase for key %s, primary = %s, signature.isNullOrBlank = %s, token.isNullOrBlank = %s, activation.isNullOrBlank = %s",
            this.id, this.isPrimary, this.signature.isNullOrBlank(), this.token.isNullOrBlank(), this.activation.isNullOrBlank())

        return null
    }
}

/**
 * Decrypt Activation Token for AddressKey.
 *
 * @return null if we couldn't determine passphrase for some reason
 */
fun Keys.getActivationToken(openPgp: OpenPGP, userKeys: List<Keys>, userKeysPassphrase: ByteArray): String? {

    if (!this.Activation.isNullOrBlank()) {

        // try decrypting Activation Token with all UserKeys we have
        userKeys.forEach {

            try {
                return openPgp.decryptMessage(this.activation, it.privateKey, userKeysPassphrase)
            } catch (e: Exception) {
                Timber.d(e, "exception decrypting Activation Token")
            }
            val message = "failed obtaining activation for key $id, primary = ${isPrimary}, signature.isNullOrBlank = ${signature.isNullOrBlank()}, token.isNullOrBlank = ${token.isNullOrBlank()}, activation.isNullOrBlank = ${activation.isNullOrBlank()}"
            val eventBuilder = EventBuilder()
                    .withTag(Constants.LogTags.SENDING_FAILED_TAG, AppUtil.getAppVersion())
                    .withTag(Constants.LogTags.SENDING_FAILED_DEVICE_TAG, Build.MODEL + " " + Build.VERSION.SDK_INT)
            Sentry.capture(eventBuilder.withMessage(message).build())
        }

        Timber.e("failed obtaining activation for key %s, primary = %s, signature.isNullOrBlank = %s, token.isNullOrBlank = %s, activation.isNullOrBlank = %s",
            this.id, this.isPrimary, this.signature.isNullOrBlank(), this.token.isNullOrBlank(), this.activation.isNullOrBlank())

    }

    return null
}
