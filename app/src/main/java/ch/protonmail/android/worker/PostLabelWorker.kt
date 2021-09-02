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
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.local.model.LabelType
import ch.protonmail.android.labels.data.mapper.LabelEntityApiMapper
import ch.protonmail.android.labels.data.remote.model.LabelRequestBody
import ch.protonmail.android.labels.data.remote.model.LabelResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.network.domain.ApiResult

internal const val KEY_INPUT_DATA_LABEL_NAME = "keyInputDataLabelName"
internal const val KEY_INPUT_DATA_LABEL_ID = "keyInputDataLabelId"
internal const val KEY_INPUT_DATA_IS_UPDATE = "keyInputDataIsUpdate"
internal const val KEY_INPUT_DATA_LABEL_COLOR = "keyInputDataLabelColor"
internal const val KEY_INPUT_DATA_LABEL_EXPANDED = "keyInputDataLabelExpanded"
internal const val KEY_INPUT_DATA_LABEL_TYPE = "keyInputDataLabelType"
internal const val KEY_POST_LABEL_WORKER_RESULT_ERROR = "keyResultDataPostLabelWorkerError"

@HiltWorker
class PostLabelWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted val workerParams: WorkerParameters,
    private val apiManager: ProtonMailApiManager,
    private val labelRepository: LabelRepository,
    private val labelsMapper: LabelEntityApiMapper,
    private val accountManager: AccountManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val labelName = getLabelNameParam() ?: return Result.failure()
        val color = getLabelColorParam() ?: return Result.failure()
        val expanded = getExpandedParam()
        val type = getTypeParam()

        return when (val response = createOrUpdateLabel(labelName, color, expanded, type)) {
            is ApiResult.Success -> {
                val labelResponse = response.value
                if (labelResponse.label.id.isEmpty()) {
                    return failureResultWithError("Error, Label id is empty")
                }
                val userId = requireNotNull(accountManager.getPrimaryUserId().first())
                val contactLabelEntity = labelsMapper.toEntity(labelResponse.label, userId)
                labelRepository.saveLabel(contactLabelEntity)
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
        val errorData = workDataOf(KEY_POST_LABEL_WORKER_RESULT_ERROR to error)
        return Result.failure(errorData)
    }

    private suspend fun createOrUpdateLabel(
        labelName: String,
        color: String,
        expanded: Int,
        type: Int
    ): ApiResult<LabelResponse> {
        val requestBody = LabelRequestBody(
            name = labelName,
            color = color,
            type = type,
            parentId = null,
            notify = 0,
            expanded = expanded,
            sticky = 0
        )

        val userId = requireNotNull(accountManager.getPrimaryUserId().first())
        return if (isUpdateParam()) {
            val validLabelId = getLabelIdParam()
                ?: throw IllegalArgumentException("Missing required LabelID parameter")
            apiManager.updateLabel(userId, validLabelId, requestBody)
        } else {
            apiManager.createLabel(userId, requestBody)
        }
    }

    private fun getLabelIdParam() = inputData.getString(KEY_INPUT_DATA_LABEL_ID)

    private fun getTypeParam() = inputData.getInt(KEY_INPUT_DATA_LABEL_TYPE, LabelType.MESSAGE_LABEL.typeInt)

    private fun getExpandedParam() = inputData.getInt(KEY_INPUT_DATA_LABEL_EXPANDED, 0)

    private fun getLabelColorParam() = inputData.getString(KEY_INPUT_DATA_LABEL_COLOR)

    private fun getLabelNameParam() = inputData.getString(KEY_INPUT_DATA_LABEL_NAME)

    private fun isUpdateParam() = inputData.getBoolean(KEY_INPUT_DATA_IS_UPDATE, false)

    class Enqueuer(private val workManager: WorkManager) {

        fun enqueue(
            labelName: String,
            color: String,
            expanded: Int? = 0,
            type: LabelType,
            update: Boolean? = false,
            labelId: String? = null
        ): LiveData<WorkInfo> {

            val postLabelWorkerRequest = OneTimeWorkRequestBuilder<PostLabelWorker>()
                .setInputData(
                    workDataOf(
                        KEY_INPUT_DATA_LABEL_ID to labelId,
                        KEY_INPUT_DATA_LABEL_NAME to labelName,
                        KEY_INPUT_DATA_LABEL_COLOR to color,
                        KEY_INPUT_DATA_LABEL_TYPE to type.typeInt,
                        KEY_INPUT_DATA_IS_UPDATE to update,
                        KEY_INPUT_DATA_LABEL_EXPANDED to expanded
                    )
                ).build()

            workManager.enqueue(postLabelWorkerRequest)
            return workManager.getWorkInfoByIdLiveData(postLabelWorkerRequest.id)
        }
    }

}
