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
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.usecase.fetch.FetchMailSettings
import kotlinx.coroutines.flow.first
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getAccounts
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * A Worker that handles fetching mail settings for all ready accounts.
 */
@HiltWorker
class FetchMailSettingsWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val fetchMailSettings: FetchMailSettings,
    private val accountManager: AccountManager,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            accountManager.getAccounts(AccountState.Ready).first().forEach {
                withContext(dispatchers.Io) { fetchMailSettings.invoke(Id(it.userId.id)) }
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                Timber.d(it)
                Result.retry()
            }
        )
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue() {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<FetchMailSettingsWorker>()
                .setConstraints(constraints)
                .build()
            Timber.v("Scheduling Fetch Mail Settings Worker")
            workManager.enqueue(request)
        }
    }
}
