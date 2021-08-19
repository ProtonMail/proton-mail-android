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
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.contacts.receive.LabelsMapper
import ch.protonmail.android.api.models.messages.receive.LabelRequestBody
import ch.protonmail.android.api.models.messages.receive.LabelResponse
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.core.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.network.domain.ApiResult
import javax.inject.Inject

internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_NAME = "keyCreateContactGroupInputDataName"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_ID = "keyCreateContactGroupInputDataId"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE = "keyCreateContactGroupInputDataIsUpdate"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_COLOR = "keyCreateContactGroupInputDataColor"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_STICKY = "keyCreateContactGroupInputDataIsSticky"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_GROUP_EXPANDED = "keyCreateContactGroupInputDataExpanded"
internal const val KEY_RESULT_DATA_CREATE_CONTACT_GROUP_ERROR = "keyCreateContactGroupResultWorkerError"

@HiltWorker
class CreateContactGroupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiManager: ProtonMailApiManager,
    private val repository: ContactGroupsRepository,
    private val labelsMapper: LabelsMapper,
    private val accountManager: AccountManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val groupName = getContactGroupNameParam() ?: return Result.failure()
        val color = getContactGroupColorParam() ?: return Result.failure()

        return when (val response = createContactGroup(groupName, color)) {
            is ApiResult.Success -> {
                val labelResponse = response.value
                if (labelResponse.label.id.isEmpty()) {
                    return failureResultWithError("Error, Label id is empty")
                }
                val contactLabel = labelsMapper.mapLabelToContactLabelEntity(labelResponse.label)
                repository.saveContactGroup(contactLabel)
                return Result.success()
            }
            is ApiResult.Error.Http -> {
                return failureResultWithError(response.proton?.error ?: "unknown error")
            }
            else -> {
                Result.failure()
            }
        }
    }

    private fun failureResultWithError(error: String): Result {
        val errorData = workDataOf(KEY_RESULT_DATA_CREATE_CONTACT_GROUP_ERROR to error)
        return Result.failure(errorData)
    }

    private suspend fun createContactGroup(name: String, color: String): ApiResult<LabelResponse> {
        val labelBody = buildLabelBody(name, color)
        val userId = requireNotNull(accountManager.getPrimaryUserId().first())

        if (isUpdateParam()) {
            val validContactGroupId = getContactGroupIdParam() ?: throw missingContactGroupIdError()
            return apiManager.updateLabel(userId, validContactGroupId, labelBody)
        }

        return apiManager.createLabel(userId, labelBody)
    }

    private fun buildLabelBody(name: String, color: String) =
        LabelRequestBody(
            name = name,
            color = color,
            type = Constants.LABEL_TYPE_CONTACT_GROUPS,
            parentId = "",
            notify = 0,
            expanded = getExpandedParam(),
            sticky = getStickyParam()
        )

    private fun missingContactGroupIdError() =
        IllegalArgumentException("Missing required ID parameter to create contact group")

    private fun getContactGroupIdParam() = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_ID)

    private fun getExpandedParam() = inputData.getInt(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_EXPANDED, 0)

    private fun getStickyParam() = inputData.getInt(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_STICKY, 0)

    private fun getContactGroupColorParam() = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_COLOR)

    private fun getContactGroupNameParam() = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_NAME)

    private fun isUpdateParam() = inputData.getBoolean(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE, false)

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            name: String,
            color: String,
            expanded: Int? = 0,
            sticky: Int? = 0,
            update: Boolean? = false,
            id: String? = null
        ): LiveData<WorkInfo> {

            val createContactGroupRequest = OneTimeWorkRequestBuilder<CreateContactGroupWorker>()
                .setInputData(
                    workDataOf(
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_ID to id,
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_NAME to name,
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_COLOR to color,
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_EXPANDED to expanded,
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE to update,
                        KEY_INPUT_DATA_CREATE_CONTACT_GROUP_STICKY to sticky
                    )
                ).build()

            workManager.enqueue(createContactGroupRequest)
            return workManager.getWorkInfoByIdLiveData(createContactGroupRequest.id)
        }
    }

}

