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

package ch.protonmail.android.worker

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.core.Constants.RESPONSE_CODE_OK
import ch.protonmail.android.core.UserManager
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

/**
 * A `Worker` that handles fetching user info.
 *
 * @author Stefanija Boshkovska
 */

// region constants
const val FETCH_USER_INFO_WORKER_NAME = "FetchUserInfoWorker"
const val FETCH_USER_INFO_WORKER_RESULT = "FetchUserInfoWorkerResult"
const val FETCH_USER_INFO_WORKER_EXCEPTION_MESSAGE = "FetchUserInfoWorkerExceptionMessage"
// endregion

class FetchUserInfoWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val protonMailApiManager: ProtonMailApiManager,
    private val userManager: UserManager,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        return runCatching {
            withContext(dispatchers.Io) {
                val userInfoResponse = async {
                    protonMailApiManager.fetchUserInfo()
                }
                val addressesResponse = async {
                    protonMailApiManager.fetchAddresses()
                }
                userInfoResponse.await() to addressesResponse.await()
            }
        }.map { (userInfoResponse, addressesResponse) ->
            if (userInfoResponse.code == RESPONSE_CODE_OK && addressesResponse.code == RESPONSE_CODE_OK) {
                val user = userInfoResponse.user
                val addresses = addressesResponse.addresses
                user.username = user.name
                user.setAddresses(addresses)
                userManager.user = user
            }
            workDataOf(FETCH_USER_INFO_WORKER_RESULT to userManager.user.delinquent)
        }.fold(
            onSuccess = { resultData -> Result.success(resultData) },
            onFailure = { exception ->
                Result.failure(workDataOf(FETCH_USER_INFO_WORKER_EXCEPTION_MESSAGE to exception.message))
            }
        )
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        operator fun invoke(): WorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<FetchUserInfoWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(FETCH_USER_INFO_WORKER_NAME, ExistingWorkPolicy.KEEP, request)
            return request
        }
    }
}
