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
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.segments.TEN_SECONDS
import ch.protonmail.android.core.Constants.CONTACTS_PAGE_SIZE
import ch.protonmail.android.core.UserManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val MAX_RETRY_COUNT = 3

/**
 * Work Manager Worker responsible for fetching contacts.
 *
 *  InputData has to contain non-null values for:
 *  labelId
 *
 * @see androidx.work.WorkManager
 */
@HiltWorker
class FetchContactsDataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ProtonMailApiManager,
    private val databaseProvider: DatabaseProvider,
    private val userManager: UserManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = userManager.currentUserId
            ?: return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Can't fetch contacts without a logged in user")
            )

        val contactDao = databaseProvider.provideContactDao(userId)

        return runCatching {
            Timber.v("Fetch Contacts Worker started")
            var page = 0
            var response = api.fetchContacts(page, CONTACTS_PAGE_SIZE)
            response.contacts?.let { contacts ->
                val total = response.total
                var fetched = contacts.size
                while (total > fetched) {
                    ++page
                    response = api.fetchContacts(page, CONTACTS_PAGE_SIZE)
                    val contactDataList = response.contacts
                    if (contactDataList.isNullOrEmpty()) {
                        break
                    }
                    contacts.addAll(contactDataList)
                    fetched = contacts.size
                }

                if (contacts.isNotEmpty()) {
                    contactDao.saveAllContactsData(contacts)
                }
            }
        }.fold(
            onSuccess = {
                Result.success()
            },
            onFailure = {
                shouldReRunOnThrowable(it)
            }
        )
    }

    private fun shouldReRunOnThrowable(throwable: Throwable): Result {
        if (throwable is CancellationException) {
            throw throwable
        }

        return if (runAttemptCount < MAX_RETRY_COUNT) {
            Timber.d(throwable, "Fetch Contacts Worker failure, retrying count: $runAttemptCount")
            Result.retry()
        } else {
            Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code ${throwable.message}")
            )
        }
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(): LiveData<WorkInfo> {
            val networkConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<FetchContactsDataWorker>()
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2 * TEN_SECONDS, TimeUnit.SECONDS)
                .build()

            workManager.enqueue(workRequest)
            Timber.v("Scheduling Fetch Contacts Data Worker")
            return workManager.getWorkInfoByIdLiveData(workRequest.id)
        }
    }
}
