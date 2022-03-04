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
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.QueueNetworkUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * Work Manager Worker responsible sending a continuous ping message to the backend in a predefined interval
 * in order to check if backend server is still reachable.
 *
 * @see androidx.work.WorkManager
 */
@HiltWorker
class PingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ProtonMailApiManager,
    private val queueNetworkUtil: QueueNetworkUtil,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        withContext(dispatchers.Io) {
            runCatching {
                isBackendStillReachable()
            }.fold(
                onSuccess = { isAccessible ->
                    Timber.v("Ping isAccessible: $isAccessible")
                    if (isAccessible) {
                        queueNetworkUtil.setCurrentlyHasConnectivity()
                        Result.success()
                    } else {
                        Result.failure()
                    }
                },
                onFailure = { throwable ->
                    Timber.w(throwable, "Ping call has failed")
                    queueNetworkUtil.setConnectivityHasFailed(throwable)
                    Result.failure(
                        workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code ${throwable.message}")
                    )
                }
            )
        }

    private suspend fun isBackendStillReachable(): Boolean {
        val responseBody = api.ping()
        return when (responseBody.code) {
            Constants.RESPONSE_CODE_OK,
            Constants.RESPONSE_CODE_API_OFFLINE -> true
            else -> {
                Timber.w(
                    """
                    Ping call has failed.
                    Response code = ${responseBody.code}
                    Response error = ${responseBody.error}
                    """.trimIndent()
                )
                false
            }
        }
    }


    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        private val uniqueWorkerName = "PingWorker"

        fun enqueue() {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<PingWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(
                uniqueWorkerName,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
            Timber.v("Scheduled ping work request id:${workRequest.id}")
        }

        fun getWorkInfoState(): LiveData<List<WorkInfo>?> =
            workManager.getWorkInfosForUniqueWorkLiveData(uniqueWorkerName)
    }
}
