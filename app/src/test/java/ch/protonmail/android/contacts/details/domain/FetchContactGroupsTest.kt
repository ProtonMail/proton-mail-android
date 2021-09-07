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

package ch.protonmail.android.contacts.details.domain

import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.contacts.details.domain.model.FetchContactGroupsResult
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.data.local.model.LabelType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchContactGroupsTest : ArchTest, CoroutinesTest {

    private val repository: ContactDetailsRepository = mockk()

    private val useCase = FetchContactGroups(
        repository, dispatchers
    )

    private val testUserId = UserId("testUserId")

    @Test
    fun verifyStandardCaseJustOneEmissionOfTwoValues() = runBlockingTest {

        // given
        val contactId = "ContactId1"
        val contactEmailId1 = "ContactEmailId1"
        val contactEmailId2 = "ContactEmailId2"
        val labelId1 = LabelId("labelId1")
        val labelId2 = LabelId("labelId2")
        val labelIds = listOf(labelId1.id)
        val contactEmail1 = ContactEmail(contactEmailId1, "test1@abc.com", "name1", labelIds = labelIds)
        val contactEmail2 = ContactEmail(contactEmailId2, "test2@abc.com", "name1", labelIds = labelIds)
        val list1 = listOf(contactEmail1, contactEmail2)
        val contactLabel1 =
            LabelEntity(
                labelId1, testUserId, "name1", "color1", 1,  LabelType.MESSAGE_LABEL, "a/b", "parentId", 0, 0,
                0
            )
        val contactLabel2 =
            LabelEntity(
                labelId2, testUserId, "name2", "color2", 1,  LabelType.MESSAGE_LABEL, "a/b", "parentId", 0, 0,
                0
            )
        every { repository.observeContactEmails(contactId) } returns flowOf(list1)
        coEvery { repository.getContactGroupsLabelForId(contactEmailId1) } returns listOf(contactLabel1)
        coEvery { repository.getContactGroupsLabelForId(contactEmailId2) } returns listOf(contactLabel2)
        val expected = FetchContactGroupsResult(
            listOf(contactLabel1, contactLabel2)
        )

        // when
        val result = useCase.invoke(contactId).take(1).toList()

        // then
        assertEquals(expected, result[0])
    }

    @Test
    fun verifyStandardCaseTwoEmissionsOfThreeValuesInTotal() = runBlockingTest {

        // given
        val contactId = "ContactId1"
        val contactEmailId1 = "ContactEmailId1"
        val contactEmailId2 = "ContactEmailId2"
        val contactEmailId3 = "ContactEmailId3"
        val labelId1 = LabelId("labelId1")
        val labelId2 = LabelId("labelId2")
        val labelId3 = LabelId("labelId3")
        val labelIds = listOf(labelId1.id)
        val contactEmail1 = ContactEmail(contactEmailId1, "test1@abc.com", "name1", labelIds = labelIds)
        val contactEmail2 = ContactEmail(contactEmailId2, "test2@abc.com", "name2", labelIds = labelIds)
        val contactEmail3 = ContactEmail(contactEmailId3, "test3@abc.com", "name3", labelIds = labelIds)
        val list1 = listOf(contactEmail1, contactEmail2)
        val list2 = listOf(contactEmail3)
        val contactLabel1 =
            LabelEntity(
                labelId1, testUserId, "name1", "color1", 1,  LabelType.MESSAGE_LABEL, "a/b", "parentId", 0, 0,
                0
            )
        val contactLabel2 =
            LabelEntity(
                labelId2, testUserId, "name2", "color2", 1,  LabelType.MESSAGE_LABEL, "a/b", "parentId", 0, 0,
                0
            )
        val contactLabel3 =
            LabelEntity(
                labelId3, testUserId, "name3", "color3", 1,  LabelType.MESSAGE_LABEL, "a/b", "parentId", 0, 0,
                0
            )
        every { repository.observeContactEmails(contactId) } returns flow {
            emit(list1)
            emit(list2)
        }
        coEvery { repository.getContactGroupsLabelForId(contactEmailId1) } returns listOf(contactLabel1)
        coEvery { repository.getContactGroupsLabelForId(contactEmailId2) } returns listOf(contactLabel2)
        coEvery { repository.getContactGroupsLabelForId(contactEmailId3) } returns listOf(contactLabel3)
        val expected1 = FetchContactGroupsResult(
            listOf(contactLabel1, contactLabel2)
        )
        val expected2 = FetchContactGroupsResult(
            listOf(contactLabel3)
        )

        // when
        val result = useCase.invoke(contactId).take(2).toList()

        // then
        assertEquals(expected1, result[0])
        assertEquals(expected2, result[1])
    }
}
