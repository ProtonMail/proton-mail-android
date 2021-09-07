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
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.data.local.model.LabelType
import ch.protonmail.android.labels.data.mapper.LabelEntityApiMapper
import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.data.remote.model.LabelRequestBody
import ch.protonmail.android.labels.data.remote.model.LabelResponse
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.Test

class CreateContactGroupWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var repository: ContactGroupsRepository

    @MockK
    private lateinit var labelsMapper: LabelEntityApiMapper

    @MockK
    private lateinit var accountManager: AccountManager

    private val labelResponse = mockk<LabelResponse> {
        every { label } returns LabelApiModel(
            id = "labelID",
            name = "name",
            color = "color",
            notify = 0,
            order = 0,
            type = LabelType.MESSAGE_LABEL,
            path = "a/b",
            parentId = "parentId",
            expanded = 0,
            sticky = 0
        )
    }

    private var dispatcherProvider = TestDispatcherProvider

    private val testUserId = UserId("TestUser")

    @InjectMockKs
    private lateinit var worker: CreateContactGroupWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        val labelEntity = LabelEntity(
            id = LabelId("labelID"),
            userId = testUserId,
            name = "name",
            color = "color",
            order = 0,
            type = LabelType.MESSAGE_LABEL,
            path = "a/b",
            parentId = "parentId",
            expanded = 0,
            sticky = 0,
            notify = 0
        )
        every { labelsMapper.toEntity(any(), testUserId) } returns labelEntity
    }

    @Test
    fun `worker succeeds when create contact group is performed without passing ID parameter`() {
        runBlockingTest {
            every {
                parameters.inputData.getBoolean(
                    KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE, false
                )
            } returns false
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_ID) } returns null
            val createContactGroupApiResponse = ApiResult.Success(labelResponse)
            coEvery { apiManager.createLabel(testUserId, any()) } returns createContactGroupApiResponse
            coEvery { apiManager.updateLabel(testUserId, any(), any()) } returns createContactGroupApiResponse

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun `worker fails when requested name parameter is not passed`() {
        runBlockingTest {
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_NAME) } returns null

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.failure(), result)
        }
    }

    @Test
    fun `worker fails when requested color parameter is not passed`() {
        runBlockingTest {
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_COLOR) } returns null

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.failure(), result)
        }
    }

    @Test
    fun workerSavesContactGroupInRepositoryWhenCreationSucceeds() {
        runBlockingTest {
            val createContactGroupApiResponse = ApiResult.Success(labelResponse)
            coEvery { apiManager.createLabel(testUserId, any()) } returns createContactGroupApiResponse

            val result = worker.doWork()

            coVerify { repository.saveContactGroup(any()) }
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun `worker invokes create contact group API when it's a create contact group request`() {
        runBlockingTest {
            every {
                parameters.inputData.getBoolean(
                    KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE, false
                )
            } returns false
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_NAME) } returns "labelName"
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_COLOR) } returns "labelColor"
            val createContactGroupApiResponse = ApiResult.Success(labelResponse)
            coEvery { apiManager.createLabel(testUserId, any()) } returns createContactGroupApiResponse
            coEvery { apiManager.updateLabel(testUserId, any(), any()) } returns createContactGroupApiResponse

            val result = worker.doWork()

            coVerify { apiManager.createLabel(testUserId, buildLabelBody()) }
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun `worker invokes update contact group API when it's a update request`() {
        runBlockingTest {
            every { parameters.inputData.getBoolean(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE, false) } returns true
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_NAME) } returns "labelName"
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_COLOR) } returns "labelColor"
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_ID) } returns "labelID"
            val createContactGroupApiResponse = ApiResult.Success(labelResponse)
            coEvery { apiManager.createLabel(testUserId, any()) } returns createContactGroupApiResponse
            coEvery { apiManager.updateLabel(testUserId, any(), any()) } returns createContactGroupApiResponse

            val result = worker.doWork()

            coVerify { apiManager.updateLabel(testUserId, "labelID", buildLabelBody()) }
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun `worker fails updating contact group when a valid ID is not passed`() {
        runBlockingTest {
            every { parameters.inputData.getBoolean(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE, false) } returns true
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_NAME) } returns "labelName"
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_COLOR) } returns "labelColor"
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_ID) } returns null

            val result = kotlin.runCatching {
                worker.doWork()
            }.exceptionOrNull()

            // Note that at runtime this exception will be wrapped into a WorkInfo.Result by the CoroutineWorker
            val expectedException = "Missing required ID parameter to create contact group"

            assertEquals(expectedException, result!!.message)
            assertTrue(result is IllegalArgumentException)
            coVerify { apiManager.updateLabel(testUserId, "labelID", buildLabelBody()) wasNot Called }
        }
    }

    @Test
    fun `worker fails returning error when API returns any errors`() {
        runBlockingTest {
            val error = "Test API Error"
            val protonErrorData = ApiResult.Error.ProtonData(123, error)
            val createContactGroupErrorApiResponse = ApiResult.Error.Http(123, error, protonErrorData)
            coEvery { apiManager.createLabel(any(), any()) } returns createContactGroupErrorApiResponse
            coEvery { apiManager.updateLabel(any(), any(), any()) } returns createContactGroupErrorApiResponse

            val result = worker.doWork()

            val expectedFailure = ListenableWorker.Result.failure(
                Data.Builder().putString(KEY_RESULT_DATA_CREATE_CONTACT_GROUP_ERROR, error).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun `worker fails returning error when API returns contactGroup with empty ID`() {
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
                Data.Builder().putString(KEY_RESULT_DATA_CREATE_CONTACT_GROUP_ERROR, error).build()
            )
            assertEquals(expectedFailure, result)
            coVerify(exactly = 0) { repository.saveContactGroup(any()) }
        }
    }

    private fun buildLabelBody(): LabelRequestBody =
        LabelRequestBody(
            name = "labelName",
            color = "labelColor",
            type = LabelType.CONTACT_GROUP.typeInt,
            parentId = EMPTY_STRING,
            notify = 0,
            expanded = 0,
            sticky = 0
        )

}

