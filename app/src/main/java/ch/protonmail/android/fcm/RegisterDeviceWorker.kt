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
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DEVICE_ENVIRONMENT_ANDROID
import ch.protonmail.android.api.models.RegisterDeviceRequestBody
import ch.protonmail.android.core.Constants.RESPONSE_CODE_OK
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.feature.account.allLoggedInBlocking
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.utils.BuildInfo
import me.proton.core.accountmanager.domain.AccountManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

// region constants
const val KEY_PM_REGISTRATION_WORKER_USER_ID = "PMRegistrationWorker.input.user.id"
const val KEY_PM_REGISTRATION_WORKER_TOKEN = "PMRegistrationWorker.input.token"
const val KEY_PM_REGISTRATION_WORKER_SESSION_ID = "PMRegistrationWorker.input.session.id"
const val KEY_PM_REGISTRATION_WORKER_ERROR = "PMRegistrationWorker.error"
// endregion

/**
 * A CoroutineWorker that handles device registration on Proton servers.
 */
@HiltWorker
class RegisterDeviceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val buildInfo: BuildInfo,
    private val protonMailApiManager: ProtonMailApiManager,
    private val fcmTokenManagerFactory: FcmTokenManager.Factory
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_PM_REGISTRATION_WORKER_USER_ID)?.takeIfNotBlank()
            ?.let(::UserId)
            ?: return Result.failure(workDataOf(KEY_PM_REGISTRATION_WORKER_ERROR to "User id not provided"))

        val userPrefs = SecureSharedPreferences.getPrefsForUser(applicationContext, userId)
        val fcmTokenManager = fcmTokenManagerFactory.create(userPrefs)

        val registerDeviceRequestBody = RegisterDeviceRequestBody(
            deviceToken = checkNotNull(fcmTokenManager.getToken()).value,
            deviceName = "Android",
            deviceModel = buildInfo.model,
            deviceVersion = "${buildInfo.sdkVersion}",
            appVersion = "Android_${buildInfo.versionName}",
            environment = DEVICE_ENVIRONMENT_ANDROID
        )

        return runCatching {
            protonMailApiManager.registerDevice(userId, registerDeviceRequestBody)
        }.map { registerDeviceResponseBody ->
            if (registerDeviceResponseBody.code == RESPONSE_CODE_OK) {
                fcmTokenManager.setTokenSent(true)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { if (runAttemptCount < 3) Result.retry() else Result.failure() }
        )
    }

    class Enqueuer @Inject constructor(
        private val context: Context,
        private val workManager: WorkManager,
        private val accountManager: AccountManager,
        private val fcmTokenManagerFactory: FcmTokenManager.Factory
    ) {

        operator fun invoke() {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            for (userId in accountManager.allLoggedInBlocking()) {
                val userPrefs = SecureSharedPreferences.getPrefsForUser(context, userId)
                val fcmTokenManager = fcmTokenManagerFactory.create(userPrefs)
                if (fcmTokenManager.isTokenSentBlocking().not()) {
                    val request = OneTimeWorkRequestBuilder<RegisterDeviceWorker>()
                        .setConstraints(constraints)
                        .setInputData(workDataOf(KEY_PM_REGISTRATION_WORKER_USER_ID to userId.id))
                        .build()

                    workManager.enqueue(request)
                }
            }
        }
    }
}
