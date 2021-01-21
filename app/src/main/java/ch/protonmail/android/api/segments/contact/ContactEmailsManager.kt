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
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.core.Constants
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

class ContactEmailsManager @Inject constructor(
    private var apiManager: ProtonMailApiManager,
    private val databaseProvider: DatabaseProvider
) {

    fun refreshBlocking() {
        runBlocking {
            refresh()
        }
    }

    suspend fun refresh() {
        Timber.v("Contacts refresh called")
        val contactLabelList = apiManager.fetchContactGroupsList()
        val firstPage = apiManager.fetchRawContactEmails(0, Constants.CONTACTS_PAGE_SIZE)
        val allResults = mutableListOf(firstPage)
        for (i in 1..(firstPage.total + (Constants.CONTACTS_PAGE_SIZE - 1)) / Constants.CONTACTS_PAGE_SIZE) {
            allResults.add(apiManager.fetchRawContactEmails(i, Constants.CONTACTS_PAGE_SIZE))
        }
        val allContactEmails = allResults.flatMap { it.contactEmails }

        saveData(allContactEmails, contactLabelList, getJoins(allContactEmails))
    }

    private fun getJoins(allContactEmails: List<ContactEmail>): MutableList<ContactEmailContactLabelJoin> {
        val allJoins = mutableListOf<ContactEmailContactLabelJoin>()
        for (contactEmail in allContactEmails) {
            val labelIds = contactEmail.labelIds
            if (labelIds != null) {
                for (labelId in labelIds) {
                    allJoins.add(ContactEmailContactLabelJoin(contactEmail.contactEmailId, labelId))
                }
            }
        }
        return allJoins
    }

    private fun saveData(
        allContactEmails: List<ContactEmail>,
        contactLabelList: List<ContactLabel>,
        allJoins: MutableList<ContactEmailContactLabelJoin>
    ) {
        Timber.v(
            "Saving fresh sizes allContactEmails: ${allContactEmails.size}, contactLabelList: ${contactLabelList.size}, allJoins: ${allJoins.size}"
        )
        val contactsDatabase = databaseProvider.provideContactsDatabase()
        val contactsDao = databaseProvider.provideContactsDao()
        contactsDatabase.runInTransaction {
            contactsDao.clearContactEmailsCache()
            contactsDao.clearContactGroupsList()
            contactsDao.saveContactGroupsListBlocking(contactLabelList)
            contactsDao.saveAllContactsEmails(allContactEmails)
            contactsDao.saveContactEmailContactLabel(allJoins)
        }
    }
}
