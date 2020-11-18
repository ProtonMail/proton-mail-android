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
package ch.protonmail.android.contacts.details.edit

import androidx.work.WorkManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.contacts.details.ContactDetailsRepository
import ch.protonmail.android.jobs.UpdateContactJob
import com.birbit.android.jobqueue.JobManager
import ezvcard.VCard
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class EditContactDetailsRepository @Inject constructor(
    workManager: WorkManager,
    jobManager: JobManager,
    api: ProtonMailApiManager,
    dispatcherProvider: DispatcherProvider,
    contactsDao: ContactsDao) : ContactDetailsRepository(workManager, jobManager, api, contactsDao, dispatcherProvider) {

    fun clearEmail(email: String) {
        contactsDao.clearByEmail(email)
    }

    fun updateContact(contactId: String, contactName: String, emails: List<ContactEmail>,
                      vCardEncrypted: VCard, vCardSigned: VCard, mapEmailGroupsIds: HashMap<ContactEmail, List<ContactLabel>>) {
        jobManager.addJobInBackground(UpdateContactJob(contactId, contactName, emails, vCardEncrypted.write(),
            vCardSigned.write(), mapEmailGroupsIds))
    }

}
