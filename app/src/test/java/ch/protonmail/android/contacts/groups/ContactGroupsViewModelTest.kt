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
package ch.protonmail.android.contacts.groups

import android.graphics.Color
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.contacts.groups.list.ContactGroupListItem
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.contacts.groups.list.ContactGroupsViewModel
import ch.protonmail.android.contacts.list.viewModel.ContactsListMapper
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.domain.usecase.DeleteLabels
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.utils.Event
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ContactGroupsViewModelTest :
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val userManager: UserManager = mockk(relaxed = true)

    private val deleteLabels: DeleteLabels = mockk(relaxed = true)

    private val contactsListMapper = ContactsListMapper()

    private val contactGroupsRepository: ContactGroupsRepository = mockk(relaxed = true)

    private val contactGroupsViewModel = ContactGroupsViewModel(
        contactGroupsRepository = contactGroupsRepository,
        contactsListMapper = contactsListMapper,
        deleteLabels = deleteLabels,
        userManager = userManager,
        moveMessagesToFolder = mockk()
    )

    private val testPath = "test/path1234"
    private val label1 =
        ContactLabelUiModel(LabelId("a"), "aa", "color", LabelType.MESSAGE_LABEL, testPath, "parentId", 0)
    private val label2 =
        ContactLabelUiModel(LabelId("b"), "bb", "color", LabelType.MESSAGE_LABEL, testPath, "parentId", 0)
    private val label3 =
        ContactLabelUiModel(LabelId("c"), "cc", "color", LabelType.MESSAGE_LABEL, testPath, "parentId", 0)

    private val testColorInt = 871

    @BeforeTest
    fun setUp() {
        mockkStatic(Color::class)
        every { Color.parseColor(any()) } returns testColorInt
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Color::class)
    }

    @Test
    fun verifyThatFetchContactGroupsPostsSucceedsWithDataEmittedInContactGroupsResult() {
        // given
        val searchTerm = "searchTerm"
        val resultLiveData = contactGroupsViewModel.contactGroupsResult.testObserver()
        val contactLabels = listOf(label1, label2, label3)
        val listItem1 = ContactGroupListItem(
            label1.id.id,
            label1.name,
            0,
            color = testColorInt,
        )
        val listItem2 = ContactGroupListItem(
            label2.id.id,
            label2.name,
            0,
            color = testColorInt,
        )
        val listItem3 = ContactGroupListItem(
            label3.id.id,
            label3.name,
            0,
            color = testColorInt,
        )
        val contactListItems = listOf(listItem1, listItem2, listItem3)
        coEvery { contactGroupsRepository.observeContactGroups(any(), searchTerm) } returns flowOf(contactLabels)

        // when
        contactGroupsViewModel.setSearchPhrase(searchTerm)
        contactGroupsViewModel.observeContactGroups()

        // then
        val observedContactLabels = resultLiveData.observedValues[0]
        assertEquals(contactListItems, observedContactLabels)
    }

    @Test
    fun verifyThatFetchContactGroupsErrorCausesContactGroupsErrorEmission() {
        // given
        runBlockingTest {
            val searchTerm = "searchTerm"
            val resultLiveData = contactGroupsViewModel.contactGroupsError.testObserver()
            val exception = Exception("test-exception")
            coEvery { contactGroupsRepository.observeContactGroups(any(), searchTerm) } throws exception

            // when
            contactGroupsViewModel.setSearchPhrase(searchTerm)
            contactGroupsViewModel.observeContactGroups()

            // then
            val observedError = resultLiveData.observedValues[0]
            assertEquals("test-exception", (observedError as Event).getContentIfNotHandled())
        }
    }
}
