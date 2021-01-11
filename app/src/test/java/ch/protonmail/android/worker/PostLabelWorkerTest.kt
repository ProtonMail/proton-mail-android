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
import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.models.messages.receive.LabelResponse
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.data.LabelRepository
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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

    private lateinit var worker: PostLabelWorker

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { apiManager.createLabel(any()) } returns labelApiResponse
        every { apiManager.updateLabel(any(), any()) } returns labelApiResponse
        every { labelApiResponse.label } returns Label("labelID", "name", "color")

        worker = PostLabelWorker(
            context,
            parameters,
            apiManager,
            repository
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
            every { labelApiResponse.hasError() } returns false
            every { apiManager.createLabel(any()) } returns labelApiResponse

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
            val labelBody = LabelBody("labelName", "labelColor", 0, 0)

            val result = worker.doWork()

            verify { apiManager.createLabel(labelBody) }
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
            val labelBody = LabelBody("labelName", "labelColor", 0, 0)

            val result = worker.doWork()

            verify { apiManager.updateLabel("labelID", labelBody) }
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
            val labelBody = LabelBody("labelName", "labelColor", 0, 0)

            val result = kotlin.runCatching {
                worker.doWork()
            }.exceptionOrNull()

            // Note that at runtime this exception will be wrapped into a WorkInfo.Result by the CoroutineWorker
            val expectedException = "Missing required LabelID parameter"

            assertEquals(expectedException, result!!.message)
            assertTrue(result is IllegalArgumentException)
            verify { apiManager.updateLabel("labelID", labelBody) wasNot Called }
        }
    }

    @Test
    fun `worker fails returning error when api returns any errors`() {
        runBlockingTest {
            val error = "Test API Error"
            every { labelApiResponse.hasError() } returns true
            every { labelApiResponse.error } returns error

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
            val error = "Test API Error"
            every { labelApiResponse.hasError() } returns false
            every { labelApiResponse.label.id } returns ""
            every { labelApiResponse.error } returns error

            val result = worker.doWork()

            val expectedFailure = ListenableWorker.Result.failure(
                Data.Builder().putString(KEY_POST_LABEL_WORKER_RESULT_ERROR, error).build()
            )
            assertEquals(expectedFailure, result)
            coVerify(exactly = 0) { repository.saveLabel(any()) }
        }
    }
}
