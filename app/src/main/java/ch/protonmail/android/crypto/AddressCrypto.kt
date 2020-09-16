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
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.domain.entity.user.AddressKeys
import ch.protonmail.android.mapper.bridge.UserBridgeMapper
import ch.protonmail.android.utils.crypto.BinaryDecryptionResult
import ch.protonmail.android.utils.crypto.EOToken
import ch.protonmail.android.utils.crypto.MimeDecryptor
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.crypto.TextDecryptionResult
import ch.protonmail.libs.core.utils.encodeToBase64String
import com.proton.gopenpgp.armor.Armor
import com.proton.gopenpgp.constants.Constants
import com.proton.gopenpgp.crypto.KeyRing
import com.proton.gopenpgp.crypto.PlainMessage
import com.proton.gopenpgp.crypto.SessionKey
import com.proton.gopenpgp.crypto.Crypto as GoOpenPgpCrypto

class AddressCrypto(
    userManager: UserManager,
    openPgp: OpenPGP,
    username: Name,
    private val addressId: Id,
    userMapper: UserBridgeMapper = UserBridgeMapper.buildDefault()
) : Crypto<AddressKey>(userManager, openPgp, username, userMapper) {

    private val address get() =
        // Address here cannot be null
        user.addresses.findBy(addressId)
            ?: throw IllegalArgumentException("Cannot find an address with given id")

    private val addressKeys: AddressKeys
        get() = address.keys

    override val currentKeys: Collection<AddressKey>
        get() = addressKeys.keys

    override val passphrase: ByteArray?
        get() = passphraseFor(addressKeys.keys.first())

    protected override fun passphraseFor(key: AddressKey): ByteArray? {
        // This is for smart-cast support
        val token = key.token
        val signature = key.signature

        return if (token == null || signature == null) {
            mailboxPassword

        } else {
            // try decrypting Token with all UserKeys we have
            val errorMessage = "Failed getting passphrase for key ${key.id.s}, " +
                "primary = ${key.isPrimary}, " +
                "has activation = ${key.activation != null}"

            withCurrentKeys(errorMessage) {
                val decryptedToken = openPgp.decryptMessage(token.string, it.privateKey.string, mailboxPassword)
                val validSignature = openPgp.verifyTextSignDetached(
                    signature.string,
                    decryptedToken,
                    Armor.armorKey(openPgp.getPublicKey(it.privateKey.string)),
                    0
                )
                require(validSignature)

                decryptedToken.toByteArray(Charsets.UTF_8)
            }
        }
    }


    /**
     * Encrypt for Attachment
     */
    fun encrypt(data: ByteArray, filename: String): CipherText {
        val keyRing = createAndUnlockKeyRing()
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

    fun decryptKeyPacket(keyPacket: ByteArray): ByteArray =
        createAndUnlockKeyRing().decryptSessionKey(keyPacket).key

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

    fun decryptMime(message: CipherText): MimeDecryptor =
        MimeDecryptor(message.armored, openPgp, getUnarmoredKeys(), String(passphrase!!))

    fun generateEOToken(password: ByteArray): EOToken {
        // generate a 256 bit token.
        val randomToken = openPgp.randomToken()
        val base64token = randomToken.encodeToBase64String(Base64.NO_WRAP)
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


    private val AddressKey.isPrimary get() =
        this == addressKeys.primaryKey

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    protected override val AddressKey.privateKey: PgpField.PrivateKey
        get() = privateKey
}
