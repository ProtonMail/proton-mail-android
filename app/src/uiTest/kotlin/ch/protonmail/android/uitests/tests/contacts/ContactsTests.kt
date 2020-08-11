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
import ch.protonmail.android.uitests.testsHelper.TestData.twoPassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.experimental.categories.Category
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

    @Category(SmokeTest::class)
    @Test
    fun contactDetailSendMessage() {
        val subject = TestData.messageSubject
        val body = TestData.messageBody
        contactsRobot
            .contactsView()
            .clickSendMessageToContact(twoPassUser.email)
            .sendMessageToContact(subject, body)
            .navigateUpToInbox()
            .menuDrawer()
            .sent()
            .verify {
                messageWithSubjectExists(subject)
            }
    }

    @Test
    fun contactGroupSendMessage() {
        val subject = TestData.messageSubject
        val body = TestData.messageBody
        val groupName = "Second Group"
        contactsRobot
            .groupsView()
            .clickSendMessageToGroup(groupName)
            .sendMessageToContact(subject, body)
            .navigateUpToInbox()
            .menuDrawer()
            .sent()
            .verify {
                messageWithSubjectExists(subject)
            }
    }
}
