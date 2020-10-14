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
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.fcm.FcmUtil
import ch.protonmail.android.utils.AppUtil
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

private const val MAX_RETRY_COUNT = 3

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
    private val userManager: UserManager,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        withContext(dispatchers.Io) {
            val userName = userManager.username

            Timber.v("Unregistering user: $userName")

            val loggedInUsers = accountManager.getLoggedInUsers()
            if (loggedInUsers.isEmpty() || loggedInUsers.size == 1 && loggedInUsers[0] == userName) {
                val registrationId = FcmUtil.getRegistrationId()
                if (registrationId.isNotEmpty()) {
                    Timber.v("Unregistering from Firebase Cloud Messaging (FCM)")
                    api.unregisterDevice(registrationId)
                }
                //AppUtil.postEventOnUi(LogoutEvent(Status.SUCCESS))
            }
            accountManager.clear()

            // Revoke access token through API
            if (userName.isNotEmpty()) {
                api.revokeAccess(userName)
            }

            AppUtil.deleteSecurePrefs(userName, userManager.nextLoggedInAccountOtherThanCurrent == null)
            userManager.getTokenManager(userName)?.clear()
            TokenManager.clearInstance(userName)

            Result.success()
        }

    fun shouldReRunOnThrowable(exception: Exception, runCount: Int, maxRunCount: Int): Result {
        return if (!
            (
                exception.cause is IllegalArgumentException &&
                    (exception.cause as IllegalArgumentException).message == "value == null"
                )
        )
            Result.retry()
        else
            Result.failure()
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(): LiveData<WorkInfo> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<LogoutWorker>()
                .setConstraints(constraints)
                .build()
            workManager.enqueue(workRequest)

            return workManager.getWorkInfoByIdLiveData(workRequest.id)
        }
    }
}
