/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.uitests.tests.contacts

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.contacts.ContactsRobot
import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.StringUtils.getEmailString
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestUser.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
import kotlin.test.BeforeTest
import kotlin.test.Test

class ContactsTests : BaseTest() {

    private lateinit var contactsRobot: ContactsRobot

    private val loginRobot = LoginMailRobot()

    @BeforeTest
    override fun setUp() {
        super.setUp()
        contactsRobot = loginRobot
            .loginOnePassUser()
            .menuDrawer()
            .contacts()
    }

    @TestId("1419")
    @Test
    fun createContact() {
        val name = TestData.newContactName
        val email = TestData.newEmailAddress
        contactsRobot
            .addContact()
            .setNameEmailAndSave(name, email)
            .openOptionsMenu()
            .refreshContacts()
            .clickContactByEmail(email)
            .deleteContact()
            .verify { contactDoesNotExists(name, email) }
    }

    @TestId("1420")
    @Category(SmokeTest::class)
    @Test
    fun addEditDeleteContact() {
        val name = TestData.newContactName
        val email = TestData.newEmailAddress
        val editedName = TestData.editContactName
        val editedEmail = TestData.editEmailAddress
        contactsRobot
            .addContact()
            .setNameEmailAndSave(name, email)
            .openOptionsMenu()
            .refreshContacts()
            .clickContactByEmail(email)
            .editContact()
            .editNameEmailAndSave(editedName, editedEmail)
            .navigateUp()
            .openOptionsMenu()
            .refreshContacts()
            .clickContactByEmail(editedEmail)
            .deleteContact()
            .verify { contactDoesNotExists(editedName, editedEmail) }
    }

    @TestId("21241")
    @Test
    fun deleteContact() {
        val name = TestData.newContactName
        val email = TestData.newEmailAddress
        contactsRobot
            .addContact()
            .setNameEmailAndSave(name, email)
            .openOptionsMenu()
            .refreshContacts()
            .clickContactByEmail(email)
            .deleteContact()
            .verify { contactDoesNotExists(name, email) }
    }

    @TestId("1421")
    @Test
    fun createGroup() {
        val contactEmail = internalEmailTrustedKeys
        val groupName = TestData.newContactName
        val groupMembersCount =
            targetContext.resources.getQuantityString(R.plurals.contact_group_members, 1, 1)
        contactsRobot
            .addGroup()
            .groupName(groupName)
            .manageAddresses()
            .addContactToGroup(contactEmail.email)
            .done()
            .groupsView()
            .openOptionsMenu()
            .refreshGroups()
            .clickGroup(groupName)
            .deleteGroup()
            .verify { groupDoesNotExists(groupName, groupMembersCount) }
    }

    @TestId("1422")
    @Test
    fun addEditDeleteGroup() {
        val contactEmail = internalEmailTrustedKeys
        val groupName = TestData.newContactName
        val groupMembersCount =
            targetContext.resources.getQuantityString(R.plurals.contact_group_members, 1, 1)
        val newGroupName = TestData.editContactName
        contactsRobot
            .addGroup()
            .groupName(groupName)
            .manageAddresses()
            .addContactToGroup(contactEmail.email)
            .done()
            .groupsView()
            .openOptionsMenu()
            .refreshGroups()
            .clickGroup(groupName)
            .edit()
            .editNameAndSave(newGroupName)
            .navigateUp()
            .openOptionsMenu()
            .refreshGroups()
            .clickGroup(newGroupName)
            .deleteGroup()
            .verify { groupDoesNotExists(newGroupName, groupMembersCount) }
    }

    @TestId("21240")
    @Test
    fun deleteGroup() {
        val contactEmail = internalEmailTrustedKeys
        val groupName = TestData.newContactName
        val groupMembersCount =
            targetContext.resources.getQuantityString(R.plurals.contact_group_members, 1, 1)
        contactsRobot
            .addGroup()
            .groupName(groupName)
            .manageAddresses()
            .addContactToGroup(contactEmail.email)
            .done()
            .groupsView()
            .openOptionsMenu()
            .refreshGroups()
            .clickGroup(groupName)
            .deleteGroup()
            .verify { groupDoesNotExists(groupName, groupMembersCount) }
    }

    @TestId("30833")
    @Category(SmokeTest::class)
    @Test
    fun contactDetailInitiateMessageSending() {
        contactsRobot
            .contactsView()
            .clickSendMessageToContact(internalEmailTrustedKeys.email)
            .verify { fromEmailIs(onePassUser.email) }
    }

    @TestId("1423")
    @Test
    fun contactGroupInitiateMessageSending() {
        val groupName = "Second Group"
        contactsRobot
            .groupsView()
            .clickSendMessageToGroup(groupName)
            .verify { fromEmailIs(onePassUser.email) }
    }
}
