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
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.AppUtil
import timber.log.Timber
import javax.inject.Inject

internal const val KEY_INPUT_USER_NAME = "KeyInputUserName"
internal const val KEY_INPUT_FCM_REGISTRATION_ID = "KeyInputRegistrationId"

/**
 * Work Manager Worker responsible for sending various logout related network calls.
 *
 * @see androidx.work.WorkManager
 */
class LogoutWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ProtonMailApiManager,
    private val accountManager: AccountManager,
    private val userManager: UserManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching {
            val userName = inputData.getString(KEY_INPUT_USER_NAME) ?: userManager.username
            val registrationId = inputData.getString(KEY_INPUT_FCM_REGISTRATION_ID) ?: ""

            if (userName.isEmpty()) {
                throw IllegalArgumentException("Cannot proceed with an empty user name")
            }

            val loggedInUsers = accountManager.getLoggedInUsers()
            // Unregister FCM only if this is the last user on the device
            if (loggedInUsers.isEmpty() || loggedInUsers.size == 1 && loggedInUsers[0] == userName) {
                if (registrationId.isNotEmpty()) {
                    Timber.v("Unregistering from Firebase Cloud Messaging (FCM)")
                    api.unregisterDevice(registrationId)
                } else {
                    Timber.w("LogoutWorker called with empty FCM registration token")
                }
            }
            AppUtil.postEventOnUi(LogoutEvent(Status.SUCCESS))
            accountManager.clear()

            // Revoke access token through API
            if (userName.isNotEmpty()) {
                api.revokeAccess(userName)
            }

            AppUtil.deleteSecurePrefs(userName, userManager.nextLoggedInAccountOtherThanCurrent == null)
            userManager.getTokenManager(userName)?.clear()
            TokenManager.clearInstance(userName)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { throwable ->
                Timber.d(throwable, "Logout Failure")
                Result.failure(workDataOf(KEY_WORKER_ERROR_DESCRIPTION to throwable.message))
            }
        )

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(userName: String, fcmRegistrationId: String): LiveData<WorkInfo> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<LogoutWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_USER_NAME to userName,
                        KEY_INPUT_FCM_REGISTRATION_ID to fcmRegistrationId
                    )
                )
                .build()
            workManager.enqueue(workRequest)
            return workManager.getWorkInfoByIdLiveData(workRequest.id)
        }
    }
}
