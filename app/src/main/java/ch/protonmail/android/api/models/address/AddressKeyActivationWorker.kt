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
package ch.protonmail.android.api.models.address

import android.content.Context
import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.getActivationToken
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.domain.entity.user.Addresses
import ch.protonmail.android.domain.entity.user.UserKeys
import ch.protonmail.android.mapper.bridge.AddressBridgeMapper
import ch.protonmail.android.utils.crypto.OpenPGP
import com.proton.gopenpgp.helper.Helper
import me.proton.core.domain.arch.map
import timber.log.Timber
import javax.inject.Inject
import ch.protonmail.android.api.models.address.Address as OldAddress

// region constants
private const val KEY_INPUT_DATA_USERNAME = "KEY_INPUT_DATA_USERNAME"
// endregion

/**
 * This worker handles AddressKey activation by decrypting Activation Token and updating Private Key on server.
 */
class AddressKeyActivationWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userManager: UserManager,
    private val api: ProtonMailApiManager
) : Worker(context, params) {

    private val openPgp: OpenPGP by lazy { userManager.openPgp }

    override fun doWork(): Result {

        val username = inputData.getString(KEY_INPUT_DATA_USERNAME) ?: return Result.failure()
        val mailboxPassword = userManager.getMailboxPassword(username) ?: return Result.failure()

        val user = userManager.getUser(username).toNewUser()
        val primaryKeys = user.addresses.addresses.values.map { it.keys.primaryKey }
        val keysToActivate = user.addresses.addresses.values.flatMap { address ->
            address.keys.keys.filter { it.activation != null }
        }

        val errors = keysToActivate.map {
            activateKey(it, user.keys, mailboxPassword, it in primaryKeys)
        }.filterIsInstance<ActivationResult.Error>()
        val failedCountMessage = "impossible to activate ${errors.size} keys out of ${keysToActivate.size}"

        return when {
            errors.isEmpty() -> Result.success()
            errors.any { it is ActivationResult.Error.Network } -> {
                Timber.w("Network error: $failedCountMessage")
                Result.retry()
            }
            else -> {
                Timber.e("Error: $failedCountMessage")
                Result.failure()
            }
        }
    }

    private fun activateKey(
        addressKey: AddressKey,
        userKeys: UserKeys,
        mailboxPassword: ByteArray,
        isPrimary: Boolean
    ): ActivationResult {
        return try {
            val activationToken = try {
                getActivationToken(addressKey, openPgp, userKeys, mailboxPassword, isPrimary)
            } catch (ignored: Exception) {
                return ActivationResult.Error.MissingToken(addressKey.id)
            }

            val privateKeyString = addressKey.privateKey.string
            val newPrivateKey: String = openPgp.updatePrivateKeyPassphrase(
                privateKeyString,
                activationToken.toByteArray(),
                mailboxPassword
            )
            val keyFingerprint = openPgp.getFingerprint(privateKeyString)
            val keyList = """
                [{
                    "Fingerprint": "$keyFingerprint",
                    "SHA256Fingerprints": "${String(Helper.getJsonSHA256Fingerprints(privateKeyString))}",
                    "Primary": 1, 
                    "Flags": 3
                }]
                """.trimMargin()

            val signature = openPgp.signTextDetached(keyList, newPrivateKey, mailboxPassword)
            val signedKeyList = SignedKeyList(keyList, signature)

            try {
                api.activateKey(KeyActivationBody(newPrivateKey, signedKeyList), addressKey.id.s)
                ActivationResult.Success
            } catch (ignored: Exception) {
                ActivationResult.Error.Network(addressKey.id)
            }

        } catch (ignored: Exception) {
            ActivationResult.Error.Generic(addressKey.id)
        }
    }

    private fun getActivationToken(
        addressKey: AddressKey,
        openPgp: OpenPGP,
        userKeys: UserKeys,
        userKeysPassphrase: ByteArray,
        isPrimary: Boolean
    ): String {
        // This is for smart-cast support
        val activation = addressKey.activation
        requireNotNull(activation)

        // try decrypting Activation Token with all UserKeys we have
        userKeys.keys.forEach {
            addressKey.runCatching {
                return openPgp.decryptMessage(activation.string, it.privateKey.string, userKeysPassphrase)
            }
        }

        throw IllegalStateException("Failed obtaining activation for key ${addressKey.id.s}, " +
            "primary = $isPrimary, " +
            "has signature = ${addressKey.signature != null}, " +
            "has token = ${addressKey.token != null}")
    }

    sealed class ActivationResult {
        object Success : ActivationResult()
        sealed class Error : ActivationResult() {
            abstract val id: Id

            class Network(override val id: Id) : Error()
            class MissingToken(override val id: Id) : Error()
            class Generic(override val id: Id) : Error()
        }
    }

    companion object {

        @Deprecated(
            "Use new model",
            ReplaceWith(
                """
                    val mapper = AddressBridgeMapper.buildDefault()
                    runIfNeeded(context, addresses.map(mapper) { it.toNewModel() }, Name(username))
                """,
                "me.proton.core.domain.arch.map",
                "ch.protonmail.android.mapper.bridge.AddressBridgeMapper"
            )
        )
        fun activateAddressKeysIfNeeded(context: Context, addresses: List<OldAddress>, username: String) {
            val mapper = AddressBridgeMapper.buildDefault()
            runIfNeeded(context, addresses.map(mapper) { it.toNewModel() }, Name(username))
        }

        @Suppress("unused") // When using new User, it will be nicer to user 'user.addresses'
        fun runIfNeeded(context: Context, addresses: Addresses, username: Name) {
            runIfNeeded(context, addresses.addresses.values, username)
        }

        fun runIfNeeded(context: Context, addresses: Collection<Address>, username: Name) {
            val isActivationNeeded = addresses.any { address ->
                address.keys.keys.any { it.activation != null }
            }
            if (isActivationNeeded) {
                val data = workDataOf(KEY_INPUT_DATA_USERNAME to username.s)
                val work = OneTimeWorkRequestBuilder<AddressKeyActivationWorker>()
                    .setInputData(data)
                    .build()
                WorkManager.getInstance(context).enqueue(work)
            }
        }
    }
}
