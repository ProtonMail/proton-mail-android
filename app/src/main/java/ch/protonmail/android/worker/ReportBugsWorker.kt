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
import androidx.work.workDataOf
import ch.protonmail.android.R
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.utils.notifier.UserNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.network.domain.ApiResult
import timber.log.Timber
import javax.inject.Inject

internal const val KEY_INPUT_OS_NAME = "KeyInputOsName"
internal const val KEY_INPUT_APP_VERSION = "KeyInputAppVersion"
internal const val KEY_INPUT_CLIENT = "KeyInputClient"
internal const val KEY_INPUT_CLIENT_VERSION = "KeyInputClientVersion"
internal const val KEY_INPUT_TITLE = "KeyInputTitle"
internal const val KEY_INPUT_DESCRIPTION = "KeyInputDescription"
internal const val KEY_INPUT_USER_NAME = "KeyInputUserName"
internal const val KEY_INPUT_EMAIL = "KeyInputEmail"

@HiltWorker
class ReportBugsWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private var api: ProtonMailApiManager,
    private val userNotifier: UserNotifier
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        val osName = requireNotNull(inputData.getString(KEY_INPUT_OS_NAME))
        val appVersion = requireNotNull(inputData.getString(KEY_INPUT_APP_VERSION))
        val client = requireNotNull(inputData.getString(KEY_INPUT_CLIENT))
        val clientVersion = requireNotNull(inputData.getString(KEY_INPUT_CLIENT_VERSION))
        val title = requireNotNull(inputData.getString(KEY_INPUT_TITLE))
        val description = requireNotNull(inputData.getString(KEY_INPUT_DESCRIPTION))
        val userName = requireNotNull(inputData.getString(KEY_INPUT_USER_NAME))
        val email = requireNotNull(inputData.getString(KEY_INPUT_EMAIL))

        val response = api.reportBug(
            osName,
            appVersion,
            client,
            clientVersion,
            title,
            description,
            userName,
            email
        )
        Timber.v("Report bugs response $response")
        return if (response is ApiResult.Success) {
            userNotifier.showError(R.string.received_report)
            Result.success()
        } else {
            userNotifier.showError(R.string.not_received_report)
            Result.failure()
        }

    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            osName: String,
            appVersion: String,
            client: String,
            clientVersion: String,
            title: String,
            description: String,
            userName: String,
            email: String
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ReportBugsWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_OS_NAME to osName,
                        KEY_INPUT_APP_VERSION to appVersion,
                        KEY_INPUT_CLIENT to client,
                        KEY_INPUT_CLIENT_VERSION to clientVersion,
                        KEY_INPUT_TITLE to title,
                        KEY_INPUT_DESCRIPTION to description,
                        KEY_INPUT_USER_NAME to userName,
                        KEY_INPUT_EMAIL to email
                    )
                )
                .build()
            workManager.enqueue(request)
        }
    }
}
