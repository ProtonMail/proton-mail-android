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

package ch.protonmail.android.contacts.groups.edit

import androidx.work.WorkManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.contacts.receive.LabelsMapper
import ch.protonmail.android.api.models.messages.receive.Label
import ch.protonmail.android.api.models.messages.receive.LabelRequestBody
import ch.protonmail.android.api.models.messages.receive.LabelResponse
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.data.local.model.ContactLabelEntity
import ch.protonmail.android.worker.CreateContactGroupWorker
import com.birbit.android.jobqueue.JobManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ContactGroupEditCreateRepositoryTest {

    private val jobManager: JobManager = mockk(relaxed = true)

    private val workManager: WorkManager = mockk(relaxed = true)

    private val apiManager: ProtonMailApiManager = mockk(relaxed = true)

    private val contactDao: ContactDao = mockk(relaxed = true)

    private val labelsMapper: LabelsMapper = LabelsMapper()

    private val createContactGroupWorker: CreateContactGroupWorker.Enqueuer = mockk(relaxed = true)

    private val repository = ContactGroupEditCreateRepository(
        jobManager, workManager, apiManager, contactDao, labelsMapper, createContactGroupWorker
    )

    private val testLabel = Label(
        id = "labelID",
        name = "name",
        color = "color",
        path = "",
        type = 1,
        notify = 0,
        order = 0,
        expanded = null,
        sticky = null,
        display = 1,
        exclusive = null
    )

    private val testUserId = UserId("TestUserId")

    @Test
    fun whenEditContactGroupSucceedsThenSaveContactGroupAndContactEmailToTheDb() = runBlocking {
        val contactGroupId = "contact-group-id"
        val contactLabel = ContactLabelEntity(contactGroupId, "name", "color", 1, 0, false, 2)
        val labelResponse = LabelResponse(testLabel.copy(id = contactGroupId, type = 2))
        coEvery { apiManager.updateLabel(any(), any(), any()) } returns ApiResult.Success(labelResponse)
        val emailLabelJoinedList = listOf(ContactEmailContactLabelJoin("emailId", "labelId"))
        coEvery { contactDao.fetchJoins(contactGroupId) } returns emailLabelJoinedList
        val updateLabelRequest = LabelRequestBody(
            name = "name",
            color = "color",
            display = 1,
            exclusive = 0,
            type = 2,
            notify = 0,
            parentId = null
        )

        val apiResult = repository.editContactGroup(contactLabel, testUserId)

        assertNotNull(apiResult)
        coVerify { apiManager.updateLabel(testUserId, contactGroupId, updateLabelRequest) }
        coVerifyOrder {
            contactDao.fetchJoins(contactGroupId)
            contactDao.saveContactGroupLabel(contactLabel)
            contactDao.saveContactEmailContactLabel(emailLabelJoinedList)
        }
    }


    @Test
    fun whenEditContactGroupApiCallFailsThenCreateContactGroupWorkerIsCalled() = runBlocking {
        val contactGroupId = "contact-group-id"
        val contactLabel = ContactLabelEntity(contactGroupId, "name", "color")
        val error = "Test API Error"
        val protonErrorData = ApiResult.Error.ProtonData(123, error)
        val apiResponse = ApiResult.Error.Http(123, error, protonErrorData)
        coEvery { apiManager.updateLabel(any(), any(), any()) } returns apiResponse

        val apiResult = repository.editContactGroup(contactLabel, testUserId)

        assert(apiResult is ApiResult.Error)
        verify { createContactGroupWorker.enqueue("name", "color", 0, 0, true, contactGroupId) }
    }

    @Test
    fun whenCreateContactGroupIsCalledCreateLabelCompletableApiGetsCalledWithTheRequestObject() =
        runBlocking {
            val contactGroupId = "contact-group-id"
            val contactLabel = ContactLabelEntity(contactGroupId, "name", "color")
            val updateLabelRequest =  labelsMapper.mapContactLabelToRequestLabel(contactLabel)
            val labelResponse = LabelResponse(testLabel)
            coEvery { apiManager.createLabel(testUserId, any()) } returns ApiResult.Success(labelResponse)

            repository.createContactGroup(contactLabel, testUserId)

            coVerify { apiManager.createLabel(testUserId, updateLabelRequest) }
        }

    @Test
    fun `when createContactGroup succeeds then save contact group to the DB`() = runBlocking {
        val contactLabel = ContactLabelEntity("labelID", "name", "color", 1, 0, false, 1)
        val labelResponse = LabelResponse(testLabel)
        coEvery { apiManager.createLabel(testUserId, any()) } returns ApiResult.Success(labelResponse)

        val apiResult = repository.createContactGroup(contactLabel, testUserId)

        assertNotNull(apiResult)
        assertEquals(testLabel, (apiResult as? ApiResult.Success)?.valueOrThrow?.label)
        verify { contactDao.saveContactGroupLabel(contactLabel) }
    }

    @Test
    fun `when createContactGroup API call fails then createContactGroupWorker is called`() = runBlocking {
        val contactLabel = ContactLabelEntity("", "name", "color", 1, 0, false, 2)
        val error = "Test API Error"
        val protonErrorData = ApiResult.Error.ProtonData(123, error)
        val apiResponse = ApiResult.Error.Http(123, error, protonErrorData)
        coEvery { apiManager.createLabel(testUserId, any()) } returns apiResponse

        val apiResult = repository.createContactGroup(contactLabel, testUserId)

        assertNotNull(apiResult)
        assert(apiResult is ApiResult.Error)
        verify { createContactGroupWorker.enqueue("name", "color", 1, 0, false, "") }
    }
}
