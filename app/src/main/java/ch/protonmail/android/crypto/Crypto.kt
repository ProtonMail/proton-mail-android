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

import androidx.annotation.VisibleForTesting
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.domain.entity.user.AddressKeys
import ch.protonmail.android.domain.entity.user.UserKey
import ch.protonmail.android.domain.entity.user.UserKeys
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.crypto.TextDecryptionResult
import com.proton.gopenpgp.armor.Armor
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.keystore.decrypt
import me.proton.core.crypto.common.keystore.encrypt
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import com.proton.gopenpgp.crypto.Crypto as GoOpenPgpCrypto

/**
 * @param <KC> Type of the Container.
 * @see UserKeys
 * @see AddressKeys
 *
 * @param <K> Type of the Key.
 * @see UserKey
 * @see AddressKey
 */
abstract class Crypto<K>(
    private val userManager: UserManager,
    protected val openPgp: OpenPGP,
    private val userId: UserId
) {

    protected val user by lazy {
        userManager.getUserBlocking(userId)
    }

    protected val userKeys
        get() = user.keys

    protected abstract val currentKeys: Collection<K>

    protected abstract val primaryKey: K?

    protected val userPassphrase: EncryptedByteArray?
        get() = userManager.getUserPassphraseBlocking(userId)

    /**
     * Return passphrase for decryption
     */
    protected abstract val primaryPassphrase: EncryptedByteArray?

    /**
     * @return Non null [K]
     * @throws IllegalStateException if [primaryKey] is actually `null`
     */
    protected fun requirePrimaryKey(): K =
        checkNotNull(primaryKey) { "No primary key found" }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    abstract fun passphraseFor(key: K): EncryptedByteArray?

    protected abstract val K.privateKey: PgpField.PrivateKey

    fun sign(data: ByteArray): String = primaryPassphrase.use {
        openPgp.signBinDetached(data, requirePrimaryKey().privateKey.string, it)
    }

    fun sign(data: String): String = primaryPassphrase.use {
        openPgp.signTextDetached(data, requirePrimaryKey().privateKey.string, it)
    }

    /**
     * Encrypt for Message or Contact
     */
    fun encrypt(text: String, sign: Boolean): CipherText {
        val publicKey = buildArmoredPublicKey(requirePrimaryKey().privateKey)
        val privateKey = requirePrimaryKey().takeIf { sign }?.privateKey?.string
        return passphraseFor(requirePrimaryKey()).use {
            CipherText(openPgp.encryptMessage(text, publicKey, privateKey, it, true))
        }
    }

    /**
     * Decrypt Message or Contact Data
     */
    abstract fun decrypt(message: CipherText): TextDecryptionResult

    /**
     * Decrypt Message or Contact Data
     */
    fun decrypt(message: CipherText, publicKeys: List<ByteArray>, time: Long): TextDecryptionResult {
        return withCurrentKeys("Error decrypting message") { key ->
            val privateKeys = listOf(Armor.unarmor(key.privateKey.string))
            passphraseFor(key).use {
                openPgp.decryptMessageVerifyBinKeyPrivbinkeys(message.armored, publicKeys, privateKeys, it, time)
            }
        }
    }

    fun getVerificationKeys(): List<ByteArray> = currentKeys
        .map { GoOpenPgpCrypto.newKeyFromArmored(it.privateKey.string).publicKey }

    fun buildArmoredPublicKeyOrNull(key: PgpField.PrivateKey): String? =
        runCatching { buildArmoredPublicKey(key) }
            .onFailure { Timber.e(it) }
            .getOrNull()

    fun buildArmoredPublicKey(key: PgpField.PrivateKey): String {
        val newKey = GoOpenPgpCrypto.newKeyFromArmored(key.string)
        return newKey.armoredPublicKey
            .also { newKey.clearPrivateParams() }
    }

    fun getUnarmoredKeys(): List<ByteArray> =
        currentKeys.map { Armor.unarmor(it.privateKey.string) }

    /**
     * Try to run [block] for every [K] in [currentKeys]
     * @return result of the first succeed [block]
     * @throws IllegalStateException if [block] fails for each key
     */
    protected inline fun <V> withCurrentKeys(errorMessage: String? = null, block: (K) -> V): V {
        for (key in currentKeys) {
            runCatching {
                return block(key)
            }.onFailure(Timber::d)
        }
        val messagePrefix = errorMessage?.let { "$it. " } ?: EMPTY_STRING
        throw IllegalStateException(
            "${messagePrefix}There is no valid decryption key, currentKeys size: ${currentKeys.size}"
        )
    }

    /**
     * Decrypt the [EncryptedByteArray], executes the given block function on decrypted [PlainByteArray]
     * and then clear it whether an exception is thrown or not.
     */
    protected fun <V> EncryptedByteArray?.use(block: (ByteArray?) -> V): V =
        if (this == null) block.invoke(null)
        else decrypt(userManager.keyStoreCrypto).use { decrypted -> block.invoke(decrypted.array) }

    /**
     * Encrypt the [ByteArray] and then clear it whether an exception is thrown or not.
     */
    protected fun ByteArray.encrypt(): EncryptedByteArray =
        PlainByteArray(this).use { it.encrypt(userManager.keyStoreCrypto) }

    companion object {

        @JvmStatic
        @Deprecated(
            "Please use injected UserCrypto instead",
            ReplaceWith(
                "userCrypto",
                "ch.protonmail.android.crypto.UserCrypto"
            )
        )
        fun forUser(userManager: UserManager, userId: UserId): UserCrypto =
            UserCrypto(userManager, userManager.openPgp, userId)

        @JvmStatic
        fun forAddress(userManager: UserManager, userId: UserId, addressId: AddressId): AddressCrypto =
            AddressCrypto(userManager, userManager.openPgp, userId, addressId)
    }
}
