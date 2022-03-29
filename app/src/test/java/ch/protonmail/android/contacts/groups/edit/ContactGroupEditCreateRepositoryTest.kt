/*
 * Copyright (c) 2022 Proton Technologies AG
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
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.data.mapper.LabelEntityApiMapper
import ch.protonmail.android.labels.data.mapper.LabelEntityDomainMapper
import ch.protonmail.android.labels.data.mapper.LabelRequestMapper
import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.data.remote.model.LabelRequestBody
import ch.protonmail.android.labels.data.remote.model.LabelResponse
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.testdata.UserIdTestData.userId
import ch.protonmail.android.worker.CreateContactGroupWorker
import com.birbit.android.jobqueue.JobManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ContactGroupEditCreateRepositoryTest {

    private val jobManager: JobManager = mockk(relaxed = true)

    private val workManager: WorkManager = mockk(relaxed = true)

    private val apiManager: ProtonMailApiManager = mockk(relaxed = true)

    private val contactsRepository: ContactsRepository = mockk(relaxed = true)

    private val labelsMapper: LabelEntityApiMapper = LabelEntityApiMapper()

    private val labelDomainMapper = LabelEntityDomainMapper()

    private val requestMapper: LabelRequestMapper = LabelRequestMapper()

    private val labelRepository: LabelRepository = mockk()

    private val createContactGroupWorker: CreateContactGroupWorker.Enqueuer = mockk(relaxed = true)

    private val repository = ContactGroupEditCreateRepository(
        jobManager, workManager, apiManager, contactsRepository, labelsMapper, labelDomainMapper,
        createContactGroupWorker, labelRepository, requestMapper
    )

    private val testPath = "a/bpath"
    private val testParentId = "parentIdForTests"
    private val testType = LabelType.CONTACT_GROUP

    private val testLabel = LabelApiModel(
        id = "labelID",
        name = "name",
        color = "color",
        path = testPath,
        type = testType,
        notify = 0,
        order = 1,
        expanded = null,
        sticky = null,
        parentId = testParentId
    )

    private val testUserId = UserId("TestUserId")

    @Test
    fun whenEditContactGroupSucceedsThenSaveContactGroupAndContactEmailToTheDb() = runBlockingTest {
        // given
        val contactGroupId = "contact-group-id"
        val contactLabel = Label(
            LabelId(contactGroupId), "name", "color", 1, testType, testPath, testParentId
        )

        val labelResponse =
            LabelResponse(testLabel.copy(id = contactGroupId, type = LabelType.CONTACT_GROUP))
        coEvery { apiManager.updateLabel(any(), any(), any()) } returns ApiResult.Success(labelResponse)
        coEvery { labelRepository.saveLabel(contactLabel, testUserId) } just Runs
        val updateLabelRequest = LabelRequestBody(
            name = "name",
            color = "color",
            type = testType.typeInt,
            notify = null,
            parentId = testParentId,
            expanded = null,
            sticky = null
        )

        // when
        val apiResult = repository.editContactGroup(contactLabel, testUserId)

        // then
        assertNotNull(apiResult)
        coVerify { apiManager.updateLabel(testUserId, contactGroupId, updateLabelRequest) }
        coVerify {
            labelRepository.saveLabel(contactLabel, testUserId)
        }
    }


    @Test
    fun whenEditContactGroupApiCallFailsThenCreateContactGroupWorkerIsCalled() = runBlockingTest {
        val contactGroupId = "contact-group-id"
        val contactLabel = Label(
            LabelId(contactGroupId),
            "name",
            "color",
            0,
            testType,
            testPath,
            testParentId
        )
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
        runBlockingTest {
            val contactGroupId = "contact-group-id"
            val contactLabel = Label(
                LabelId(contactGroupId),
                "name",
                "color",
                0,
                testType,
                testPath,
                testParentId,
            )
            val updateLabelRequest = requestMapper.toRequest(contactLabel)
            val labelResponse = LabelResponse(testLabel)
            coEvery { apiManager.createLabel(testUserId, any()) } returns ApiResult.Success(labelResponse)
            coEvery { labelRepository.saveLabel(any(), testUserId) } just Runs

            repository.createContactGroup(contactLabel, testUserId)

            coVerify { apiManager.createLabel(testUserId, updateLabelRequest) }
        }

    @Test
    fun whenCreateContactGroupSucceedsThenSaveContactGroupToTheDb() = runBlockingTest {
        // given
        val contactLabel =
            Label(
                LabelId("labelID"),
                "name",
                "color",
                1,
                LabelType.CONTACT_GROUP,
                testPath,
                testParentId,
            )
        val labelResponse = LabelResponse(testLabel)
        coEvery { apiManager.createLabel(testUserId, any()) } returns ApiResult.Success(labelResponse)
        coEvery { labelRepository.saveLabel(contactLabel, testUserId) } just Runs

        // when
        val apiResult = repository.createContactGroup(contactLabel, testUserId)

        // then
        assertNotNull(apiResult)
        assertEquals(testLabel, (apiResult as? ApiResult.Success)?.valueOrThrow?.label)
        coVerify { labelRepository.saveLabel(contactLabel, testUserId) }
    }

    @Test
    fun whenCreateContactGroupApiCallFailsThenCreateContactGroupWorkerIsCalled() = runBlockingTest {
        val contactLabel =
            Label(
                LabelId(""), "name", "color", 0, LabelType.CONTACT_GROUP, "a/b", "ParentId"
            )
        val error = "Test API Error"
        val protonErrorData = ApiResult.Error.ProtonData(123, error)
        val apiResponse = ApiResult.Error.Http(123, error, protonErrorData)
        coEvery { apiManager.createLabel(testUserId, any()) } returns apiResponse

        val apiResult = repository.createContactGroup(contactLabel, testUserId)

        assertNotNull(apiResult)
        assert(apiResult is ApiResult.Error)
        verify { createContactGroupWorker.enqueue("name", "color", 0, 0, false, "") }
    }

    @Test
    fun verifyThatRemovingMembersEmailsFromGroupsWorks() = runBlockingTest {
        // given
        val testGroupName = "testGroupName"
        val testMember1 = "one@member.com"
        val testLabel1Id = "testLabel1Id"
        val testLabel2Id = "testLabel2Id"
        val testLabel3Id = "testLabel3Id"
        val testGroupId = testLabel1Id
        val membersToRemove = listOf(testMember1)
        val inputContactEmail = ContactEmail(
            contactEmailId = testMember1,
            email = "firstsender@protonmail.com",
            name = "firstContactName",
            labelIds = listOf(testLabel1Id, testLabel2Id, testLabel3Id)
        )
        val inputContactEmails = listOf(inputContactEmail)
        coEvery { contactsRepository.findAllContactEmailsByContactGroupId(userId, testGroupId) } returns inputContactEmails

        // when
        repository.removeMembersFromContactGroup(
            userId,
            testGroupId,
            testGroupName,
            membersToRemove
        )

        // then
        coVerify { apiManager.unlabelContactEmails(any()) }
        coVerify {
            contactsRepository.saveContactEmail(
                userId,
                inputContactEmail.copy(labelIds = listOf(testLabel2Id, testLabel3Id))
            )
        }
    }

    @Test
    fun verifyThatAddingMembersEmailsToGroupsWorks() = runBlockingTest {
        // given
        val testGroupName = "testGroupName"
        val testMember1 = "one@member.com"
        val testLabel1Id = "testLabel1Id"
        val testLabel2Id = "testLabel2Id"
        val testLabel3Id = "testLabel3Id"
        val testGroupId = testLabel3Id
        val membersToAdd = listOf(testMember1)
        val existingContactEmail = ContactEmail(
            contactEmailId = testMember1,
            email = "firstsender@protonmail.com",
            name = "firstContactName",
            labelIds = listOf(testLabel1Id, testLabel2Id)
        )
        coEvery { contactsRepository.findAllContactEmailsById(userId, testMember1) } returns existingContactEmail

        // when
        repository.setMembersForContactGroup(
            userId,
            testGroupId,
            testGroupName,
            membersToAdd
        )

        // then
        coVerify { apiManager.labelContacts(any()) }
        coVerify {
            contactsRepository.saveContactEmail(
                userId,
                existingContactEmail.copy(labelIds = listOf(testLabel1Id, testLabel2Id, testLabel3Id))
            )
        }
    }
}
