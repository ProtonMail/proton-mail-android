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

package ch.protonmail.android.usecase.create

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.worker.CreateContactWorker
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CreateContactTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var createContactScheduler: CreateContactWorker.Enqueuer

    @RelaxedMockK
    private lateinit var contactsDao: ContactsDao

    @RelaxedMockK
    private lateinit var mockObserver: Observer<CreateContact.CreateContactResult>

    @InjectMockKs
    private lateinit var createContact: CreateContact

    private val dispatcherProvider = TestDispatcherProvider

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `create contact saves contact emails with contact ID in database`() {
        runBlockingTest {
            val contactData = ContactData("contactDataId", "name")
            val contactEmails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom"),
                ContactEmail("ID2", "secondary@proton.com", "Mike")
            )

            createContact(contactData, contactEmails)

            val emailWithContactId = ContactEmail("ID1", "email@proton.com", "Tom", contactId = "contactDataId")
            val secondaryEmailWithContactId = ContactEmail("ID2", "secondary@proton.com", "Mike", contactId = "contactDataId")
            val expectedContactEmails = listOf(emailWithContactId, secondaryEmailWithContactId)
            verify { contactsDao.saveAllContactsEmails(expectedContactEmails) }
        }
    }

}
