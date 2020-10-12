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
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager

internal const val KEY_CREATE_CONTACT_GROUP_INPUT_DATA_LABEL_NAME = "keyCreateContactGroupInputDataLabelName"
internal const val KEY_CREATE_CONTACT_GROUP_INPUT_DATA_LABEL_ID = "keyCreateContactGroupInputDataLabelId"
internal const val KEY_CREATE_CONTACT_GROUP_INPUT_DATA_IS_UPDATE = "keyCreateContactGroupInputDataIsUpdate"
internal const val KEY_CREATE_CONTACT_GROUP_INPUT_DATA_LABEL_COLOR = "keyCreateContactGroupInputDataLabelColor"
internal const val KEY_CREATE_CONTACT_GROUP_INPUT_DATA_LABEL_DISPLAY = "keyCreateContactGroupInputDataLabelIsDisplay"
internal const val KEY_CREATE_CONTACT_GROUP_INPUT_DATA_LABEL_EXCLUSIVE = "keyCreateContactGroupInputDataLabelExclusive"
internal const val KEY_CREATE_CONTACT_GROUP_WORKER_RESULT_ERROR = "keyCreateContactGroupResultDataPostLabelWorkerError"

class CreateContactGroupWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ProtonMailApiManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }

}

class Enqueuer(private val workManager: WorkManager) {
    fun enqueue(
        labelName: String,
        color: String,
        display: Int? = 0,
        exclusive: Int? = 0,
        update: Boolean? = false,
        labelId: String? = null
    ): LiveData<WorkInfo> {

        val postLabelWorkerRequest = OneTimeWorkRequestBuilder<CreateContactGroupWorker>()
            .setInputData(
                workDataOf(
                    KEY_CREATE_CONTACT_GROUP_INPUT_DATA_LABEL_ID to labelId,
                    KEY_CREATE_CONTACT_GROUP_INPUT_DATA_LABEL_NAME to labelName,
                    KEY_CREATE_CONTACT_GROUP_INPUT_DATA_LABEL_COLOR to color,
                    KEY_CREATE_CONTACT_GROUP_INPUT_DATA_LABEL_EXCLUSIVE to exclusive,
                    KEY_CREATE_CONTACT_GROUP_INPUT_DATA_IS_UPDATE to update,
                    KEY_CREATE_CONTACT_GROUP_INPUT_DATA_LABEL_DISPLAY to display
                )
            ).build()

        workManager.enqueue(postLabelWorkerRequest)
        return workManager.getWorkInfoByIdLiveData(postLabelWorkerRequest.id)
    }
}

