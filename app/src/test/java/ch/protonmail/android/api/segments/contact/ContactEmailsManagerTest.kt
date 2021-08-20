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

package ch.protonmail.android.api.segments.contact

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEmailsResponseV2
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.contacts.receive.ContactGroupsResponse
import ch.protonmail.android.api.models.contacts.receive.LabelsMapper
import ch.protonmail.android.api.models.messages.receive.Label
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.data.local.model.LabelEntity
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

class ContactEmailsManagerTest : CoroutinesTest, ArchTest {

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
    private lateinit var labelsMapper: LabelsMapper

    private val testUserId = UserId("TestUser")
    private val testPath = "a/bcPath"
    private val testParentId = "parentIdForTests"
    private val testType = Constants.LABEL_TYPE_MESSAGE_LABEL

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        every { dbProvider.provideContactDao(any()) } returns contactDao
        manager = ContactEmailsManager(api, dbProvider, accountManager, labelsMapper)
    }

    @Test
    fun verifyThatCanFetchAndUpdateDbWithFreshContactsDataWithJustOnePage() = runBlockingTest {
        // given
        val pageSize = Constants.CONTACTS_PAGE_SIZE
        val labelId1 = "labelId1"
        val labelName1 = "labelName1"
        val labelColor = "labelColor11"
        val label = Label(
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
        val apiResult = ApiResult.Success(ContactGroupsResponse(labelList))
        val contactLabel = LabelEntity(
            id = labelId1,
            name = labelName1,
            color = labelColor,
            display = 0,
            order = 0,
            exclusive = false,
            type = testType,
            notify = 0,
            parentId = testParentId,
            path = testPath,
            expanded = 0,
            sticky = 0
        )
        val contactLabelList = listOf(contactLabel)
        val contactEmailId = "emailId1"
        val labelIds = listOf(labelId1)
        val contactEmail = ContactEmail(contactEmailId, "test1@abc.com", "name1", labelIds = labelIds)
        val newContactEmails = listOf(contactEmail)
        val join1 = ContactEmailContactLabelJoin(contactEmailId, labelId1)
        val newJoins = listOf(join1)
        coEvery { api.fetchContactGroups(testUserId) } returns apiResult
        val emailsResponse = mockk<ContactEmailsResponseV2> {
            every { contactEmails } returns newContactEmails
            every { total } returns 0
        }
        coEvery { api.fetchContactEmails(any(), pageSize) } returns emailsResponse
        coEvery { contactDao.insertNewContactsAndLabels(newContactEmails, any(), newJoins) } returns Unit
        every { labelsMapper.mapLabelToLabelEntity(any()) } returns contactLabel

        // when
        manager.refresh(pageSize)

        // then
        coVerify { contactDao.insertNewContactsAndLabels(newContactEmails, contactLabelList, newJoins) }
    }

    @Test
    fun verifyThatCanFetchAndUpdateDbWithFreshContactsDataWithJustTwoPages() = runBlockingTest {
        // given
        val pageSize = 2
        val labelId1 = "labelId1"
        val labelName1 = "labelName1"
        val labelColor = "labelColor11"
        val label = Label(
            id = labelId1,
            name = labelName1,
            path = "",
            color = labelColor,
            type = Constants.LABEL_TYPE_MESSAGE_LABEL,
            notify = 0,
            order = 0,
            expanded = null,
            sticky = null
        )
        val labelList = listOf(label)
        val contactLabel = LabelEntity(
            id = labelId1,
            name = labelName1,
            color = labelColor,
            display = 0,
            order = 0,
            exclusive = false,
            type = testType,
            expanded = 0,
            parentId = testParentId,
            path = testPath,
            sticky = 0,
            notify = 0
        )
        val contactLabelList = listOf(contactLabel)
        val apiResult = ApiResult.Success(ContactGroupsResponse(labelList))
        val contactEmailId1 = "emailId1"
        val contactEmailId2 = "emailId2"
        val contactEmailId3 = "emailId3"
        val contactEmailId4 = "emailId4"
        val contactEmailId5 = "emailId5"
        val labelIds = listOf(labelId1)
        val contactEmail1 = ContactEmail(contactEmailId1, "test1@abc.com", "name1", labelIds = labelIds)
        val contactEmail2 = ContactEmail(contactEmailId2, "test2@abc.com", "name2", labelIds = labelIds)
        val contactEmail3 = ContactEmail(contactEmailId3, "test3@abc.com", "name3", labelIds = labelIds)
        val contactEmail4 = ContactEmail(contactEmailId4, "test4@abc.com", "name4", labelIds = labelIds)
        val contactEmail5 = ContactEmail(contactEmailId5, "test5@abc.com", "name5", labelIds = labelIds)
        val newContactEmails1 = listOf(contactEmail1, contactEmail2)
        val newContactEmails2 = listOf(contactEmail3, contactEmail4)
        val newContactEmails3 = listOf(contactEmail5)
        val join1 = ContactEmailContactLabelJoin(contactEmailId1, labelId1)
        val join2 = ContactEmailContactLabelJoin(contactEmailId2, labelId1)
        val join3 = ContactEmailContactLabelJoin(contactEmailId3, labelId1)
        val join4 = ContactEmailContactLabelJoin(contactEmailId4, labelId1)
        val join5 = ContactEmailContactLabelJoin(contactEmailId5, labelId1)
        val allContactEmails = listOf(contactEmail1, contactEmail2, contactEmail3, contactEmail4, contactEmail5)
        val newJoins = listOf(join1, join2, join3, join4, join5)
        coEvery { api.fetchContactGroups(any()) } returns apiResult
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
        coEvery { contactDao.insertNewContactsAndLabels(allContactEmails, any(), newJoins) } returns Unit
        every { labelsMapper.mapLabelToLabelEntity(any()) } returns contactLabel

        // when
        manager.refresh(pageSize)

        // then
        coVerify { contactDao.insertNewContactsAndLabels(allContactEmails, contactLabelList, newJoins) }
    }

}
