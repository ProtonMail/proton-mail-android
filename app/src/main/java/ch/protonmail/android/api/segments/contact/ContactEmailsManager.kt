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
package ch.protonmail.android.api.segments.contact

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactEmailContactLabelJoin
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.core.Constants
import javax.inject.Inject

class ContactEmailsManager @Inject constructor(
    private var apiManager: ProtonMailApiManager,
    private val databaseProvider: DatabaseProvider
) {

    private var contactApi: ContactApiSpec = apiManager

    fun refresh() {
        // fetch and prepare data
        val contactLabelList = apiManager.fetchContactGroups()
            .map { it.contactGroups }
            .subscribeOn(ThreadSchedulers.io())
            .observeOn(ThreadSchedulers.io())
            .blockingGet()
        val list = contactApi.fetchContactEmails(Constants.CONTACTS_PAGE_SIZE)
        val allContactEmails = ArrayList<ContactEmail>()
        list.forEach {
            it?.let {
                allContactEmails.addAll(it.contactEmails)
            }
        }
        val allJoins = ArrayList<ContactEmailContactLabelJoin>()
        for (contactEmail in allContactEmails) {
            val labelIds = contactEmail.labelIds
            if (labelIds != null) {
                for (labelId in labelIds) {
                    allJoins.add(ContactEmailContactLabelJoin(contactEmail.contactEmailId, labelId))
                }
            }
        }
        val contactsDatabase = databaseProvider.provideContactsDatabase()
        val contactsDao = databaseProvider.provideContactsDao()
        contactsDatabase.runInTransaction {
            contactsDao.clearContactEmailsCache()
            contactsDao.clearContactGroupsList()
            contactsDao.saveContactGroupsList(contactLabelList)
            contactsDao.saveAllContactsEmails(allContactEmails)
            contactsDao.saveContactEmailContactLabel(allJoins)
        }
    }
}
