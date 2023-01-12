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
package ch.protonmail.android.crypto

import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.UserKey
import ch.protonmail.android.utils.crypto.KeyInformation
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.crypto.TextDecryptionResult
import ch.protonmail.android.utils.crypto.TextVerificationResult
import com.proton.gopenpgp.armor.Armor
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import java.util.Arrays

class UserCrypto @AssistedInject constructor(
    userManager: UserManager,
    openPgp: OpenPGP,
    @Assisted userId: UserId
) : Crypto<UserKey>(userManager, openPgp, userId) {

    override val currentKeys: Collection<UserKey>
        get() = userKeys.keys

    override val primaryKey: UserKey?
        get() = userKeys.primaryKey

    override val primaryPassphrase: EncryptedByteArray?
        get() = userPassphrase

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    override val UserKey.privateKey: PgpField.PrivateKey
        get() = privateKey

    override fun passphraseFor(key: UserKey): EncryptedByteArray? = primaryPassphrase

    fun verify(data: String, signature: String): TextVerificationResult {
        return try {
            val verifiedTimestamp = openPgp.getVerifiedSignatureTimestamp(signature, data, getVerificationKeys(), openPgp.time)
            TextVerificationResult(data, true, verifiedTimestamp)
        } catch(signatureVerificationError: Exception) {
            TextVerificationResult(data, false, null)
        }
    }

    /**
     * Decrypt Message or Contact Data
     */
    override fun decrypt(message: CipherText): TextDecryptionResult {
        val publicKeys = getVerificationKeys()
        Timber.v("Decrypt with keys $publicKeys")
        return decrypt(message, publicKeys, openPgp.time)
    }

    fun decryptMessage(message: String): TextDecryptionResult {
        val errorMessage = "Error decrypting message, invalid passphrase"
        checkNotNull(userPassphrase) { errorMessage }

        return withCurrentKeys("Error decrypting message") { key ->
            val cipherText = CipherText(message)
            val unarmored = Armor.unarmor(key.privateKey.string)
            userPassphrase.use {
                val decrypted = openPgp.decryptMessageBinKey(cipherText.armored, unarmored, it)
                TextDecryptionResult(decrypted, false, false)
            }
        }
    }

    fun deriveKeyInfo(key: String): KeyInformation {
        return try {
            val fingerprint = openPgp.getFingerprint(key)
            val isExpired = openPgp.isKeyExpired(key)
            var privateKey = Armor.unarmor(key)
            val publicKey = openPgp.getPublicKey(key)
            val canEncrypt = openPgp.canEncrypt(key)
            privateKey = if (Arrays.equals(privateKey, publicKey)) null else privateKey
            KeyInformation(publicKey, privateKey, true, fingerprint, isExpired, canEncrypt)
        } catch (ignored: Exception) {
            KeyInformation.EMPTY()
        }
    }

    @AssistedInject.Factory
    interface AssistedFactory {
        fun create(userId: UserId): UserCrypto
    }
}
