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

package ch.protonmail.android.contacts.details.domain

import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.contacts.details.domain.model.FetchContactGroupsResult
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.testdata.UserTestData.userId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchContactGroupsTest : ArchTest by ArchTest(),
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private val repository: ContactDetailsRepository = mockk()

    private lateinit var useCase: FetchContactGroups

    @BeforeTest
    fun setUp() {
        useCase = FetchContactGroups(repository, dispatchers)
    }

    @Test
    fun verifyStandardCaseJustOneEmissionOfTwoValues() = runTest(dispatchers.Main) {

        // given
        val contactId = "ContactId1"
        val contactEmailId1 = "ContactEmailId1"
        val contactEmailId2 = "ContactEmailId2"
        val labelId1 = LabelId("labelId1")
        val labelId2 = LabelId("labelId2")
        val labelIds = listOf(labelId1.id)
        val contactEmail1 =
            ContactEmail(contactEmailId1, "test1@abc.com", "name1", labelIds = labelIds, lastUsedTime = 111)
        val contactEmail2 =
            ContactEmail(contactEmailId2, "test2@abc.com", "name1", labelIds = labelIds, lastUsedTime = 112)
        val list1 = listOf(contactEmail1, contactEmail2)
        val contactLabel1 =
            Label(
                labelId1, "name1", "color1", 0, LabelType.MESSAGE_LABEL, "a/b", "parentId"
            )
        val contactLabel2 =
            Label(
                labelId2, "name2", "color2", 0, LabelType.MESSAGE_LABEL, "a/b", "parentId"
            )
        every { repository.observeContactEmails(contactId) } returns flowOf(list1)
        coEvery { repository.getContactGroupsLabelForId(userId, contactEmailId1) } returns listOf(contactLabel1)
        coEvery { repository.getContactGroupsLabelForId(userId, contactEmailId2) } returns listOf(contactLabel2)
        val expected = FetchContactGroupsResult(
            listOf(contactLabel1, contactLabel2)
        )

        // when
        val result = useCase.invoke(userId, contactId).take(1).toList()

        // then
        assertEquals(expected, result[0])
    }

    @Test
    fun verifyStandardCaseTwoEmissionsOfThreeValuesInTotal() = runTest(dispatchers.Main) {

        // given
        val contactId = "ContactId1"
        val contactEmailId1 = "ContactEmailId1"
        val contactEmailId2 = "ContactEmailId2"
        val contactEmailId3 = "ContactEmailId3"
        val labelId1 = LabelId("labelId1")
        val labelId2 = LabelId("labelId2")
        val labelId3 = LabelId("labelId3")
        val labelIds = listOf(labelId1.id)
        val contactEmail1 =
            ContactEmail(contactEmailId1, "test1@abc.com", "name1", labelIds = labelIds, lastUsedTime = 111)
        val contactEmail2 =
            ContactEmail(contactEmailId2, "test2@abc.com", "name2", labelIds = labelIds, lastUsedTime = 113)
        val contactEmail3 =
            ContactEmail(contactEmailId3, "test3@abc.com", "name3", labelIds = labelIds, lastUsedTime = 112)
        val list1 = listOf(contactEmail1, contactEmail2)
        val list2 = listOf(contactEmail3)
        val contactLabel1 =
            Label(
                labelId1, "name1", "color1", 0, LabelType.MESSAGE_LABEL, "a/b", "parentId"
            )
        val contactLabel2 =
            Label(
                labelId2, "name2", "color2", 0, LabelType.MESSAGE_LABEL, "a/b", "parentId"
            )
        val contactLabel3 =
            Label(
                labelId3, "name3", "color3", 0, LabelType.MESSAGE_LABEL, "a/b", "parentId"
            )
        every { repository.observeContactEmails(contactId) } returns flow {
            emit(list1)
            emit(list2)
        }
        coEvery { repository.getContactGroupsLabelForId(userId, contactEmailId1) } returns listOf(contactLabel1)
        coEvery { repository.getContactGroupsLabelForId(userId, contactEmailId2) } returns listOf(contactLabel2)
        coEvery { repository.getContactGroupsLabelForId(userId, contactEmailId3) } returns listOf(contactLabel3)
        val expected1 = FetchContactGroupsResult(
            listOf(contactLabel1, contactLabel2)
        )
        val expected2 = FetchContactGroupsResult(
            listOf(contactLabel3)
        )

        // when
        val result = useCase.invoke(userId, contactId).take(2).toList()

        // then
        assertEquals(expected1, result[0])
        assertEquals(expected2, result[1])
    }
}
