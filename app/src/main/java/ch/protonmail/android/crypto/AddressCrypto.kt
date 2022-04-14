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
package ch.protonmail.android.crypto

import android.util.Base64
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.domain.entity.user.AddressKeys
import ch.protonmail.android.utils.crypto.BinaryDecryptionResult
import ch.protonmail.android.utils.crypto.EOToken
import ch.protonmail.android.utils.crypto.MimeDecryptor
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.crypto.TextDecryptionResult
import com.proton.gopenpgp.armor.Armor
import com.proton.gopenpgp.constants.Constants
import com.proton.gopenpgp.crypto.KeyRing
import com.proton.gopenpgp.crypto.PGPMessage
import com.proton.gopenpgp.crypto.PGPSignature
import com.proton.gopenpgp.crypto.PlainMessage
import com.proton.gopenpgp.crypto.SessionKey
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import timber.log.Timber
import javax.mail.internet.InternetHeaders
import com.proton.gopenpgp.crypto.Crypto as GoOpenPgpCrypto

const val EXPECTED_TOKEN_LENGTH = 64
const val HEX_DIGITS = "0123456789abcdefABCDEF"

class AddressCrypto @AssistedInject constructor(
    val userManager: UserManager,
    openPgp: OpenPGP,
    @Assisted userId: UserId,
    @Assisted private val addressId: AddressId
) : Crypto<AddressKey>(userManager, openPgp, userId) {

    @AssistedInject.Factory
    interface Factory {
        fun create(userId: UserId, addressId: AddressId): AddressCrypto
    }

    private val address
        get() =
            // Address here cannot be null
            user.addresses.findBy(addressId)
                ?: throw IllegalArgumentException("Cannot find an address with given id")

    private val addressKeys: AddressKeys
        get() = address.keys

    override val currentKeys: Collection<AddressKey>
        get() = addressKeys.keys

    override val primaryKey: AddressKey?
        get() = addressKeys.primaryKey

    override val passphrase: ByteArray?
        get() = passphraseFor(requirePrimaryKey())

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    protected override val AddressKey.privateKey: PgpField.PrivateKey
        get() = privateKey

    override fun passphraseFor(key: AddressKey): ByteArray? {
        // This is for smart-cast support
        val token = key.token
        val signature = key.signature

        if (token == null || signature == null) {
            return mailboxPassword
        }

        val pgpMessage = GoOpenPgpCrypto.newPGPMessageFromArmored(token.string)

        userManager.currentUser?.keys?.keys?.forEach { userKey ->
            // We need to catch exceptions because the operation might throw if the user has inactive keys
            runCatching {
                GoOpenPgpCrypto.newKeyRing(
                    GoOpenPgpCrypto.newKeyFromArmored(userKey.privateKey.string).unlock(mailboxPassword)
                )
            }.onSuccess { userKeyRing ->
                decryptToken(
                    pgpMessage,
                    userKeyRing
                )?.let { decryptedToken ->
                    if (!userKey.active) {
                        Timber.w("Key used to decrypt token is inactive")
                    }
                    if (
                        verifyTokenFormat(decryptedToken) &&
                        verifySignature(userKeyRing, decryptedToken, signature, userKey.id.id, key.id.id)
                    ) {
                        return decryptedToken
                    }
                }
            }
        }
        Timber.e("Failed getting passphrase for key (id = ${key.id.id}) using user keys")
        return null
    }

    /**
     * Check that the key token is a 32 byte value encoded in hexadecimal form.
     */
    private fun verifyTokenFormat(decryptedToken: ByteArray): Boolean {
        if (decryptedToken.size != EXPECTED_TOKEN_LENGTH) {
            return false
        }
        for (char in decryptedToken) {
            if (!HEX_DIGITS.contains(char.toInt().toChar())) {
                return false
            }
        }
        return true
    }

    private fun decryptToken(
        token: PGPMessage,
        userKeyRing: KeyRing
    ): ByteArray? = runCatching {
        userKeyRing.decrypt(token, null, 0).data
    }.getOrNull()

    private fun verifySignature(
        userKeyRing: KeyRing,
        decryptedToken: ByteArray,
        signature: PgpField.Signature,
        userKeyId: String,
        addressKeyId: String
    ) = runCatching {
        userKeyRing.verifyDetached(
            PlainMessage(decryptedToken),
            PGPSignature(signature.string),
            GoOpenPgpCrypto.getUnixTime()
        )
    }.fold(
        onSuccess = { true },
        onFailure = {
            Timber.e(
                it,
                "Verification of token for address key (id = $addressKeyId) " +
                    "with user key (id = $userKeyId) failed"
            )
            false
        }
    )

    /**
     * Encrypt for Attachment
     */
    fun encryptWithPrimary(data: ByteArray, filename: String): CipherText {
        val keyRing = createAndUnlockPrimaryKeyRing().getOrThrow()
        val pgpSplitMessage = keyRing.encryptAttachment(PlainMessage(data), filename)
        keyRing.clearPrivateParams()
        return CipherText(pgpSplitMessage.keyPacket, pgpSplitMessage.dataPacket)
    }

    fun encryptKeyPacket(sessionKey: ByteArray, publicKey: String): ByteArray {
        val keyRing = openPgp.buildKeyRing(Armor.unarmor(publicKey))
        val symmetricKey = SessionKey(sessionKey, Constants.AES256)
        return keyRing.encryptSessionKey(symmetricKey)
    }

    fun encryptKeyPacketWithPassword(sessionKey: ByteArray, password: ByteArray): ByteArray {
        val symmetricKey = SessionKey(sessionKey, Constants.AES256)
        return GoOpenPgpCrypto.encryptSessionKeyWithPassword(symmetricKey, password)
    }

    fun decryptKeyPacket(keyPacket: ByteArray): ByteArray {
        val unlockedKeyRing = createAndUnlockKeyRing()
        val key = unlockedKeyRing.decryptSessionKey(keyPacket).key
        unlockedKeyRing.clearPrivateParams()
        return key
    }

    fun decryptAttachment(message: CipherText): BinaryDecryptionResult {
        return withCurrentKeys("Error decrypting attachment") { key ->
            val data = openPgp.decryptAttachmentBinKey(
                message.keyPacket,
                message.dataPacket,
                listOf(Armor.unarmor(key.privateKey.string)),
                passphraseFor(key)
            )
            BinaryDecryptionResult(data, false, false)
        }
    }

    fun decryptAttachment(keyPacket: ByteArray, dataPacket: ByteArray): BinaryDecryptionResult =
        decryptAttachment(CipherText(keyPacket, dataPacket))

    /**
     * Decrypt Message or Contact Data
     */
    override fun decrypt(message: CipherText): TextDecryptionResult {
        return withCurrentKeys("Error decrypting message") { key ->
            val unarmor = Armor.unarmor(key.privateKey.string)
            val keyPassphrase = passphraseFor(key)
            val decryptMessageBinKey =
                openPgp.decryptMessageBinKey(message.armored, unarmor, keyPassphrase)
            TextDecryptionResult(decryptMessageBinKey, false, false)
        }
    }

    fun decryptMime(
        message: CipherText,
        onBody: (String, String) -> Unit,
        onError: (Exception) -> Unit,
        onVerified: (Boolean, Boolean) -> Unit,
        onAttachment: (InternetHeaders, ByteArray) -> Unit,
        keys: List<ByteArray>?,
        time: Long
    ) {
        val keyRing = createAndUnlockKeyRing()
        with(MimeDecryptor(message.armored, openPgp, keyRing)) {
            this.onBody = onBody
            this.onError = onError
            this.onAttachment = onAttachment
            if (keys != null && keys.isNotEmpty()) {
                for (key in keys) {
                    withVerificationKey(key)
                }
                this.onVerified = onVerified
            }
            withMessageTime(time)

            start()
            await()
        }
    }

    fun generateEOToken(password: ByteArray): EOToken {
        // generate a 256 bit token.
        val randomToken = openPgp.randomToken()
        val base64token = Base64.encodeToString(randomToken, Base64.NO_WRAP)
        val encToken = openPgp.encryptMessageWithPassword(base64token, password)
        return EOToken(base64token, encToken)
    }

    fun getFingerprint(key: String): String =
        openPgp.getFingerprint(key)

    fun getSessionKey(keyPacket: ByteArray): SessionKey {
        return withCurrentKeys("Error getting Session") { key ->
            val unarmored = Armor.unarmor(key.privateKey.string)
            openPgp.getSessionFromKeyPacketBinkeys(keyPacket, unarmored, passphraseFor(key))
        }
    }

    private fun createAndUnlockKeyRing(): KeyRing {
        checkNotNull(mailboxPassword) { "Error creating KeyRing, invalid passphrase" }

        val addressKeyRing = GoOpenPgpCrypto.newKeyRing(null)
        var unlockedAtLeastOnce = false
        val errors = mutableSetOf<Throwable>()

        // try to unlock as many keys as possible, using their respective passphrases
        for (key in currentKeys) {
            try {
                val addressKeyPassphrase = passphraseFor(key)
                val lockedAddressKey = GoOpenPgpCrypto.newKeyFromArmored(key.privateKey.string)
                addressKeyRing.addKey(lockedAddressKey.unlock(addressKeyPassphrase))
                unlockedAtLeastOnce = true
            } catch (ignored: Exception) {
                // This exception says only that one of possibly many keys was incorrect
                errors += ignored
            }
        }

        if (unlockedAtLeastOnce)
            return addressKeyRing

        val errorMessage = "Could not unlock Address KeyRing"
        throw when (errors.size) {
            0 -> IllegalStateException(errorMessage)
            1 -> IllegalStateException(errorMessage, errors.first())
            else -> IllegalStateException("$errorMessage. Caused by ${errors.joinToString { it.message!! }}")
        }
    }

    private fun createAndUnlockPrimaryKeyRing(): Result<KeyRing> = runCatching {
        val primaryAddressKey = currentKeys.first()
        val addressKeyPassphrase = checkNotNull(
            passphraseFor(primaryAddressKey)
        ) { "Could not get the address key passphrase" }
        val lockedAddressKey = GoOpenPgpCrypto.newKeyFromArmored(primaryAddressKey.privateKey.string)
        val unlockedAddressKey = lockedAddressKey.unlock(addressKeyPassphrase)
        GoOpenPgpCrypto.newKeyRing(unlockedAddressKey)
    }
}
