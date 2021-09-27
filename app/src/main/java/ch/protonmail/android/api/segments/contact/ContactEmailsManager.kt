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
import ch.protonmail.android.api.models.ContactEmailsResponseV2
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactEmailContactLabelJoin
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.core.Constants
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

class ContactEmailsManager @Inject constructor(
    private var api: ProtonMailApiManager,
    private val contactsDao: ContactsDao
) {

    suspend fun refresh(pageSize: Int = Constants.CONTACTS_PAGE_SIZE) {
        val contactLabelList = api.fetchContactGroupsList()

        var currentPage = 0
        var hasMorePages = true
        val allResults = mutableListOf<ContactEmailsResponseV2>()
        while (hasMorePages) {
            val result = api.fetchContactEmails(currentPage, pageSize)
            allResults += result
            hasMorePages = currentPage < result.total / pageSize
            currentPage++
        }

        if (allResults.isNotEmpty()) {
            val allContactEmails = allResults.flatMap { it.contactEmails }
            val allJoins = getJoins(allContactEmails)
            Timber.v(
                "Refresh emails: ${allContactEmails.size}, labels: ${contactLabelList.size}, allJoins: ${allJoins.size}"
            )
            contactsDao.insertNewContactsAndLabels(allContactEmails, contactLabelList, allJoins)
        } else {
            Timber.v("contactEmails result list is empty")
        }
    }

    private fun getJoins(allContactEmails: List<ContactEmail>): List<ContactEmailContactLabelJoin> {
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

    @Deprecated(
        message = "Please use suspended version wherever possible",
        replaceWith = ReplaceWith("refresh()")
    )
    fun refreshBlocking() = runBlocking {
        refresh()
    }
}
