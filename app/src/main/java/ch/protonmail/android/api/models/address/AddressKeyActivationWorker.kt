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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.domain.entity.user.Addresses
import ch.protonmail.android.domain.entity.user.UserKeys
import ch.protonmail.android.mapper.bridge.AddressBridgeMapper
import ch.protonmail.android.usecase.crypto.GenerateTokenAndSignature
import ch.protonmail.android.utils.crypto.OpenPGP
import ch.protonmail.android.utils.extensions.app
import com.proton.gopenpgp.helper.Helper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import me.proton.core.domain.arch.map
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import ch.protonmail.android.api.models.address.Address as OldAddress

internal const val KEY_INPUT_DATA_USER_ID = "KEY_INPUT_DATA_USER_ID"
private const val MAX_RETRY_COUNT = 3

/**
 * This worker handles AddressKey activation by decrypting Activation Token and updating Private Key on server.
 */
@HiltWorker
class AddressKeyActivationWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    private val userManager: UserManager,
    private val api: ProtonMailApiManager,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(context, params) {

    private val openPgp: OpenPGP by lazy { userManager.openPgp }

    override suspend fun doWork(): Result = withContext(dispatchers.Io) {

        val userIdString = inputData.getString(KEY_INPUT_DATA_USER_ID) ?: return@withContext Result.failure()
        val userId = UserId(userIdString)
        val mailboxPassword = userManager.getUserPassphrase(userId)

        Timber.v("AddressKeyActivationWorker started with user: ${userId.id}")

        val user = userManager.getUser(userId)
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
                val userIdString = inputData.getString(KEY_INPUT_DATA_USER_ID)!!
                val userId = UserId(userIdString)
                val user = userManager.getUser(userId)
                if (user.isLegacy) {
                    Timber.v("Activate key for legacy user")
                    api.activateKeyLegacy(
                        KeyActivationBody(
                            newPrivateKey,
                            signedKeyList,
                            null,
                            null
                        ),
                        addressKey.id.id
                    )
                } else {
                    Timber.v("Activate key for non-legacy user")
                    val generatedTokenAndSignature =
                        GenerateTokenAndSignature(userManager, openPgp).invoke(orgKeys?.toUserKey())
                    val keyActivationBody = KeyActivationBody(
                        newPrivateKey,
                        signedKeyList,
                        generatedTokenAndSignature.token,
                        generatedTokenAndSignature.signature
                    )
                    api.activateKey(keyActivationBody, addressKey.id.id)
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
            "Failed obtaining activation for key ${addressKey.id.id}, " +
                "primary = $isPrimary, " +
                "has signature = ${addressKey.signature != null}, " +
                "has token = ${addressKey.token != null}"
        )
    }

    sealed class ActivationResult {
        object Success : ActivationResult()
        sealed class Error : ActivationResult() {
            abstract val id: UserId

            class Network(override val id: UserId) : Error()
            class MissingToken(override val id: UserId) : Error()
            class Generic(override val id: UserId) : Error()
        }
    }

    companion object {

        @Deprecated(
            "Use new model",
            ReplaceWith(
                """
                    val mapper = AddressBridgeMapper.buildDefault()
                    runIfNeeded(workManager, addresses.map(mapper) { it.toNewModel() }, Name(username))
                """,
                "me.proton.core.domain.arch.map",
                "ch.protonmail.android.mapper.bridge.AddressBridgeMapper"
            )
        )
        fun activateAddressKeysIfNeeded(context: Context, addresses: List<OldAddress>, userId: UserId) {
            val mapper = AddressBridgeMapper.buildDefault()
            runIfNeeded(WorkManager.getInstance(context), addresses.map(mapper) { it.toNewModel() }, userId)
        }

        @Suppress("unused") // When using new User, it will be nicer to user 'user.addresses'
        fun runIfNeeded(workManager: WorkManager, addresses: Addresses, userId: UserId) {
            runIfNeeded(workManager, addresses.addresses.values, userId)
        }

        private fun runIfNeeded(
            workManager: WorkManager,
            addresses: Collection<Address>,
            userId: UserId
        ) {
            val isActivationNeeded = addresses.any { address ->
                address.keys.keys.any { it.activation != null }
            }

            if (isActivationNeeded) {
                val data = workDataOf(KEY_INPUT_DATA_USER_ID to userId.id)
                val work = OneTimeWorkRequestBuilder<AddressKeyActivationWorker>()
                    .setInputData(data)
                    .build()
                workManager.enqueue(work)
            }
        }
    }
}
