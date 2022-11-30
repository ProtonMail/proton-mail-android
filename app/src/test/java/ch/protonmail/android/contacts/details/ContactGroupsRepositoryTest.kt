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
package ch.protonmail.android.contacts.details

import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.testAndroid.rx.TestSchedulerRule
import ch.protonmail.android.testdata.UserTestData.userId
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test

class ContactGroupsRepositoryTest {

    @get:Rule
    val testSchedulerRule = TestSchedulerRule()

    private val labelRepository: LabelRepository = mockk()
    private val accountManager: AccountManager = mockk()
    private val contactRepository: ContactsRepository = mockk()
    private val dispatchers = TestDispatcherProvider()

    private val contactGroupsRepository = ContactGroupsRepository(
        labelRepository = labelRepository,
        accountsManager = accountManager,
        contactRepository = contactRepository,
        dispatchers = dispatchers
    )

    private val label1 = Label(
        id = LabelId("a"),
        name = "aa",
        color = "testColor",
        order = 0,
        type = LabelType.MESSAGE_LABEL,
        path = "a/b",
        parentId = "parentId",
    )

    private val label1UiModel = ContactLabelUiModel(
        id = LabelId("a"),
        name = "aa",
        color = "testColor",
        type = LabelType.MESSAGE_LABEL,
        path = "a/b",
        parentId = "parentId",
        contactEmailsCount = 1
    )

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(userId)
    }

    @Test
    fun verifyThatDbAndApiContactsAreEmittedInOrder() {
        runTest(dispatchers.Main) {
            // given
            val dbContactsList = listOf(label1)
            val searchTerm = "Rob"
            coEvery { labelRepository.observeSearchContactGroups(searchTerm, userId) } returns flowOf(
                dbContactsList
            )
            coEvery { contactRepository.countContactEmailsByLabelId(userId, any()) } returns 1

            // when
            val result = contactGroupsRepository.observeContactGroups(userId, searchTerm).first()

            // then
            assertEquals(listOf(label1UiModel), result)
        }
    }

    @Test
    fun verifyThatDbAndApiContactsAreEmittedIn() {
        runTest(dispatchers.Main) {
            // given
            val dbContactsList = listOf(label1)
            val searchTerm = "Rob"
            coEvery { labelRepository.observeSearchContactGroups(searchTerm, userId) } returns flowOf(
                dbContactsList
            )
            coEvery { contactRepository.countContactEmailsByLabelId(userId, any()) } returns 1

            // when
            val result = contactGroupsRepository.observeContactGroups(userId, searchTerm).first()

            // then
            assertEquals(listOf(label1UiModel), result)
        }
    }

    @Test
    fun verifyThatDbContactsAreEmitted() {
        runTest(dispatchers.Main) {
            // given
            val searchTerm = "search"
            val dbContactsList = listOf(label1)
            coEvery { labelRepository.observeSearchContactGroups(searchTerm, userId) } returns flowOf(
                dbContactsList
            )
            coEvery { contactRepository.countContactEmailsByLabelId(userId, any()) } returns 1

            // when
            val result = contactGroupsRepository.observeContactGroups(userId, searchTerm).first()

            // then
            assertEquals(listOf(label1UiModel), result)
        }
    }

    @Test
    fun saveContactGroupStoresGivenContactGroupInDatabase() = runTest(dispatchers.Main) {
        val userId = UserId("testUserId")
        val contactGroup = Label(
            id = LabelId("Id"),
            name = "name",
            color = "color",
            order = 0,
            type = LabelType.MESSAGE_LABEL,
            path = "a/b",
            parentId = "parentId",
        )
        coEvery { labelRepository.saveLabel(any(), any()) } just Runs

        contactGroupsRepository.saveContactGroup(contactGroup, userId)

        coVerify { labelRepository.saveLabel(contactGroup, userId) }
    }

}
