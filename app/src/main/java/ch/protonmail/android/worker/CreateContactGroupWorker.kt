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
import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.models.messages.receive.LabelResponse
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.core.Constants
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_NAME = "keyCreateContactGroupInputDataName"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_ID = "keyCreateContactGroupInputDataId"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE = "keyCreateContactGroupInputDataIsUpdate"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_COLOR = "keyCreateContactGroupInputDataColor"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_DISPLAY = "keyCreateContactGroupInputDataIsDisplay"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_EXCLUSIVE = "keyCreateContactGroupInputDataExclusive"
internal const val KEY_RESULT_DATA_CREATE_CONTACT_GROUP_ERROR = "keyCreateContactGroupResultWorkerError"

class CreateContactGroupWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiManager: ProtonMailApiManager,
    private val repository: ContactGroupsRepository,
    private val dispatcherProvider: DispatcherProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val groupName = getContactGroupNameParam() ?: return Result.failure()
        val color = getContactGroupColorParam() ?: return Result.failure()

        val response = withContext(dispatcherProvider.Io) {
            createContactGroup(groupName, color)
        }

        if (response.hasError()) {
            return failureResultWithError(response.error)
        }

        if (hasInvalidApiResponse(response)) {
            return failureResultWithError(response.error)
        }

        repository.saveContactGroup(response.contactGroup)
        return Result.success()
    }

    private fun failureResultWithError(error: String): Result {
        val errorData = workDataOf(KEY_RESULT_DATA_CREATE_CONTACT_GROUP_ERROR to error)
        return Result.failure(errorData)
    }

    private fun createContactGroup(name: String, color: String): LabelResponse {
        val labelBody = buildLabelBody(name, color)

        if (isUpdateParam()) {
            val validContactGroupId = getContactGroupIdParam() ?: throw missingContactGroupIdError()
            return apiManager.updateLabel(validContactGroupId, labelBody)
        }

        return apiManager.createLabel(labelBody)
    }

    private fun buildLabelBody(name: String, color: String) =
        LabelBody(
            name,
            color,
            getDisplayParam(),
            getExclusiveParam(),
            Constants.LABEL_TYPE_CONTACT_GROUPS
        )

    private fun missingContactGroupIdError() =
        IllegalArgumentException("Missing required ID parameter to create contact group")

    private fun hasInvalidApiResponse(labelResponse: LabelResponse) = labelResponse.contactGroup.ID.isEmpty()

    private fun getContactGroupIdParam() = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_ID)

    private fun getExclusiveParam() = inputData.getInt(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_EXCLUSIVE, 0)

    private fun getDisplayParam() = inputData.getInt(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_DISPLAY, 0)

    private fun getContactGroupColorParam() = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_COLOR)

    private fun getContactGroupNameParam() = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_NAME)

    private fun isUpdateParam() = inputData.getBoolean(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE, false)

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {
        fun enqueue(
            name: String,
            color: String,
            display: Int? = 0,
            exclusive: Int? = 0,
            update: Boolean? = false,
            id: String? = null
        ): LiveData<WorkInfo> {

            val createContactGroupRequest = OneTimeWorkRequestBuilder<CreateContactGroupWorker>()
                .setInputData(
                    workDataOf(
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_ID to id,
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_NAME to name,
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_COLOR to color,
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_EXCLUSIVE to exclusive,
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE to update,
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_DISPLAY to display
                    )
                ).build()

            workManager.enqueue(createContactGroupRequest)
            return workManager.getWorkInfoByIdLiveData(createContactGroupRequest.id)
        }
    }

}

