/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.api.segments.contact

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEmailsResponseV2
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.data.mapper.LabelEntityApiMapper
import ch.protonmail.android.labels.data.mapper.LabelEntityDomainMapper
import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.data.remote.model.LabelsResponse
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ContactEmailsManagerTest : CoroutinesTest by CoroutinesTest(), ArchTest by ArchTest() {

    private lateinit var manager: ContactEmailsManager

    @MockK
    private lateinit var api: ProtonMailApiManager

    @MockK
    private lateinit var dbProvider: DatabaseProvider

    @MockK
    private lateinit var accountManager: AccountManager

    @MockK
    private lateinit var contactDao: ContactDao

    @MockK
    private lateinit var labelsMapper: LabelEntityApiMapper

    @MockK
    private lateinit var labelRepository: LabelRepository

    private val labelsDomainMapper = LabelEntityDomainMapper()

    private val testUserId = UserId("TestUser")
    private val testPath = "a/bcPath"
    private val testParentId = "parentIdForTests"
    private val testType = LabelType.MESSAGE_LABEL

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        every { dbProvider.provideContactDao(any()) } returns contactDao
        manager =
            ContactEmailsManager(api, dbProvider, accountManager, labelsMapper, labelsDomainMapper, labelRepository)
    }

    @Test
    fun verifyThatCanFetchAndUpdateDbWithFreshContactsDataWithJustOnePage() = runBlockingTest {
        // given
        val pageSize = Constants.CONTACTS_PAGE_SIZE
        val labelId1 = "labelId1"
        val labelName1 = "labelName1"
        val labelColor = "labelColor11"
        val label = LabelApiModel(
            id = labelId1,
            name = labelName1,
            path = testPath,
            color = labelColor,
            type = testType,
            notify = 0,
            order = 0,
            expanded = null,
            sticky = null,
            parentId = testParentId
        )
        val labelList = listOf(label)
        val apiResult = ApiResult.Success(LabelsResponse(labelList))
        val contactLabel = LabelEntity(
            id = LabelId(labelId1),
            userId = testUserId,
            name = labelName1,
            color = labelColor,
            order = 0,
            type = testType,
            parentId = testParentId,
            path = testPath,
            expanded = 0,
            sticky = 0,
            notify = 0,
        )
        val contactEmailId = "emailId1"
        val labelIds = listOf(labelId1)
        val contactEmail =
            ContactEmail(contactEmailId, "test1@abc.com", "name1", labelIds = labelIds, lastUsedTime = 111)
        val newContactEmails = listOf(contactEmail)
        coEvery { api.getContactGroups(testUserId) } returns apiResult
        val emailsResponse = mockk<ContactEmailsResponseV2> {
            every { contactEmails } returns newContactEmails
            every { total } returns 0
        }
        coEvery { api.fetchContactEmails(any(), pageSize) } returns emailsResponse
        coEvery { labelRepository.saveLabels(any(), any()) } returns Unit
        coEvery { contactDao.insertNewContacts(newContactEmails) } returns Unit
        every { labelsMapper.toEntity(any(), testUserId) } returns contactLabel

        // when
        manager.refresh(pageSize)

        // then
        coVerify { contactDao.insertNewContacts(newContactEmails) }
    }

    @Test
    fun verifyThatCanFetchAndUpdateDbWithFreshContactsDataWithJustTwoPages() = runBlockingTest {
        // given
        val pageSize = 2
        val labelId1 = "labelId1"
        val labelName1 = "labelName1"
        val labelColor = "labelColor11"
        val label = LabelApiModel(
            id = labelId1,
            name = labelName1,
            path = "",
            color = labelColor,
            type = LabelType.MESSAGE_LABEL,
            notify = 0,
            order = 0,
            expanded = null,
            sticky = null
        )
        val labelList = listOf(label)
        val contactLabel = LabelEntity(
            id = LabelId(labelId1),
            userId = testUserId,
            name = labelName1,
            color = labelColor,
            order = 0,
            type = testType,
            expanded = 0,
            parentId = testParentId,
            path = testPath,
            sticky = 0,
            notify = 0
        )
        val apiResult = ApiResult.Success(LabelsResponse(labelList))
        val contactEmailId1 = "emailId1"
        val contactEmailId2 = "emailId2"
        val contactEmailId3 = "emailId3"
        val contactEmailId4 = "emailId4"
        val contactEmailId5 = "emailId5"
        val labelIds = listOf(labelId1)
        val contactEmail1 =
            ContactEmail(contactEmailId1, "test1@abc.com", "name1", labelIds = labelIds, lastUsedTime = 111)
        val contactEmail2 =
            ContactEmail(contactEmailId2, "test2@abc.com", "name2", labelIds = labelIds, lastUsedTime = 113)
        val contactEmail3 =
            ContactEmail(contactEmailId3, "test3@abc.com", "name3", labelIds = labelIds, lastUsedTime = 115)
        val contactEmail4 =
            ContactEmail(contactEmailId4, "test4@abc.com", "name4", labelIds = labelIds, lastUsedTime = 114)
        val contactEmail5 =
            ContactEmail(contactEmailId5, "test5@abc.com", "name5", labelIds = labelIds, lastUsedTime = 112)
        val newContactEmails1 = listOf(contactEmail1, contactEmail2)
        val newContactEmails2 = listOf(contactEmail3, contactEmail4)
        val newContactEmails3 = listOf(contactEmail5)
        val allContactEmails = listOf(contactEmail1, contactEmail2, contactEmail3, contactEmail4, contactEmail5)
        coEvery { api.getContactGroups(any()) } returns apiResult
        val emailsResponse1 = mockk<ContactEmailsResponseV2> {
            every { contactEmails } returns newContactEmails1
            every { total } returns allContactEmails.size
        }
        val emailsResponse2 = mockk<ContactEmailsResponseV2> {
            every { contactEmails } returns newContactEmails2
            every { total } returns allContactEmails.size
        }
        val emailsResponse3 = mockk<ContactEmailsResponseV2> {
            every { contactEmails } returns newContactEmails3
            every { total } returns allContactEmails.size
        }
        coEvery { api.fetchContactEmails(0, pageSize) } returns emailsResponse1
        coEvery { api.fetchContactEmails(1, pageSize) } returns emailsResponse2
        coEvery { api.fetchContactEmails(2, pageSize) } returns emailsResponse3
        coEvery { labelRepository.saveLabels(any(), any()) } returns Unit
        coEvery { contactDao.insertNewContacts(allContactEmails) } returns Unit
        every { labelsMapper.toEntity(any(), testUserId) } returns contactLabel

        // when
        manager.refresh(pageSize)

        // then
        coVerify { contactDao.insertNewContacts(allContactEmails) }
    }

}
