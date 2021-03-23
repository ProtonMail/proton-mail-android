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

import ch.protonmail.android.contacts.list.listView.ContactItem
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class ContactsListMapper @Inject constructor() {

    fun mapToContactItems(
        dataList: List<ContactData>,
        emailsList: List<ContactEmail>
    ): List<ContactItem> {
        val emailsMap = emailsList.groupBy(ContactEmail::contactId)

        return dataList.map { contactData ->
            Timber.v("Map contactData: $contactData")
            val contactId = contactData.contactId
            val name = contactData.name
            var primaryEmail: String? = null
            var additionalEmailsCount = 0

            emailsMap[contactId]?.apply {
                if (!isEmpty()) {
                    primaryEmail = get(0).email
                }
                if (size > 1) {
                    additionalEmailsCount = kotlin.math.max(size - 1, 0)
                }
            }

            ContactItem(
                true,
                contactId,
                name,
                primaryEmail,
                additionalEmailsCount
            )
        }
    }

    fun mergeContactItems(
        protonmailContacts: List<ContactItem>,
        androidContacts: List<ContactItem>
    ): List<ContactItem> {
        val protonMailEmails = protonmailContacts.asSequence()
            .map { it.getEmail().toLowerCase(Locale.ENGLISH) }
            .toSet()

        val filteredAndroidContacts = androidContacts.filter {
            !protonMailEmails.contains(it.getEmail().toLowerCase(Locale.ENGLISH))
        }

        val mergedContacts = mutableListOf<ContactItem>()
        if (protonmailContacts.isNotEmpty()) {
            // adding this for serving as a header item
            mergedContacts.add(ContactItem(contactId = "-1", isProtonMailContact = true))
            mergedContacts.addAll(protonmailContacts)
        }
        if (filteredAndroidContacts.isNotEmpty()) {
            // adding this for serving as a header item
            mergedContacts.add(ContactItem(contactId = "-1", isProtonMailContact = false))
            mergedContacts.addAll(filteredAndroidContacts)
        }
        return mergedContacts
    }
}
