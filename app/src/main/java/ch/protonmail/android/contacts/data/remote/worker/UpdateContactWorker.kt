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

package ch.protonmail.android.contacts.data.remote.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.CreateContactV2BodyItem
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_INVALID_EMAIL
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.domain.util.requireNotEmpty
import ch.protonmail.android.events.ContactEvent
import ch.protonmail.android.utils.AppUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import javax.inject.Inject

private const val KEY_LABEL_WORKER_CONTACT_NAME = "ContactName"
private const val KEY_LABEL_WORKER_CONTACT_ID = "ContactId"
private const val KEY_LABEL_WORKER_ENCRYPTED_DATA = "EncryptedData"
private const val KEY_LABEL_WORKER_SIGNED_DATA = "SignedData"

@HiltWorker
class UpdateContactWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted val workerParams: WorkerParameters,
    private val apiManager: ProtonMailApiManager,
    private val userCrypto: UserCrypto
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val contactId = requireNotEmpty(inputData.getString(KEY_LABEL_WORKER_CONTACT_ID))
        val encryptedData = requireNotEmpty(inputData.getString(KEY_LABEL_WORKER_ENCRYPTED_DATA))
        val signedData = requireNotEmpty(inputData.getString(KEY_LABEL_WORKER_SIGNED_DATA))


        val tct = userCrypto.encrypt(encryptedData, false)
        val encryptedDataSignature = userCrypto.sign(encryptedData)
        val signedDataSignature = userCrypto.sign(signedData)
        val body = CreateContactV2BodyItem(
            signedData, signedDataSignature,
            tct.armored, encryptedDataSignature
        )
        val response = apiManager.updateContact(contactId, body)
        return if (response != null) {
            Timber.v("Update contacts response code:%s error:%s", response.code, response.error)
            when (response.code) {
                RESPONSE_CODE_ERROR_EMAIL_EXIST -> {
                    AppUtil.postEventOnUi(ContactEvent(ContactEvent.ALREADY_EXIST, true))
                    Result.failure()
                }
                RESPONSE_CODE_ERROR_INVALID_EMAIL -> {
                    AppUtil.postEventOnUi(ContactEvent(ContactEvent.INVALID_EMAIL, true))
                    Result.failure()
                }
                RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED -> {
                    AppUtil.postEventOnUi(ContactEvent(ContactEvent.DUPLICATE_EMAIL, true))
                    Result.failure()
                }
                else -> {
                    AppUtil.postEventOnUi(ContactEvent(ContactEvent.SUCCESS, true))
                    Result.success()
                }
            }
        } else {
            Result.failure()
        }

    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            contactName: String,
            contactId: String,
            encryptedData: String,
            signedData: String,
        ): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                KEY_LABEL_WORKER_CONTACT_NAME to contactName,
                KEY_LABEL_WORKER_CONTACT_ID to contactId,
                KEY_LABEL_WORKER_ENCRYPTED_DATA to encryptedData,
                KEY_LABEL_WORKER_SIGNED_DATA to signedData
            )

            val request = OneTimeWorkRequestBuilder<UpdateContactWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            return workManager.enqueue(request)
        }
    }
}
