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
import androidx.work.ListenableWorker.Result.failure
import androidx.work.ListenableWorker.Result.success
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.api.segments.contact.ContactEmailsManager
import ch.protonmail.android.core.UserManager
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FetchContactsEmailsWorker @WorkerInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val userManager: UserManager,
    private val contactEmailsManagerFactory: ContactEmailsManager.AssistedFactory
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val currentUserId = userManager.currentUserId
            ?: return failure()

        return runCatching { contactEmailsManagerFactory.create(currentUserId).refresh() }
            .fold(
                onSuccess = {
                    success()
                },
                onFailure = { throwable ->
                    if (throwable is CancellationException) {
                        throw throwable
                    }
                    failure(throwable)
                }
            )
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(delayMs: Long = 0): LiveData<WorkInfo> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<FetchContactsEmailsWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueue(workRequest)
            Timber.v("Scheduling Fetch Contacts Emails Worker")
            return workManager.getWorkInfoByIdLiveData(workRequest.id)
        }
    }
}
