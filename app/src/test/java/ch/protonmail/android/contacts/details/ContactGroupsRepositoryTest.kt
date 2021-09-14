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
package ch.protonmail.android.contacts.details

import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.testAndroid.rx.TestSchedulerRule
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
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

    @MockK
    private lateinit var labelRepository: LabelRepository

    @MockK
    private lateinit var accountManager: AccountManager

    @MockK
    private lateinit var contactRepository: ContactsRepository

    private val dispatcherProvider = TestDispatcherProvider

    @InjectMockKs
    private lateinit var contactGroupsRepository: ContactGroupsRepository

    private val testUserId = UserId("testUserId")

    private val label1 = Label(
        id = LabelId("a"),
        name = "aa",
        color = "testColor",
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
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
    }

    @Test
    fun verifyThatDbAndApiContactsAreEmittedInOrder() {
        runBlockingTest {
            // given
            val dbContactsList = listOf(label1)
            val searchTerm = "Rob"
            coEvery { labelRepository.observeSearchContactGroups(searchTerm, testUserId) } returns flowOf(
                dbContactsList
            )
            coEvery { contactRepository.countContactEmailsByLabelId(any()) } returns 1

            // when
            val result = contactGroupsRepository.observeContactGroups(searchTerm).first()

            // then
            assertEquals(listOf(label1UiModel), result)
        }
    }

    @Test
    fun verifyThatDbAndApiContactsAreEmittedIn() {
        runBlockingTest {
            // given
            val dbContactsList = listOf(label1)
            val searchTerm = "Rob"
            coEvery { labelRepository.observeSearchContactGroups(searchTerm, testUserId) } returns flowOf(
                dbContactsList
            )
            coEvery { contactRepository.countContactEmailsByLabelId(any()) } returns 1

            // when
            val result = contactGroupsRepository.observeContactGroups(searchTerm).first()

            // then
            assertEquals(listOf(label1UiModel), result)
        }
    }

    @Test
    fun verifyThatDbContactsAreEmitted() {
        runBlockingTest {
            // given
            val searchTerm = "search"
            val dbContactsList = listOf(label1)
            coEvery { labelRepository.observeSearchContactGroups(searchTerm, testUserId) } returns flowOf(
                dbContactsList
            )
            coEvery { contactRepository.countContactEmailsByLabelId(any()) } returns 1

            // when
            val result = contactGroupsRepository.observeContactGroups(searchTerm).first()

            // then
            assertEquals(listOf(label1UiModel), result)
        }
    }

    @Test
    fun saveContactGroupStoresGivenContactGroupInDatabase() = runBlockingTest {
        val userId = UserId("testUserId")
        val contactGroup = Label(
            id = LabelId("Id"),
            name = "name",
            color = "color",
            type = LabelType.MESSAGE_LABEL,
            path = "a/b",
            parentId = "parentId",
        )
        coEvery { labelRepository.saveLabel(any(), any()) } just Runs

        contactGroupsRepository.saveContactGroup(contactGroup, userId)

        coVerify { labelRepository.saveLabel(contactGroup, userId) }
    }

}
