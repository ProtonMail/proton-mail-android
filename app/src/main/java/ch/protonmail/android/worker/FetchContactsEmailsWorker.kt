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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result.success
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.api.segments.contact.ContactEmailsManager
import ch.protonmail.android.events.ContactsFetchedEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.AppUtil
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.milliseconds

class FetchContactsEmailsWorker @WorkerInject constructor (
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val contactEmailsManager: ContactEmailsManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching { contactEmailsManager.refresh() }
            .fold(onSuccess = {
                // TODO: remove and observe Work directly
                AppUtil.postEventOnUi(ContactsFetchedEvent(Status.SUCCESS))
                success()
            }, onFailure = {
                // TODO: remove and observe Work directly
                AppUtil.postEventOnUi(ContactsFetchedEvent(Status.FAILED))
                failure(it)
            })
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        operator fun invoke(delay: Duration = 0.milliseconds): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<FetchContactsEmailsWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delay.toLongMilliseconds(), TimeUnit.MILLISECONDS)
                .build()

            return workManager.enqueue(workRequest)
        }

        @Deprecated(
            "Use invoke with kotlin.time APIs, this is for Java only",
            ReplaceWith("invoke(delayMs.milliseconds)", imports = ["kotlin.time.milliseconds"])
        )
        @JvmOverloads
        fun enqueue(delayMs: Long = 0): Operation =
            invoke(delayMs.milliseconds)

    }
}
