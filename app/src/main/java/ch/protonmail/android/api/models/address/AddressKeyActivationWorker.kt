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
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.domain.entity.user.Addresses
import ch.protonmail.android.domain.entity.user.UserKeys
import ch.protonmail.android.mapper.bridge.AddressBridgeMapper
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.extensions.app
import com.proton.gopenpgp.helper.Helper
import kotlinx.coroutines.withContext
import me.proton.core.domain.arch.map
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import ch.protonmail.android.api.models.address.Address as OldAddress

private const val KEY_INPUT_DATA_USERNAME = "KEY_INPUT_DATA_USERNAME"
private const val MAX_RETRY_COUNT = 3

/**
 * This worker handles AddressKey activation by decrypting Activation Token and updating Private Key on server.
 */
class AddressKeyActivationWorker @WorkerInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    private val userManager: UserManager,
    private val api: ProtonMailApiManager,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(context, params) {

    private val openPgp: OpenPGP by lazy { userManager.openPgp }

    override suspend fun doWork(): Result = withContext(dispatchers.Io) {

        val username = inputData.getString(KEY_INPUT_DATA_USERNAME) ?: return@withContext Result.failure()
        val mailboxPassword = userManager.getMailboxPassword(username) ?: return@withContext Result.failure()

        val user = userManager.getUser(username).toNewUser()
        val primaryKeys = user.addresses.addresses.values.map { it.keys.primaryKey }
        val keysToActivate = user.addresses.addresses.values.flatMap { address ->
            address.keys.keys.filter { it.activation != null }
        }

        // get orgKeys if existent -> This will work only if user is an organisation owner, otherwise
        // backend will return 403 for all other users (e.g. just members of an organisation)
        val orgKeys = if (context.app.organization != null) {
            api.fetchOrganizationKeys()
        } else {
            null
        }

        val errors = keysToActivate.map {
            activateKey(it, user.keys, mailboxPassword, orgKeys, it in primaryKeys)
        }.filterIsInstance<ActivationResult.Error>()
        val failedCountMessage = "impossible to activate ${errors.size} keys out of ${keysToActivate.size}"

        return@withContext when {
            errors.isEmpty() -> Result.success()
            errors.any { it is ActivationResult.Error.Network } -> {
                Timber.w("Network error: $failedCountMessage, runAttemptCount: $runAttemptCount")
                if (runAttemptCount < MAX_RETRY_COUNT) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
            else -> {
                Timber.e("Error: $failedCountMessage")
                Result.failure()
            }
        }
    }

    private suspend fun activateKey(
        addressKey: AddressKey,
        userKeys: UserKeys,
        mailboxPassword: ByteArray,
        orgKeys: Keys?,
        isPrimary: Boolean
    ): ActivationResult {
        return try {
            val activationToken = try {
                getActivationToken(addressKey, openPgp, userKeys, mailboxPassword, isPrimary)
            } catch (exception: Exception) {
                Timber.i(exception, "getActivationToken has failed")
                return ActivationResult.Error.MissingToken(addressKey.id)
            }

            val privateKeyString = addressKey.privateKey.string
            val newPrivateKey: String = openPgp.updatePrivateKeyPassphrase(
                privateKeyString,
                activationToken.toByteArray(),
                mailboxPassword
            )
            val keyFingerprint = openPgp.getFingerprint(privateKeyString)

            // please keep this format as it is, as server is very sensitive about that
            val keyList = "[{\"Fingerprint\": \"" + keyFingerprint + "\", " +
                "\"SHA256Fingerprints\": " + String(Helper.getJsonSHA256Fingerprints(privateKeyString)) + ", " +
                "\"Primary\": 1, \"Flags\": 3}]"

            val signature = openPgp.signTextDetached(keyList, newPrivateKey, mailboxPassword)
            val signedKeyList = SignedKeyList(keyList, signature)

            try {
                val username = inputData.getString(KEY_INPUT_DATA_USERNAME)!!
                val user = userManager.getUser(username)
                if (user.legacyAccount) {
                    Timber.v("Activate key for legacy user")
                    api.activateKeyLegacy(
                        KeyActivationBody(
                            newPrivateKey,
                            signedKeyList,
                            null,
                            null
                        ),
                        addressKey.id.s
                    )
                }
                ActivationResult.Success
            } catch (exception: Exception) {
                Timber.i(exception, "activateKey Network error")
                ActivationResult.Error.Network(addressKey.id)
            }

        } catch (exception: Exception) {
            Timber.i(exception, "activateKey Generic error")
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
        val activation = requireNotNull(addressKey.activation)

        // try decrypting Activation Token with all UserKeys we have
        userKeys.keys.forEach {
            addressKey.runCatching {
                return openPgp.decryptMessage(activation.string, it.privateKey.string, userKeysPassphrase)
            }.onFailure {
                Timber.i(it, "decryptMessage has failed")
            }
        }

        throw IllegalStateException(
            "Failed obtaining activation for key ${addressKey.id.s}, " +
                "primary = $isPrimary, " +
                "has signature = ${addressKey.signature != null}, " +
                "has token = ${addressKey.token != null}"
        )
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

        private fun runIfNeeded(context: Context, addresses: Collection<Address>, username: Name) {
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
