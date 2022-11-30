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

package ch.protonmail.android.contacts.list.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.work.WorkManager
import ch.protonmail.android.contacts.repositories.andorid.baseInfo.AndroidContactsRepository
import ch.protonmail.android.contacts.repositories.andorid.details.AndroidContactDetailsRepository
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.utils.Event
import ch.protonmail.android.views.models.LocalContact
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.proton.core.test.android.ArchTest
import org.junit.Test
import kotlin.test.assertEquals

class ContactsListViewModelTest : ArchTest by ArchTest() {

    private val contactDaoMock: ContactDao = mockk()
    private val workManagerMock: WorkManager = mockk()
    private val androidContactsRepositoryMock: AndroidContactsRepository = mockk {
        every { androidContacts } returns MutableLiveData(emptyList())
        every { setContactsPermission(any()) } returns Unit
    }
    private val androidContactsDetailsRepositoryMock: AndroidContactDetailsRepository = mockk {
        every { contactDetails } returns MutableLiveData(Event(TestLocalContact.instance))
    }
    private val contactsListMapperMock: ContactsListMapper = mockk()
    private val viewModel = ContactsListViewModel(
        contactDaoMock,
        workManagerMock,
        androidContactsRepositoryMock,
        androidContactsDetailsRepositoryMock,
        contactsListMapperMock,
        moveMessagesToFolder = mockk()
    )

    @Test
    fun whenHasPermissionsShouldUpdateRepositoryAndNotShowPermissionDialog() {
        val dialogTriggerObserver = viewModel.showPermissionMissingDialog.testObserver()

        viewModel.setHasContactsPermission(true)

        verify { androidContactsRepositoryMock.setContactsPermission(true) }
        val numberOfReceivedDialogEvents = dialogTriggerObserver.observedValues.size
        assertEquals(expected = 0, actual = numberOfReceivedDialogEvents)
    }

    @Test
    fun whenDoesNotHavePermissionsShouldUpdateRepositoryAndShowPermissionDialog() {
        val dialogTriggerObserver = viewModel.showPermissionMissingDialog.testObserver()

        viewModel.setHasContactsPermission(false)

        verify { androidContactsRepositoryMock.setContactsPermission(false) }
        val numberOfReceivedDialogEvents = dialogTriggerObserver.observedValues.size
        assertEquals(expected = 1, actual = numberOfReceivedDialogEvents)
    }
}

private object TestLocalContact {
    const val NAME = "Name"
    val instance = LocalContact(
        name = NAME,
        emails = emptyList(),
        phones = emptyList(),
        addresses = emptyList(),
        groups = emptyList()
    )
}
