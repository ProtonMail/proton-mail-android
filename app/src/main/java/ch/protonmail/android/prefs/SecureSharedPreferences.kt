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
package ch.protonmail.android.prefs

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.preference.PreferenceManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_ID
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_NAME
import ch.protonmail.android.core.PREF_USERNAME
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.extensions.obfuscate
import ch.protonmail.android.utils.extensions.obfuscateUsername
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.android.sharedpreferences.clearAll
import me.proton.core.util.android.sharedpreferences.get
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import java.math.BigInteger
import java.security.Key
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.UnrecoverableKeyException
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import javax.security.auth.x500.X500Principal

// region constants
private const val UTF8 = "UTF-8"
private const val ALGORITHM_AES = "AES"
const val PREF_SYMMETRIC_KEY = "SEKRIT"
// endregion

class SecureSharedPreferences(
    private val delegate: SharedPreferences,
    private val sekrit: CharArray
) : SharedPreferences {

    override fun edit() = Editor()

    /**
     * @return [Map] of decrypted [String] and [String]
     *  If the content can't be decrypted, it will be served as encrypted
     */
    override fun getAll(): Map<String, *> =
        delegate.all.map { (encryptedKey, encryptedValue) ->
            val key = decrypt(encryptedKey)
            val value = decrypt(encryptedValue as String)
            Pair(
                key ?: encryptedKey,
                value ?: encryptedValue
            )
        }.toMap()

    @Synchronized
    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return try {
            delegate.getString(encryptProxyKey(key), null)
                ?.let(::decrypt)?.toBoolean()
                ?: defValue

        } catch (e: ClassCastException) {
            Timber.i(e)
            delegate.getBoolean(key, defValue)
        }
    }

    @Synchronized
    override fun getFloat(key: String, defValue: Float): Float {
        return try {
            delegate.getString(encryptProxyKey(key), null)
                ?.let(::decrypt)?.toFloatOrNull()
                ?: defValue

        } catch (e: ClassCastException) {
            Timber.i(e)
            delegate.getFloat(key, defValue)
        }
    }

    @Synchronized
    override fun getInt(key: String, defValue: Int): Int {
        return try {
            delegate.getString(encryptProxyKey(key), null)
                ?.let(::decrypt)?.toIntOrNull()
                ?: defValue

        } catch (e: ClassCastException) {
            Timber.i(e)
            delegate.getInt(key, defValue)
        }
    }

    @Synchronized
    override fun getLong(key: String, defValue: Long): Long {
        return try {
            delegate.getString(encryptProxyKey(key), null)
                ?.let(::decrypt)?.toLongOrNull()
                ?: defValue

        } catch (e: ClassCastException) {
            Timber.i(e)
            return delegate.getLong(key, defValue)
        }
    }

    @Synchronized
    override fun getString(key: String, defValue: String?): String? =
        delegate.getString(encryptProxyKey(key), null)?.let(::decrypt) ?: defValue

    @Synchronized
    fun getStringOrNull(key: String): String? =
        delegate.getString(encryptProxyKey(key), null)?.let(::decryptOrNull)

    override fun contains(key: String): Boolean =
        delegate.contains(encryptProxyKey(key))

    override fun registerOnSharedPreferenceChangeListener(
        onSharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        delegate.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
    }

    @Suppress("FunctionMaxLength") // Platform name
    override fun unregisterOnSharedPreferenceChangeListener(
        onSharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        delegate.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        throw UnsupportedOperationException("This class does not support String Sets")

    fun encrypt(value: String?): String {
        val bytes = value?.toByteArray(charset(UTF8)) ?: ByteArray(0)
        val digester = MessageDigest.getInstance("SHA-256")
        digester.update(String(sekrit).toByteArray(charset("UTF-8")))
        val key = digester.digest()
        val spec = SecretKeySpec(key, "AES")
        val pbeCipher = Cipher.getInstance(ALGORITHM_AES)
        pbeCipher.init(Cipher.ENCRYPT_MODE, spec)
        return String(Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP), charset(UTF8))
    }

    private fun decrypt(value: String?): String? {
        return try {
            decryptOrThrow(value)
        } catch (e: Exception) {
            Timber.e(e)
            value
        }
    }

    private fun decryptOrNull(value: String?): String? {
        return try {
            decryptOrThrow(value)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    private fun decryptOrThrow(value: String?): String? {
        val bytes = if (value != null) Base64.decode(value, Base64.NO_WRAP) else ByteArray(0)
        val digester = MessageDigest.getInstance("SHA-256")
        digester.update(String(sekrit).toByteArray(charset("UTF-8")))
        val key = digester.digest()
        val spec = SecretKeySpec(key, "AES")
        val pbeCipher = Cipher.getInstance(ALGORITHM_AES)
        pbeCipher.init(Cipher.DECRYPT_MODE, spec)
        return String(pbeCipher.doFinal(bytes), charset(UTF8))
    }

    fun encryptProxyKey(value: String) = encrypt(value)

    inner class Editor internal constructor() : SharedPreferences.Editor {
        private var delegate: SharedPreferences.Editor =
            this@SecureSharedPreferences.delegate.edit()

        @Synchronized
        override fun putBoolean(key: String, value: Boolean): Editor {
            delegate.putString(encryptProxyKey(key), encrypt(value.toString()))
            return this
        }

        @Synchronized
        override fun putFloat(key: String, value: Float): Editor {
            delegate.putString(encryptProxyKey(key), encrypt(value.toString()))
            return this
        }

        @Synchronized
        override fun putInt(key: String, value: Int): Editor {
            delegate.putString(encryptProxyKey(key), encrypt(value.toString()))
            return this
        }

        @Synchronized
        override fun putLong(key: String, value: Long): Editor {
            delegate.putString(encryptProxyKey(key), encrypt(value.toString()))
            return this
        }

        @Synchronized
        override fun putString(key: String, value: String?): Editor {
            delegate.putString(encryptProxyKey(key), encrypt(value))
            return this
        }

        @Synchronized
        override fun apply() {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) delegate.apply()
            else delegate.commit()
        }

        @Synchronized
        override fun clear(): Editor {
            delegate.clear()
            return this
        }

        @Synchronized
        override fun commit() = delegate.commit()

        @Synchronized
        override fun remove(s: String): Editor {
            delegate.remove(encryptProxyKey(s))
            return this
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor =
            throw UnsupportedOperationException("This class does not work with String Sets.")
    }

    /**
     * Migrate [SecureSharedPreferences] to use Users' [Id] instead of username
     *
     * ### Output:
     *  A [Map] of [String] usernames associated by its [Id]
     */
    class UsernameToIdMigration @Inject constructor(
        private val preferencesFactory: Factory,
        private val dispatchers: DispatcherProvider
    ) {

        suspend operator fun invoke(usernames: Collection<String>): Map<String, UserId> =
            withContext(dispatchers.Io) {
                usernames.mapNotNull(::migrateForUser).toMap()
            }

        private fun migrateForUser(username: String): Pair<String, UserId>? {
            Timber.v("Migrating SecureSharedPreferences for ${username.obfuscateUsername()}")

            @Suppress("DEPRECATION")
            val oldPrefs = preferencesFactory._usernamePreferences(username) as SecureSharedPreferences
            val userId = oldPrefs.getStringOrNull(PREF_USER_ID)?.let(::UserId)

            return if (userId != null) {
                val newPrefs = preferencesFactory.userPreferences(userId)
                for ((key, value) in oldPrefs.all) {
                    newPrefs[key] = value
                }
                newPrefs[PREF_USER_NAME] = username
                newPrefs[PREF_USERNAME] = username
                username to userId

            } else {
                Timber.e("Cannot get user Id for ${username.obfuscate()}")
                null

            }.also { oldPrefs.clearAll() }
        }

    }

    @Singleton
    class Factory @Inject constructor(
        private val context: Context,
        @DefaultSharedPreferences private val defaultSharedPreferences: SharedPreferences
    ) {

        private val keyStore = KeyStore.getInstance(keyStoreName).also { it.load(null) }
        private val sekrit by lazy { generateSekrit() }

        fun appPreferences(): SharedPreferences = SecureSharedPreferences(
            delegate = context.getSharedPreferences("ProtonMailSSP", Context.MODE_PRIVATE),
            sekrit = sekrit
        )

        @Suppress("FunctionName")
        @Deprecated("This should not be used! needed only for migration!")
        fun _usernamePreferences(
            username: String
        ): SharedPreferences {
            val name = "${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-SSP"
            return SecureSharedPreferences(
                delegate = context.getSharedPreferences(name, Context.MODE_PRIVATE),
                sekrit = sekrit
            )
        }

        fun userPreferences(userId: UserId): SharedPreferences =
            SecureSharedPreferences(
                delegate = context.getSharedPreferences(userId.id, Context.MODE_PRIVATE),
                sekrit = sekrit
            )

        private fun generateSekrit(): CharArray {
            val keyPair = retrieveAsymmetricKeyPair(asymmetricKeyAlias)
                ?: generateKeyPair(asymmetricKeyAlias)

            val storedSymmetricKey = defaultSharedPreferences.get<String>(PREF_SYMMETRIC_KEY).let { key ->
                if (key == null) {
                    AppUtil.deletePrefs()
                    null
                } else {
                    decryptAsymmetric(key, keyPair.private)
                }
            }

            val sekritString = storedSymmetricKey
                ?: UUID.randomUUID().toString().also {
                    defaultSharedPreferences[PREF_SYMMETRIC_KEY] = encryptAsymmetric(it, keyPair.public)
                }


            return sekritString.toCharArray()
        }

        private fun generateKeyPair(alias: String): KeyPair {

            // workaround for BouncyCastle crashing when parsing Date in RTL languages
            // we set locale temporarily to US and then go back
            val defaultLocale = Locale.getDefault()
            setLocale(Locale.US)

            val start = GregorianCalendar()
            val end = GregorianCalendar()
            end.add(Calendar.YEAR, 5)

            val keyPairGenerator = KeyPairGenerator.getInstance("RSA", keyStoreName)

            // The KeyPairGeneratorSpec object is how parameters for your key pair are passed
            // to the KeyPairGenerator.
            val algorithmParameterSpec = KeyGenParameterSpec
                .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setCertificateSubject(X500Principal("CN=ProtonMail, O=Android Authority"))
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .setCertificateSerialNumber(BigInteger.ONE)
                .setCertificateNotBefore(start.time)
                .setCertificateNotAfter(end.time)
                .build()

            keyPairGenerator.initialize(algorithmParameterSpec)
            val keyPair = keyPairGenerator.generateKeyPair()

            setLocale(defaultLocale)

            return keyPair
        }

        private fun retrieveAsymmetricKeyPair(alias: String): KeyPair? {
            val privateKey = try {
                keyStore.getKey(alias, null) as PrivateKey?
            } catch (e: UnrecoverableKeyException) {
                Timber.i(e)
                null
            }
            val publicKey = keyStore.getCertificate(alias)?.publicKey

            return if (privateKey != null && publicKey != null) {
                KeyPair(publicKey, privateKey)
            } else {
                null
            }
        }

        private fun encryptAsymmetric(plainText: String, key: Key): String {
            val asymmetricCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            asymmetricCipher.init(Cipher.ENCRYPT_MODE, key)
            val bytes = asymmetricCipher.doFinal(plainText.toByteArray())
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        /**
         * @return null when decryption key couldn't decrypt value
         */
        private fun decryptAsymmetric(cipherText: String, key: Key): String? {
            if (cipherText.isBlank()) {
                return ""
            }
            val asymmetricCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            asymmetricCipher.init(Cipher.DECRYPT_MODE, key)
            val encryptedData = Base64.decode(cipherText, Base64.NO_WRAP)
            return try {
                String(asymmetricCipher.doFinal(encryptedData))
            } catch (e: Exception) {
                Timber.i(e)
                null
            }
        }

        private fun setLocale(locale: Locale) {
            Locale.setDefault(locale)
            val resources = context.resources
            val configuration = Configuration(resources.configuration)
            configuration.setLocale(locale)
            context.createConfigurationContext(configuration)
        }
    }

    companion object {

        private const val keyStoreName = "AndroidKeyStore"
        private const val asymmetricKeyAlias = "ProtonMailKey"

        @Deprecated(
            "Use SecureSharedPreferences.Factory",
            ReplaceWith("secureSharedPreferencesFactory.userPreferences(userId)")
        )
        @Synchronized
        fun getPrefsForUser(context: Context, userId: UserId): SharedPreferences =
            Factory(
                context.applicationContext,
                PreferenceManager.getDefaultSharedPreferences(context)
            ).userPreferences(userId)
    }
}
