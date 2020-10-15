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
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.core.Constants
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateContactGroupWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var repository: ContactGroupsRepository

    @RelaxedMockK
    private lateinit var createContactGroupApiResponse: LabelResponse

    private var dispatcherProvider = TestDispatcherProvider

    @InjectMockKs
    private lateinit var worker: CreateContactGroupWorker

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { apiManager.createLabel(any()) } returns createContactGroupApiResponse
        every { apiManager.updateLabel(any(), any()) } returns createContactGroupApiResponse
        every { createContactGroupApiResponse.contactGroup } returns ContactLabel("labelID", "name", "color")
    }

    @Test
    fun `worker succeeds when create contact group is performed without passing ID parameter`() {
        runBlockingTest {
            every { parameters.inputData.getBoolean(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE, false) } returns false
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_ID) } returns null

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
    fun `worker saves contact group in repository when creation succeeds`() {
        runBlockingTest {
            every { createContactGroupApiResponse.hasError() } returns false
            every { apiManager.createLabel(any()) } returns createContactGroupApiResponse

            val result = worker.doWork()

            coVerify { repository.saveContactGroup(any()) }
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun `worker invokes create contact group API when it's a create contact group request`() {
        runBlockingTest {
            every { parameters.inputData.getBoolean(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_IS_UPDATE, false) } returns false
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_NAME) } returns "labelName"
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_GROUP_COLOR) } returns "labelColor"

            val result = worker.doWork()

            verify { apiManager.createLabel(buildLabelBody()) }
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

            val result = worker.doWork()

            verify { apiManager.updateLabel("labelID", buildLabelBody()) }
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
            verify { apiManager.updateLabel("labelID", buildLabelBody()) wasNot Called }
        }
    }

    @Test
    fun `worker fails returning error when API returns any errors`() {
        runBlockingTest {
            val error = "Test API Error"
            every { createContactGroupApiResponse.hasError() } returns true
            every { createContactGroupApiResponse.error } returns error

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
            val error = "Test API Error"
            every { createContactGroupApiResponse.hasError() } returns false
            every { createContactGroupApiResponse.contactGroup.ID } returns ""
            every { createContactGroupApiResponse.error } returns error

            val result = worker.doWork()

            val expectedFailure = ListenableWorker.Result.failure(
                Data.Builder().putString(KEY_RESULT_DATA_CREATE_CONTACT_GROUP_ERROR, error).build()
            )
            assertEquals(expectedFailure, result)
            coVerify(exactly = 0) { repository.saveContactGroup(any()) }
        }
    }

    private fun buildLabelBody(): LabelBody =
        LabelBody(
            "labelName",
            "labelColor",
            0,
            0,
            Constants.LABEL_TYPE_CONTACT_GROUPS
        )

}

