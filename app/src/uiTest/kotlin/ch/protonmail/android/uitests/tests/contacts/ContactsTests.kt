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

import androidx.test.filters.LargeTest
import ch.protonmail.android.uitests.robots.contacts.ContactsRobot
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@LargeTest
class ContactsTests : BaseTest() {

    lateinit var contactsRobot: ContactsRobot
    private val loginRobot = LoginRobot()

    @Before
    override fun setUp() {
        super.setUp()
        contactsRobot = loginRobot
            .loginUser(TestData.onePassUser)
            .menuDrawer()
            .contacts()
    }

    @Test
    fun contactListRefresh() {
        contactsRobot
            .openOptionsMenu()
            .refresh()
            .verify { contactsRefreshed() }
    }

    @Test
    fun addNewContact() {
        contactsRobot
            .addFab()
            .addContact()
            .insertContactName(TestData.newContactName)
            .saveContact()
            .verify { contactSaved() }
    }


    //TODO Check with developers as I'm getting java.lang.ArrayIndexOutOfBoundsException: during test run
    fun editContact() {
        val contactName = "${TestData.newContactName} ${System.currentTimeMillis()}"
        contactsRobot
            .addFab()
            .addContact()
            .insertContactName(contactName)
            .saveContact()
            .chooseContact(contactName)
            .contactDetails()
            .editDisplayName(TestData.editContactName)
            .editEmailAddress(TestData.editEmailAddress)
            .saveContact()
            .verify { contactSaved() }
    }

    // TODO Check with developers as I'm getting java.lang.ArrayIndexOutOfBoundsException: during test run
    fun deleteContact() {
        val deletedContact = ""
        contactsRobot
            .delete()
            .confirmDelete()
            .verify { contactDeleted(deletedContact) }
    }

    //TODO to be enabled when https://jira.protontech.ch/browse/MAILAND-662 is fixed
    fun contactDetailSendMessage() {
        contactsRobot
            .chooseContactWithText("protonmail.com")
            .composeButton()
            .composeMessage(TestData.messageSubject, TestData.messageBody)
            .send()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun addNewContactGroup() {
        val contactGroupName = "${TestData.newGroupName} ${System.currentTimeMillis()}"
        contactsRobot
            .addFab()
            .addContactGroup()
            .editGroupName(contactGroupName)
            .saveGroup()
            .verify { groupSaved() }
    }

    @Test
    fun contactGroupEdit() {
        val contactGroupName = "${TestData.newGroupName} ${System.currentTimeMillis()}"
        val editedGroupName = "${TestData.newGroupName} edited ${System.currentTimeMillis()}"
        contactsRobot
            .addFab()
            .addContactGroup()
            .editGroupName(contactGroupName)
            .saveGroup()
            .groupsView()
            .chooseGroup(contactGroupName)
            .editFab()
            .editGroupName(editedGroupName)
            .saveGroup()
            .verify { groupSaved() }
    }

    //TODO to be enabled when https://jira.protontech.ch/browse/MAILAND-662 is fixed
    fun contactGroupSendMessage() {
        val contactGroupName = "${TestData.newGroupName} ${System.currentTimeMillis()}"
        contactsRobot
            .addFab()
            .addContactGroup()
            .editGroupName(contactGroupName)
            .saveGroup()
            .groupsView()
            .chooseGroup(contactGroupName)
            .groupsView()
            .composeToGroup()
            .composeMessage(TestData.messageSubject, TestData.messageBody)
            .send()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun deleteGroup() {
        val contactGroupName = "${TestData.newGroupName} ${System.currentTimeMillis()}"
        contactsRobot
            .addFab()
            .addContactGroup()
            .editGroupName(contactGroupName)
            .saveGroup()
            .groupsView()
            .chooseGroup(contactGroupName)
            .delete()
            .confirmDelete()
            .verify { groupDeleted(contactGroupName) }
    }
}
