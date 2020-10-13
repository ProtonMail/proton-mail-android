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

package ch.protonmail.android.contacts.groups.edit

import androidx.work.WorkManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.contacts.receive.ContactLabelFactory
import ch.protonmail.android.api.models.messages.receive.ServerLabel
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import com.birbit.android.jobqueue.JobManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ContactGroupEditCreateRepositoryTest {

    @RelaxedMockK
    private lateinit var jobManager: JobManager

    @RelaxedMockK
    private lateinit var workManager: WorkManager

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var contactsDao: ContactsDao
   
    @RelaxedMockK
    private lateinit var contactLabelFactory: ContactLabelFactory

    @InjectMockKs
    private lateinit var repository: ContactGroupEditCreateRepository

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `when editContactGroup is called labelConverterFactory maps DB object to Server Object`() {
        val contactLabel = ContactLabel("Id", "name", "color")

        repository.editContactGroup(contactLabel)

        verify { contactLabelFactory.createServerObjectFromDBObject(contactLabel) }
    }

    @Test
    fun `when editContactGroup is called updateLabelCompletable API gets called with the request object`() {
        val contactGroupId = "contact-group-id"
        val contactLabel = ContactLabel(contactGroupId, "name", "color")
        val updateLabelRequest = ServerLabel(contactGroupId, "name", "color", 0, 0, 0, 0)
        every { contactLabelFactory.createServerObjectFromDBObject(contactLabel) } returns updateLabelRequest

        repository.editContactGroup(contactLabel)

        verify { apiManager.updateLabelCompletable(contactGroupId, updateLabelRequest.labelBody) }
    }

}
