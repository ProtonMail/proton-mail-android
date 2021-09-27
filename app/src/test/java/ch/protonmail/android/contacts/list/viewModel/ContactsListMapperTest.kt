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

import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.contacts.list.listView.ContactItem
import kotlin.test.Test
import kotlin.test.assertEquals

class ContactsListMapperTest {

    private val mapper = ContactsListMapper()
    private val contactId1 = "contactId1"
    private val name1 = "name1"
    private val contactId2 = "contactId2"
    private val name2 = "name2"
    private val email1 = "email1@abc.com"
    private val email2 = "email2@abc.com"
    private val contactItem1 = ContactItem(
        isProtonMailContact = true,
        contactId = contactId1,
        name = name1,
        email = email1,
        additionalEmailsCount = 0,
        labels = null,
        isChecked = false
    )
    private val contactItem2 = ContactItem(
        isProtonMailContact = true,
        contactId = contactId2,
        name = name2,
        email = email2,
        additionalEmailsCount = 0,
        labels = null,
        isChecked = false
    )

    @Test
    fun verifyThatEmptyContactsAreMappedProperly() {
        // given
        val dataList = listOf<ContactData>()
        val emailsList = listOf<ContactEmail>()
        val expected = emptyList<ContactItem>()

        // when
        val result = mapper.mapToContactItems(dataList, emailsList)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatContactsAreMappedProperly() {
        // given

        val contact1 = ContactData(contactId = contactId1, name = name1)

        val contact2 = ContactData(contactId = contactId2, name = name2)
        val dataList = listOf(contact1, contact2)
        val contactEmail1 = ContactEmail(
            contactEmailId = "contactEmailId1", email = email1, name = "emailName1", contactId = contactId1
        )
        val contactEmail2 = ContactEmail(
            contactEmailId = "contactEmailId2", email = email2, name = "emailName1", contactId = contactId2
        )
        val emailsList = listOf(contactEmail1, contactEmail2)
        val expected = listOf(
            contactItem1,
            contactItem2
        )

        // when
        val result = mapper.mapToContactItems(dataList, emailsList)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyContactsAreMergedProperlyWithoutOverlappingContacts() {
        // given
        val contactId3 = "contactId3"
        val name3 = "name1"
        val email3 = "email3@abc.com"
        val contactItem3 = ContactItem(
            isProtonMailContact = true,
            contactId = contactId3,
            name = name3,
            email = email3,
            additionalEmailsCount = 0,
            labels = null,
            isChecked = false
        )
        val headerItem =
            ContactItem(
                isProtonMailContact = true,
                contactId = "-1",
                name = null,
                email = null,
                additionalEmailsCount = 0,
                labels = null,
                isChecked = false
            )
        val protonContacts = listOf(contactItem1, contactItem2)
        val androidContacts = listOf(contactItem3)
        val expected = listOf(
            headerItem, contactItem1, contactItem2, headerItem.copy(isProtonMailContact = false), contactItem3
        )

        // when
        val result = mapper.mergeContactItems(protonContacts, androidContacts)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyContactsAreMergedProperlyWithOneOverlappingContactSkipped() {
        // given
        val contactId3 = "contactId3"
        val name3 = "name3"
        val contactItem3 = ContactItem(
            isProtonMailContact = true,
            contactId = contactId3,
            name = name3,
            email = email1,
            additionalEmailsCount = 0,
            labels = null,
            isChecked = false
        )
        val headerItem =
            ContactItem(
                isProtonMailContact = true,
                contactId = "-1",
                name = null,
                email = null,
                additionalEmailsCount = 0,
                labels = null,
                isChecked = false
            )
        val protonContacts = listOf(contactItem1, contactItem2)
        val androidContacts = listOf(contactItem3)
        val expected = listOf(headerItem, contactItem1, contactItem2)

        // when
        val result = mapper.mergeContactItems(protonContacts, androidContacts)

        // then
        assertEquals(expected, result)
    }
}
