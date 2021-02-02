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
package ch.protonmail.android.uitests.tests.composer

import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.fwSubject
import ch.protonmail.android.uitests.testsHelper.TestData.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestData.reSubject
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import kotlin.test.BeforeTest
import kotlin.test.Test

class ForwardMessageTests : BaseTest() {

    private val loginRobot = LoginRobot()
    private lateinit var subject: String
    private lateinit var body: String

    @BeforeTest
    override fun setUp() {
        super.setUp()
        subject = TestData.messageSubject
        body = TestData.messageBody
    }

    @TestId("21092")
    @Test
    fun forwardMessage() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .forward()
            .forwardMessage(to, body)
            .navigateUpToSent()
            .verify { messageWithSubjectExists(TestData.fwSubject(subject)) }
    }

    @TestId("1950")
    @Test
    fun forwardMessageWithAttachment() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageWithFileAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .forward()
            .forwardMessage(to, body)
            .navigateUpToSent()
            .verify { messageWithSubjectExists(fwSubject(subject)) }
    }

    @TestId("1361")
    @Test
    fun forwardMessageFromSearchWithAttachment() {
        val to = internalEmailTrustedKeys.email
        val searchMessageSubject = "Twitter Account Security Issue – Update Twitter for Android"
        val searchMessageSubjectPart = "Twitter Account Security Issue"
        loginRobot
            .loginUser(onePassUser)
            .searchBar()
            .searchMessageText(reSubject(searchMessageSubject))
            .clickSearchedMessageBySubjectPart(searchMessageSubjectPart)
            .forward()
            .changeSubjectAndForwardMessage(to, subject)
            .navigateUpToSearch()
            .navigateUpToInbox()
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }
}
