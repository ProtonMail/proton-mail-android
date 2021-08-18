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

import android.graphics.Color
import ch.protonmail.android.R
import ch.protonmail.android.contacts.groups.list.ContactGroupListItem
import ch.protonmail.android.contacts.list.listView.ContactItem
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactLabelEntity
import ch.protonmail.android.utils.UiUtil
import me.proton.core.util.kotlin.EMPTY_STRING
import java.util.Locale
import javax.inject.Inject

class ContactsListMapper @Inject constructor() {

    fun mapToContactItems(
        dataList: List<ContactData>,
        emailsList: List<ContactEmail>
    ): List<ContactItem> {
        val emailsMap = emailsList.groupBy(ContactEmail::contactId)

        return dataList.map { contactData ->
            val contactId = contactData.contactId
            val name = contactData.name
            var primaryEmail: String = EMPTY_STRING
            var additionalEmailsCount = 0

            emailsMap[contactId]?.apply {
                if (!isEmpty()) {
                    primaryEmail = get(0).email
                }
                if (size > 1) {
                    additionalEmailsCount = kotlin.math.max(size - 1, 0)
                }
            }

            val contactEmails =
                if (primaryEmail.isEmpty()) {
                    EMPTY_STRING
                } else {
                    val additionalEmailsText = if (additionalEmailsCount > 0)
                        ", +$additionalEmailsCount"
                    else
                        ""
                    primaryEmail + additionalEmailsText
                }

            ContactItem(
                isProtonMailContact = true,
                name = name,
                contactId = contactId,
                contactEmails = contactEmails,
                initials = UiUtil.extractInitials(name).take(2),
                additionalEmailsCount = additionalEmailsCount,
                headerStringRes = null
            )
        }
    }

    fun mergeContactItems(
        protonmailContacts: List<ContactItem>,
        androidContacts: List<ContactItem>
    ): List<ContactItem> {
        val protonMailEmails = protonmailContacts.asSequence()
            .map { it.contactEmails?.toLowerCase(Locale.ENGLISH) }
            .toSet()

        val filteredAndroidContacts = androidContacts.filter {
            !protonMailEmails.contains(it.contactEmails?.toLowerCase(Locale.ENGLISH))
        }

        val mergedContacts = mutableListOf<ContactItem>()
        if (protonmailContacts.isNotEmpty()) {
            // adding this for serving as a header item
            mergedContacts.add(
                ContactItem(
                    isProtonMailContact = true,
                    headerStringRes = R.string.protonmail_contacts,
                )
            )
            mergedContacts.addAll(protonmailContacts)
        }
        if (filteredAndroidContacts.isNotEmpty()) {
            // adding this for serving as a header item
            mergedContacts.add(
                ContactItem(
                    isProtonMailContact = false,
                    headerStringRes = R.string.device_contacts,
                )
            )
            mergedContacts.addAll(filteredAndroidContacts)
        }
        return mergedContacts
    }

    fun mapLabelToContactGroup(label: ContactLabelEntity): ContactGroupListItem =
        ContactGroupListItem(
            contactId = label.ID,
            name = label.name,
            contactEmailsCount = label.contactEmailsCount,
            color = Color.parseColor(UiUtil.normalizeColor(label.color)),
        )

    fun mapLabelsToContactGroups(contactLabels: List<ContactLabelEntity>): List<ContactGroupListItem> =
        contactLabels.map { label ->
            mapLabelToContactGroup(label)
        }

}
