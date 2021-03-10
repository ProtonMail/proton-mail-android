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
package ch.protonmail.android.uitests.tests.contacts

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.contacts.ContactsRobot
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.StringUtils.getEmailString
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.checkGroupDoesNotExist
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
import kotlin.test.BeforeTest
import kotlin.test.Test

class ContactsTests : BaseTest() {

    private lateinit var contactsRobot: ContactsRobot
    private val loginRobot = LoginRobot()

    @BeforeTest
    override fun setUp() {
        super.setUp()
        contactsRobot = loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .contacts()
    }

    @Test
    fun createContact() {
        val name = TestData.newContactName
        val email = TestData.newEmailAddress
        contactsRobot
            .addContact()
            .setNameEmailAndSave(name, email)
            .openOptionsMenu()
            .refresh()
            .clickContactByEmail(email)
            .deleteContact()
            .verify { contactDoesNotExists(name, email) }
    }

    @Category(SmokeTest::class)
    @TestId("1420")
    @Test
    fun editContact() {
        val name = TestData.newContactName
        val email = TestData.newEmailAddress
        val editedName = TestData.newContactName
        val editedEmail = TestData.newEmailAddress
        contactsRobot
            .addContact()
            .setNameEmailAndSave(name, email)
            .openOptionsMenu()
            .refresh()
            .clickContactByEmail(email)
            .editContact()
            .editNameEmailAndSave(editedName, editedEmail)
            .navigateUp()
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
            .refresh()
            .clickContactByEmail(email)
            .deleteContact()
            .verify { contactDoesNotExists(name, email) }
    }

    @TestId("1421")
    @Test
    fun createGroup() {
        val contactEmail = internalEmailTrustedKeys
        val groupName = getEmailString()
        val groupMembersCount =
            targetContext.resources.getQuantityString(R.plurals.contact_group_members, 1, 1)
        contactsRobot
            .addGroup()
            .groupName(groupName)
            .manageAddresses()
            .addContactToGroup(contactEmail.email)
            .done()
            .openOptionsMenu()
            .refresh()
            .groupsView()
            .clickGroupWithMembersCount(groupName, groupMembersCount)
            .deleteGroup()
            .groupsView()
            .verify { checkGroupDoesNotExist(groupName, groupMembersCount) }
    }

    @TestId("1422")
    @Test
    fun editGroup() {
        val contactEmail = internalEmailTrustedKeys
        val groupName = getEmailString()
        val groupMembersCount =
            targetContext.resources.getQuantityString(R.plurals.contact_group_members, 1, 1)
        val newGroupName = getEmailString()
        contactsRobot
            .addGroup()
            .groupName(groupName)
            .manageAddresses()
            .addContactToGroup(contactEmail.email)
            .done()
            .openOptionsMenu()
            .refresh()
            .groupsView()
            .clickGroup(groupName)
            .edit()
            .editNameAndSave(newGroupName)
            .navigateUp()
            .openOptionsMenu()
            .refresh()
            .groupsView()
            .clickGroupWithMembersCount(newGroupName, groupMembersCount)
            .deleteGroup()
            .groupsView()
            .verify { checkGroupDoesNotExist(newGroupName, groupMembersCount) }
    }

    @TestId("21240")
    @Test
    fun deleteGroup() {
        val contactEmail = internalEmailTrustedKeys
        val groupName = getEmailString()
        val groupMembersCount =
            targetContext.resources.getQuantityString(R.plurals.contact_group_members, 1, 1)
        contactsRobot
            .addGroup()
            .groupName(groupName)
            .manageAddresses()
            .addContactToGroup(contactEmail.email)
            .done()
            .openOptionsMenu()
            .refresh()
            .groupsView()
            .clickGroupWithMembersCount(groupName, groupMembersCount)
            .deleteGroup()
            .groupsView()
            .verify { checkGroupDoesNotExist(groupName, groupMembersCount) }
    }

    @TestId("30833")
    @Category(SmokeTest::class)
    @Test
    fun contactDetailSendMessage() {
        val subject = TestData.messageSubject
        val body = TestData.messageBody
        contactsRobot
            .contactsView()
            .clickSendMessageToContact(internalEmailTrustedKeys.email)
            .sendMessageToContact(subject, body)
            .navigateUpToInbox()
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1423")
    @Test
    fun contactGroupSendMessage() {
        val subject = TestData.messageSubject
        val body = TestData.messageBody
        val groupName = "Second Group"
        contactsRobot
            .groupsView()
            .clickSendMessageToGroup(groupName)
            .sendMessageToGroup(subject, body)
            .navigateUpToInbox()
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }
}
