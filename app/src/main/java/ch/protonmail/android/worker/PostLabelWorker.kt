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
import android.widget.Toast
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.models.messages.receive.LabelResponse
import ch.protonmail.android.data.LabelRepository
import ch.protonmail.android.events.LabelAddedEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.AppUtil

internal const val KEY_INPUT_DATA_LABEL_NAME = "keyInputDataLabelName"
internal const val KEY_INPUT_DATA_LABEL_ID = "keyInputDataLabelId"
internal const val KEY_INPUT_DATA_IS_UPDATE = "keyInputDataIsUpdate"
internal const val KEY_INPUT_DATA_LABEL_COLOR = "keyInputDataLabelColor"
internal const val KEY_INPUT_DATA_LABEL_DISPLAY = "keyInputDataLabelIsDisplay"
internal const val KEY_INPUT_DATA_LABEL_EXCLUSIVE = "keyInputDataLabelExlusive"

class PostLabelWorker @WorkerInject constructor(
    @Assisted val context: Context,
    @Assisted val workerParams: WorkerParameters,
    private val apiManager: ProtonMailApiManager,
    private val labelRepository: LabelRepository
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val update = inputData.getBoolean(KEY_INPUT_DATA_IS_UPDATE, false)
        val labelName = inputData.getString(KEY_INPUT_DATA_LABEL_NAME) ?: return Result.failure()
        val color = inputData.getString(KEY_INPUT_DATA_LABEL_COLOR) ?: return Result.failure()
        val display = inputData.getInt(KEY_INPUT_DATA_LABEL_DISPLAY, 0)
        val exclusive = inputData.getInt(KEY_INPUT_DATA_LABEL_EXCLUSIVE, 0)
        val labelIdInput = inputData.getString(KEY_INPUT_DATA_LABEL_ID)

        val labelResponse: LabelResponse = if (!update) {
            apiManager.createLabel(LabelBody(labelName, color, display, exclusive))
        } else {
            val validLabelId = labelIdInput ?: return Result.failure()
            apiManager.updateLabel(validLabelId, LabelBody(labelName, color, display, exclusive))
        }
        if (labelResponse.hasError()) {
            val errorText = labelResponse.error
            AppUtil.postEventOnUi(LabelAddedEvent(Status.FAILED, errorText))
            return Result.failure()
        }

        if (labelResponse.label == null) {
            AppUtil.postEventOnUi(LabelAddedEvent(Status.FAILED, labelResponse.error))
            return Result.failure()
        }

        if (labelResponse.label.id != "") {
            labelRepository.saveLabel(labelResponse.label)
            AppUtil.postEventOnUi(LabelAddedEvent(Status.SUCCESS, null))
        }

        return Result.success()
    }

    class Enqueuer(private val workManager: WorkManager) {

        fun enqueue(labelName: String,
                    color: String,
                    display: Int,
                    exclusive: Int,
                    update: Boolean,
                    labelId: String?): Operation {

            val postLabelWorker = OneTimeWorkRequestBuilder<PostLabelWorker>()
                .setInputData(workDataOf(
                    KEY_INPUT_DATA_LABEL_ID to labelId,
                    KEY_INPUT_DATA_LABEL_NAME to labelName,
                    KEY_INPUT_DATA_LABEL_COLOR to color,
                    KEY_INPUT_DATA_LABEL_EXCLUSIVE to exclusive,
                    KEY_INPUT_DATA_IS_UPDATE to update,
                    KEY_INPUT_DATA_LABEL_DISPLAY to display
                ))
                .build()

            return workManager.enqueue(postLabelWorker)
        }
    }

}