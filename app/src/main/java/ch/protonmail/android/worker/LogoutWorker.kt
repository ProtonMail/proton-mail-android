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
import ch.protonmail.android.core.PREF_PIN
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.utils.AppUtil
import me.proton.core.util.android.sharedpreferences.clearAll
import timber.log.Timber
import javax.inject.Inject

internal const val KEY_INPUT_USER_ID = "KeyInputUserId"
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
            val userId = Id(checkNotNull(inputData.getString(KEY_INPUT_USER_ID)) { "User id is required" })
            val registrationId = inputData.getString(KEY_INPUT_FCM_REGISTRATION_ID) ?: ""

            Timber.v("Unregistering user: $userId")

            val loggedInUsers = accountManager.allLoggedIn()
            // Unregister FCM only if this is the last user on the device
            if (loggedInUsers.isEmpty() || loggedInUsers.size == 1 && loggedInUsers.first() == userId) {
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
            api.revokeAccess(userId)

            PREF_PIN
            val prefs = SecureSharedPreferences.getPrefsForUser(applicationContext, userId)
            val isThereAnotherLoggedInUser = userManager.getNextLoggedInUser() == null
            if (isThereAnotherLoggedInUser) prefs.clearAll()
            else prefs.clearAll(excludedKeys = arrayOf(PREF_PIN))

            userManager.getTokenManager(userId).clear()
            TokenManager.clearInstance(userId)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { throwable ->
                Timber.d(throwable, "Logout Failure")
                Result.failure(workDataOf(KEY_WORKER_ERROR_DESCRIPTION to throwable.message))
            }
        )

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(userId: Id, fcmRegistrationId: String?): LiveData<WorkInfo> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<LogoutWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_USER_ID to userId.s,
                        KEY_INPUT_FCM_REGISTRATION_ID to fcmRegistrationId
                    )
                )
                .build()
            workManager.enqueue(workRequest)
            Timber.v("Scheduling logout for $userId - token: $fcmRegistrationId")
            return workManager.getWorkInfoByIdLiveData(workRequest.id)
        }

    }
}
