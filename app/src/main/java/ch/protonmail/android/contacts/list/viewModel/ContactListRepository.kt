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
package ch.protonmail.android.contacts.list.viewModel

import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.api.exceptions.ApiException
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.core.Constants
import ch.protonmail.android.jobs.DeleteContactJob
import com.birbit.android.jobqueue.JobManager
import io.reactivex.Completable
import java.io.IOException
import javax.inject.Inject

class ContactListRepository @Inject constructor(val jobManager: JobManager, val api: ProtonMailApi, val contactsDatabase: ContactsDatabase) {

     fun delete(contactItems: IDList): Completable {
        return api.deleteContact(contactItems)
                .doOnSuccess {
                    it?.responses?.forEach {
                        if (it.responseBody.code == Constants.RESPONSE_CODE_OK) {

                            val contactData = contactsDatabase.findContactDataById(it.id)
                            if (contactData != null) {
                                    val contactEmails = contactsDatabase.findContactEmailsByContactId(
                                        contactData.contactId!!
                                    )
                                    contactsDatabase.deleteAllContactsEmails(contactEmails)
                                    contactsDatabase.deleteContactData(contactData)
                            }
                        } else {
                            throw ApiException(it.responseBody, it.responseBody.error)
                        }
                    }
                }
                .doOnError {
                    if (it is IOException) {
                        jobManager.addJobInBackground(DeleteContactJob(contactItems.iDs))
                    }
                }.toCompletable()
    }
}