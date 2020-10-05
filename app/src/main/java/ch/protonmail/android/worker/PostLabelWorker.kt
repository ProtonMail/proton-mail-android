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
import androidx.work.Worker
import androidx.work.WorkerParameters
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.models.messages.receive.LabelResponse
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory.Companion.getInstance
import ch.protonmail.android.events.LabelAddedEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.AppUtil

internal const val KEY_INPUT_DATA_LABEL_NAME = "keyInputDataLabelName"
internal const val KEY_INPUT_DATA_LABEL_ID = "keyInputDataLabelId"
internal const val KEY_INPUT_DATA_IS_UPDATE = "keyInputDataIsUpdate"
internal const val KEY_INPUT_DATA_LABEL_COLOR = "keyInputDataLabelColor"
internal const val KEY_INPUT_DATA_LABEL_IS_DISPLAY = "keyInputDataLabelIsDisplay"
internal const val KEY_INPUT_DATA_LABEL_EXCLUSIVE = "keyInputDataLabelExlusive"

class PostLabelWorker @WorkerInject constructor(
    @Assisted val context: Context,
    @Assisted val workerParams: WorkerParameters,
    private val apiManager: ProtonMailApiManager
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val update = inputData.getBoolean(KEY_INPUT_DATA_IS_UPDATE, false)
        val labelName = inputData.getString(KEY_INPUT_DATA_LABEL_NAME) ?: return Result.failure()
        val color = inputData.getString(KEY_INPUT_DATA_LABEL_COLOR) ?: return Result.failure()
        val display = inputData.getInt(KEY_INPUT_DATA_LABEL_IS_DISPLAY, 0)
        val exclusive = inputData.getInt(KEY_INPUT_DATA_LABEL_EXCLUSIVE, 0)
        val labelIdInput = inputData.getString(KEY_INPUT_DATA_LABEL_ID) ?: return Result.failure()

        val messagesDatabase = getInstance(applicationContext).getDatabase()
        val labelResponse: LabelResponse = if (!update) {
            apiManager.createLabel(LabelBody(labelName, color, display, exclusive))
        } else {
            apiManager.updateLabel(labelIdInput, LabelBody(labelName, color, display, exclusive))
        }
        if (labelResponse.hasError()) {
            val errorText = labelResponse.error
            AppUtil.postEventOnUi(LabelAddedEvent(Status.FAILED, errorText))
            return Result.failure()
        }

        val labelBody = labelResponse.label
        if (labelBody == null) {
            // we have no label response, checking for error
            AppUtil.postEventOnUi(LabelAddedEvent(Status.FAILED, labelResponse.error))
            return Result.failure()
        }
        val labelId = labelBody.id
        // update local label
        // update local label
        if (labelId != "") {
            messagesDatabase.saveLabel(labelBody)
            AppUtil.postEventOnUi(LabelAddedEvent(Status.SUCCESS, null))
        }

        return Result.success()
    }

}