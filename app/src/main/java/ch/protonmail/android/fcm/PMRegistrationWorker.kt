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

package ch.protonmail.android.fcm

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DEVICE_ENVIRONMENT_ANDROID
import ch.protonmail.android.api.models.RegisterDeviceRequestBody
import ch.protonmail.android.core.Constants.RESPONSE_CODE_OK
import ch.protonmail.android.utils.BuildInfo
import javax.inject.Inject

// region constants
const val KEY_PM_REGISTRATION_WORKER_USERNAME = "username"
const val KEY_PM_REGISTRATION_WORKER_ERROR = "pmRegistrationWorkerError"
// endregion

/**
 * A CoroutineWorker that handles device registration on PM servers
 */

class PMRegistrationWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val buildInfo: BuildInfo,
    private val protonMailApiManager: ProtonMailApiManager
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val username = inputData.getString(KEY_PM_REGISTRATION_WORKER_USERNAME)
        if (username.isNullOrBlank()) {
            return Result.failure(workDataOf(KEY_PM_REGISTRATION_WORKER_ERROR to "Username not provided"))
        }

        val registerDeviceRequestBody = RegisterDeviceRequestBody(
            deviceToken = FcmUtil.getFirebaseToken(),
            deviceName = "Android",
            deviceModel = buildInfo.model,
            deviceVersion = "${buildInfo.sdkVersion}",
            appVersion = "Android_${buildInfo.versionName}",
            environment = DEVICE_ENVIRONMENT_ANDROID
        )

        return runCatching {
            protonMailApiManager.registerDevice(registerDeviceRequestBody, username)
        }.map { registerDeviceResponseBody ->
            if (registerDeviceResponseBody.code == RESPONSE_CODE_OK) {
                FcmUtil.setTokenSent(username, true)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    class Enqueuer @Inject constructor(
        private val workManager: WorkManager,
        private val accountManager: AccountManager
    ) {

        operator fun invoke() {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            for (username in accountManager.getLoggedInUsers()) {
                if (!FcmUtil.isTokenSent(username)) {
                    val request = OneTimeWorkRequestBuilder<PMRegistrationWorker>()
                        .setConstraints(constraints)
                        .setInputData(workDataOf(KEY_PM_REGISTRATION_WORKER_USER_ID to userId.s))
                        .build()

                    workManager.enqueue(request)
                }
            }
        }
    }
}
