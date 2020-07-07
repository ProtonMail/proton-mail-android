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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.contacts.ContactsRobot
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.shared.SharedRobot.clickHamburgerOrUpButton
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.StringUtils
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestUser
import ch.protonmail.android.uitests.testsHelper.UICustomViewActionsAndMatchers
import org.hamcrest.CoreMatchers
import org.hamcrest.core.AllOf.allOf
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@LargeTest
class ContactsTests : BaseTest() {

    private val contactsRobot = ContactsRobot()
    private val loginRobot = LoginRobot()

    @Before
    override fun setUp() {
        super.setUp()
        loginRobot
            .loginUser(TestUser.onePassUser())
            .openNavbar()
        onView(allOf(
            ViewMatchers.withId(R.id.menuItem),
            ViewMatchers.withTagValue(CoreMatchers.`is`(StringUtils.stringFromResource(R.string.contacts)))))
            .perform(click())
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    @Ignore
    //TODO resolve the error on verification
    fun aContactListRefresh() {
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

    @Test
    fun editContact() {
        contactsRobot
            .chooseContact()
            .contactDetails()
            .editDisplayName(TestData.editContactName)
            .editEmailAddress(TestData.editEmailAddress)
            .saveContact()
            .verify { contactSaved() }
    }

    @Test
    fun deleteContact() {
        val deletedContact = contactsRobot
            .chooseContact().contactName
        contactsRobot
            .delete()
            .confirmDelete()
            .verify { contactDeleted(deletedContact) }
    }


    @Test
    @Ignore
    //TODO to be enabled when https://jira.protontech.ch/browse/MAILAND-662 is fixed
    fun contactDetailSendMessage() {
        contactsRobot
            .chooseContactWithText("protonmail.com")
            .composeButton()
            .composeMessage(TestData.composerData())
            .send()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun addNewContactGroup() {
        contactsRobot
            .addFab()
            .addContactGroup()
            .editGroupName(TestData.newGroupName)
            .saveGroup()
            .verify { groupSaved() }
    }

    @Test
    fun contactGroupEdit() {
        contactsRobot
            .groupsView()
            .chooseGroup()
            .editFab()
            .editGroupName(TestData.editGroupName)
            .manageAddresses()
            .saveGroup()
            .saveGroup()
            .verify { groupSaved() }
    }

    @Test
    @Ignore
    //TODO to be enabled when https://jira.protontech.ch/browse/MAILAND-662 is fixed
    fun contactGroupSendMessage() {
        contactsRobot
            .groupsView()
            .composeToGroup()
            .composeMessage(TestData.composerData())
            .send()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun deleteGroup() {
        val deletedGroup = contactsRobot
            .groupsView().groupName
        contactsRobot
            .chooseGroup()
            .delete()
            .confirmDelete()
            .verify { groupDeleted(deletedGroup) }
    }
}