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
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.getActivationToken
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.Logger
import ch.protonmail.android.utils.crypto.OpenPGP
import com.proton.gopenpgp.helper.Helper
import javax.inject.Inject

// region constants
private const val KEY_INPUT_DATA_USERNAME = "KEY_INPUT_DATA_USERNAME"
// endregion

/**
 * This worker handles AddressKey activation by decrypting Activation Token and updating Private Key on server.
 */

class AddressKeyActivationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    @Inject
    internal lateinit var userManager: UserManager

    @Inject
    internal lateinit var api: ProtonMailApiManager

    private val openPgp: OpenPGP by lazy { userManager.openPgp }

    init {
        (applicationContext as ProtonMailApplication).appComponent.inject(this)
    }

    override fun doWork(): Result {

        val username = inputData.getString(KEY_INPUT_DATA_USERNAME) ?: return Result.failure()
        val mailboxPassword = userManager.getMailboxPassword(username) ?: return Result.failure()

        val keysToActivate = userManager.getUser(username).addresses.flatMap { address ->
            address.keys.filter { it.activation != null }
        }

        var retryNetworkErrors = false

        keysToActivate.forEach { key ->

            val activationToken = key.getActivationToken(openPgp, userManager.getUser(username).keys, mailboxPassword)

            if (activationToken != null) {

                try {

                    val newPrivateKey: String = openPgp.updatePrivateKeyPassphrase(key.privateKey, activationToken.toByteArray() /*TODO passphrase*/, mailboxPassword)
                    val keyFingerprint = openPgp.getFingerprint(key.privateKey)
                    val keyList = "[{\"Fingerprint\": \"" + keyFingerprint + "\", " +
                            "\"SHA256Fingerprints\": " + String(Helper.getJsonSHA256Fingerprints(key.privateKey)) + ", " +
                            "\"Primary\": 1, \"Flags\": 3}]" // one-element JSON list

                    val signature = openPgp.signTextDetached(keyList, newPrivateKey, mailboxPassword)
                    val signedKeyList = SignedKeyList(keyList, signature)

                    val response = try {
                        api.activateKey(KeyActivationBody(newPrivateKey, signedKeyList), key.id)
                    } catch (e: Exception) {
                        retryNetworkErrors = true
                        null
                    }

                    // don't force update locally stored keys, they will be updated when we get "keys event"

                    Log.d("PMTAG", "AddressKey activation response: ${response?.code}")

                } catch (e: Exception) {
                    Logger.doLogException(e)
                    Logger.doLog("network error activating Address Key ${key.id}, will retry")
                }

            } else {
                Logger.doLog("can't activate Address Key ${key.id}, error decrypting Activation Token")
            }

        }

        return if (retryNetworkErrors) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {

        fun activateAddressKeysIfNeeded(context: Context, addresses: List<Address>, username: String) {

            val isActivationNeeded = addresses.any { address ->
                address.keys.any { it.activation != null }
            }

            if (isActivationNeeded) {
                val data = Data.Builder().putString(KEY_INPUT_DATA_USERNAME, username).build()
                val work = OneTimeWorkRequest.Builder(AddressKeyActivationWorker::class.java).setInputData(data).build()
                WorkManager.getInstance(context).enqueue(work)
            }
        }

    }

}
