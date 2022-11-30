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

package ch.protonmail.android.contacts.details.presentation

import android.content.Context
import android.graphics.Color
import android.net.Uri
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import app.cash.turbine.test
import ch.protonmail.android.contacts.details.domain.FetchContactDetails
import ch.protonmail.android.contacts.details.domain.FetchContactGroups
import ch.protonmail.android.contacts.details.domain.model.FetchContactDetailsResult
import ch.protonmail.android.contacts.details.domain.model.FetchContactGroupsResult
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsUiItem
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsViewState
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.testdata.UserTestData
import ch.protonmail.android.utils.FileHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ContactDetailsViewModelTest : ArchTest by ArchTest(),
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private val fetchContactDetails: FetchContactDetails = mockk()
    private val fetchContactGroups: FetchContactGroups = mockk()
    private val mapper = ContactDetailsMapper()
    private val workManager: WorkManager = mockk()
    private val fileHelper: FileHelper = mockk()
    private val userManager: UserManager = mockk {
        every { currentUserId } returns UserTestData.userId
    }

    private val contactId1 = "contactUid1"
    private val contactName1 = "testContactName"
    private val vCardToShare1 = "testCardType2"
    private val decryptedCardType0 = "decryptedCardType0"
    private val fetchContactResult = FetchContactDetailsResult(
        contactId1,
        contactName1,
        emails = emptyList(),
        telephoneNumbers = emptyList(),
        addresses = emptyList(),
        photos = emptyList(),
        organizations = emptyList(),
        titles = emptyList(),
        nicknames = emptyList(),
        birthdays = emptyList(),
        anniversaries = emptyList(),
        roles = emptyList(),
        urls = emptyList(),
        vCardToShare = vCardToShare1,
        gender = null,
        notes = emptyList(),
        isType2SignatureValid = true,
        isType3SignatureValid = null,
        vDecryptedCardType0 = decryptedCardType0,
        vDecryptedCardType1 = null,
        vDecryptedCardType2 = null,
        vDecryptedCardType3 = null,
    )

    private val viewModel = ContactDetailsViewModel(
        fetchContactDetails = fetchContactDetails,
        fetchContactGroups = fetchContactGroups,
        mapper = mapper,
        moveMessagesToFolder = mockk(),
        workManager = workManager,
        fileHelper = fileHelper,
        userManager = userManager
    )

    private val testColorInt = 321

    private val testPath = "a/bpath"
    private val testParentId = "parentIdForTests"
    private val testType =  LabelType.CONTACT_GROUP

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
    fun verifyThatContactDetailsAndGroupsAreMergedAndMappedCorrectlyToDataState() = runBlockingTest {
        // given
        val contactId = "contactId1"
        every { fetchContactDetails(contactId) } returns flowOf(fetchContactResult)
        val groupId1 = LabelId("ID1")
        val groupName1 = "name1"
        val contactLabel = Label(
            groupId1,
            groupName1,
            "color",
            0,
            testType,
            testPath,
            testParentId,
        )
        val fetchContactGroupResult = FetchContactGroupsResult(
            listOf(contactLabel)
        )
        every { fetchContactGroups(any(), contactId) } returns flowOf(fetchContactGroupResult)
        val expected = ContactDetailsViewState.Data(
            contactId1,
            contactName1,
            "T",
            listOf(
                ContactDetailsUiItem.Group(
                    groupId1,
                    groupName1,
                    testColorInt,
                    0
                )
            ),
            vCardToShare1,
            null,
            null,
            true,
            null,
            decryptedCardType0,
            null,
            null,
            null
        )

        // when
        viewModel.getContactDetails(contactId)

        // then
        assertEquals(expected, viewModel.contactsViewState.value)
    }

    @Test
    fun verifyThatContactDetailsMergingErrorIsMappedToErrorState() = runBlockingTest {
        // given
        val contactId = "contactId1"
        val errorMessage = "An error occurred!"
        val exception = Exception(errorMessage)
        every { fetchContactDetails(contactId) } returns flow {
            throw exception
        }
        val groupId1 = LabelId("ID1")
        val groupName1 = "name1"
        val contactLabel =
            Label(
                groupId1,
                groupName1,
                "color",
                0,
                testType,
                testPath,
                testParentId,
            )
        val fetchContactGroupResult = FetchContactGroupsResult(
            listOf(contactLabel)
        )
        every { fetchContactGroups(any(), contactId) } returns flowOf(fetchContactGroupResult)
        val expected = ContactDetailsViewState.Error(
            exception
        )

        // when
        viewModel.getContactDetails(contactId)

        // then
        val actual = viewModel.contactsViewState.value
        assertEquals(expected.exception.message, (actual as ContactDetailsViewState.Error).exception.message)
    }

    @Test
    fun verifyDeleteContactIsCallingWorker() {
        // given
        val contactId = "contactId1"
        every { workManager.enqueue(any<OneTimeWorkRequest>()) } returns mockk()

        // when
        val result = viewModel.deleteContact(contactId)

        // then
        assertNotNull(result)
    }

    @Test
    fun saveVcard() = runBlockingTest {
        // given
        val vCardToShare = "vcardContent123"
        val contactName = "contactName1"
        val context = mockk<Context>()
        val testUri = mockk<Uri>()
        coEvery { fileHelper.saveStringToFileProvider("$contactName.vcf", vCardToShare, context) } returns testUri

        // when
        viewModel.vCardSharedFlow.test {
            viewModel.saveVcard(vCardToShare, contactName, context)

            // then
            assertEquals(testUri, awaitItem())
        }
    }
}
