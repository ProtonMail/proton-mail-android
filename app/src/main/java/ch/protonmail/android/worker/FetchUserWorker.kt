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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.attachments.KEY_INPUT_DATA_USER_ID_STRING
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.Delinquent
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

// region constants
const val FETCH_USER_INFO_WORKER_NAME = "FetchUserInfoWorker"
const val FETCH_USER_INFO_WORKER_RESULT = "FetchUserInfoWorkerResult"
const val FETCH_USER_INFO_WORKER_EXCEPTION_MESSAGE = "FetchUserInfoWorkerExceptionMessage"
// endregion

@Deprecated("Refactor resultData + BaseActivity.checkDelinquency")
@HiltWorker
class FetchUserWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userManager: UserManager,
    private val oldUserManager: ch.protonmail.android.core.UserManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userIdString = inputData.getString(KEY_INPUT_DATA_USER_ID_STRING)?.takeIfNotBlank()
            ?: return Result.failure(workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty user id"))

        return runCatching {
            userManager.getUser(UserId(userIdString), refresh = true).also {
                oldUserManager.clearCache()
            }
        }.map { user ->
            // TODO: Remove this result.
            workDataOf(FETCH_USER_INFO_WORKER_RESULT to (user.delinquent != Delinquent.None))
        }.fold(
            onSuccess = { resultData -> Result.success(resultData) },
            onFailure = { exception ->
                Result.failure(workDataOf(FETCH_USER_INFO_WORKER_EXCEPTION_MESSAGE to exception.message))
            }
        )
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        operator fun invoke(userId: UserId): WorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = Data.Builder()
                .putString(KEY_INPUT_DATA_USER_ID_STRING, userId.id)
                .build()

            val request = OneTimeWorkRequestBuilder<FetchUserWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            workManager.enqueueUniqueWork(FETCH_USER_INFO_WORKER_NAME, ExistingWorkPolicy.KEEP, request)
            return request
        }
    }
}
