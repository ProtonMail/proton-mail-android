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
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.data.mapper.LabelEntityApiMapper
import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.data.remote.model.LabelRequestBody
import ch.protonmail.android.labels.data.remote.model.LabelResponse
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostLabelWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var repository: LabelRepository

    @RelaxedMockK
    private lateinit var labelApiResponse: LabelResponse

    @RelaxedMockK
    private lateinit var accountManager: AccountManager

    private lateinit var worker: PostLabelWorker

    private val labelMapper = LabelEntityApiMapper()


    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(UserId("TestUserId"))
        coEvery { apiManager.createLabel(any(), any()) } returns ApiResult.Success(labelApiResponse)
        coEvery { apiManager.updateLabel(any(), any(), any()) } returns ApiResult.Success(labelApiResponse)
        every { labelApiResponse.label } returns LabelApiModel(
            id = "labelID",
            name = "name",
            color = "color",
            path = "",
            type = LabelType.MESSAGE_LABEL,
            notify = 0,
            order = 0,
            expanded = null,
            sticky = null,
        )

        worker = PostLabelWorker(
            context,
            parameters,
            apiManager,
            repository,
            labelMapper,
            accountManager
        )
    }

    @Test
    fun `worker succeeds when create label is performed without passing labelId parameter`() {
        runBlockingTest {
            every { parameters.inputData.getBoolean(KEY_INPUT_DATA_IS_UPDATE, false) } returns false
            every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_ID) } returns null

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun `worker fails when labelName parameter is not passed`() {
        runBlockingTest {
            every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_NAME) } returns null

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.failure(), result)
        }
    }

    @Test
    fun `worker fails when color parameter is not passed`() {
        runBlockingTest {
            every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_COLOR) } returns null

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.failure(), result)
        }
    }

    @Test
    fun `worker saves label in repository when creation succeeds`() {
        runBlockingTest {
            coEvery { apiManager.createLabel(any(), any()) } returns ApiResult.Success(labelApiResponse)

            val result = worker.doWork()

            coVerify { repository.saveLabel(any()) }
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun `worker invokes create label API when it's a create label request`() {
        runBlockingTest {
            every { parameters.inputData.getBoolean(KEY_INPUT_DATA_IS_UPDATE, false) } returns false
            every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_NAME) } returns "labelName"
            every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_COLOR) } returns "labelColor"
            every { parameters.inputData.getInt(KEY_INPUT_DATA_LABEL_TYPE, any()) } returns LabelType.CONTACT_GROUP.typeInt

            val result = worker.doWork()

            coVerify { apiManager.createLabel(any(), buildLabelBody()) }
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun `worker invokes update label API when it's a update label request`() {
        runBlockingTest {
            every { parameters.inputData.getBoolean(KEY_INPUT_DATA_IS_UPDATE, false) } returns true
            every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_NAME) } returns "labelName"
            every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_COLOR) } returns "labelColor"
            every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_ID) } returns "labelID"
            every { parameters.inputData.getInt(KEY_INPUT_DATA_LABEL_TYPE, any()) } returns LabelType.CONTACT_GROUP.typeInt

            val result = worker.doWork()

            coVerify { apiManager.updateLabel(any(), "labelID", buildLabelBody()) }
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun `worker fails when updating label without passing a valid labelID`() {
        runBlockingTest {
            every { parameters.inputData.getBoolean(KEY_INPUT_DATA_IS_UPDATE, false) } returns true
            every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_NAME) } returns "labelName"
            every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_COLOR) } returns "labelColor"
            every { parameters.inputData.getString(KEY_INPUT_DATA_LABEL_ID) } returns null

            val result = kotlin.runCatching {
                worker.doWork()
            }.exceptionOrNull()

            // Note that at runtime this exception will be wrapped into a WorkInfo.Result by the CoroutineWorker
            val expectedException = "Missing required LabelID parameter"

            assertEquals(expectedException, result!!.message)
            assertTrue(result is IllegalArgumentException)
            coVerify { apiManager.updateLabel(any(), "labelID", buildLabelBody()) wasNot Called }
        }
    }

    @Test
    fun `worker fails returning error when api returns any errors`() {
        runBlockingTest {
            val error = "Test API Error"
            val protonErrorData = ApiResult.Error.ProtonData(123, error)
            val createContactGroupErrorApiResponse = ApiResult.Error.Http(123, error, protonErrorData)
            coEvery { apiManager.createLabel(any(), any()) } returns createContactGroupErrorApiResponse
            coEvery { apiManager.updateLabel(any(), any(), any()) } returns createContactGroupErrorApiResponse

            val result = worker.doWork()

            val expectedFailure = ListenableWorker.Result.failure(
                Data.Builder().putString(KEY_POST_LABEL_WORKER_RESULT_ERROR, error).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun `worker fails returning error when api returns label with empty ID`() {
        runBlockingTest {
            val error = "Error, Label id is empty"
            val createContactGroupApiResponse = ApiResult.Success(
                LabelResponse(
                    LabelApiModel(
                        id = "",
                        name = "name",
                        color = "color",
                        path = "",
                        type = LabelType.MESSAGE_LABEL,
                        notify = 0,
                        order = 0,
                        expanded = null,
                        sticky = null
                    )
                )
            )

            coEvery { apiManager.createLabel(any(), any()) } returns createContactGroupApiResponse
            coEvery { apiManager.updateLabel(any(), any(), any()) } returns createContactGroupApiResponse
            val result = worker.doWork()

            val expectedFailure = ListenableWorker.Result.failure(
                Data.Builder().putString(KEY_POST_LABEL_WORKER_RESULT_ERROR, error).build()
            )
            assertEquals(expectedFailure, result)
            coVerify(exactly = 0) { repository.saveLabel(any()) }
        }
    }

    private fun buildLabelBody() =
        LabelRequestBody(
            name = "labelName",
            color = "labelColor",
            type = LabelType.CONTACT_GROUP.typeInt,
            parentId = null,
            notify = 0, // Constants.LABEL_TYPE_CONTACT_GROUPS,
            expanded = 0,
            sticky = 0
        )
}
