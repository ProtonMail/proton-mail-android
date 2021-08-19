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
import ch.protonmail.android.api.models.UnregisterDeviceRequestBody
import ch.protonmail.android.prefs.SecureSharedPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getAccounts
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import javax.inject.Inject

@HiltWorker
class UnregisterDeviceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val protonMailApiManager: ProtonMailApiManager,
    private val accountManager: AccountManager
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        // Only unregister device if no more ready/logged account.
        if (accountManager.getAccounts(AccountState.Ready).first().isNotEmpty()) return Result.success()

        val token = inputData.getString(KEY_PM_REGISTRATION_WORKER_TOKEN)?.takeIfNotBlank()
            ?: return Result.failure(workDataOf(KEY_PM_REGISTRATION_WORKER_ERROR to "Token not provided"))

        val sessionId = inputData.getString(KEY_PM_REGISTRATION_WORKER_SESSION_ID)
        val unregisterDeviceRequestBody = UnregisterDeviceRequestBody(
            deviceToken = token,
            sessionId = sessionId
        )

        return runCatching {
            protonMailApiManager.unregisterDevice(unregisterDeviceRequestBody)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                Timber.d(it)
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        )
    }

    class Enqueuer @Inject constructor(
        private val context: Context,
        private val workManager: WorkManager,
        private val fcmTokenManagerFactory: FcmTokenManager.Factory
    ) {

        operator fun invoke(userId: UserId, sessionId: SessionId?) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val userPrefs = SecureSharedPreferences.getPrefsForUser(context, userId)
            val fcmTokenManager = fcmTokenManagerFactory.create(userPrefs)
            val token = fcmTokenManager.getTokenBlocking()?.value

            val request = OneTimeWorkRequestBuilder<UnregisterDeviceWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_PM_REGISTRATION_WORKER_TOKEN to token,
                        // Don't specify sessionId as the session could already be revoked (-> HTTP 422).
                        // So, we only unregister device if no more ready/logged account (see above).
                        // KEY_PM_REGISTRATION_WORKER_SESSION_ID to sessionId?.id
                    )
                )
                .build()

            workManager.enqueue(request)
        }
    }
}
