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
import ch.protonmail.android.labels.data.local.model.LabelType
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.usecase.delete.DeleteLabel
import ch.protonmail.android.utils.Event
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ContactGroupsViewModelTest : CoroutinesTest {

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var deleteLabel: DeleteLabel

    private val contactsListMapper = ContactsListMapper()

    @RelaxedMockK
    private lateinit var contactGroupsRepository: ContactGroupsRepository

    @InjectMockKs
    private lateinit var contactGroupsViewModel: ContactGroupsViewModel

    private val testPath = "test/path1234"
    private val label1 =
        ContactLabelUiModel(LabelId("a"), "aa", "color", LabelType.MESSAGE_LABEL, testPath, "parentId", 0, 0, 0)
    private val label2 =
        ContactLabelUiModel(LabelId("b"), "bb", "color", LabelType.MESSAGE_LABEL, testPath, "parentId", 0, 0, 0)
    private val label3 =
        ContactLabelUiModel(LabelId("c"), "cc", "color", LabelType.MESSAGE_LABEL, testPath, "parentId", 0, 0, 0)

    private val testColorInt = 871

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
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
        coEvery { contactGroupsRepository.observeContactGroups(searchTerm) } returns flowOf(contactLabels)

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
            coEvery { contactGroupsRepository.observeContactGroups(searchTerm) } throws exception

            // when
            contactGroupsViewModel.setSearchPhrase(searchTerm)
            contactGroupsViewModel.observeContactGroups()

            // then
            val observedError = resultLiveData.observedValues[0]
            assertEquals("test-exception", (observedError as Event).getContentIfNotHandled())
        }
    }

}
