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
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class ReplyToMessageTests : BaseTest() {

    private val loginRobot = LoginRobot()
    private lateinit var subject: String
    private lateinit var body: String

    @Before
    override fun setUp() {
        super.setUp()
        subject = TestData.messageSubject
        body = TestData.messageBody
    }

    @TestId("1945")
    @Category(SmokeTest::class)
    @Test
    fun reply() {
        val to = TestData.internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .reply()
            .editBodyAndReply(body, "Robot Reply")
            .navigateUpToSent()
            .verify {
                messageWithSubjectExists(TestData.reSubject(subject))
            }
    }

    @TestId("1946")
    @Test
    fun replyMessageWithAttachment() {
        val to = TestData.internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageCameraCaptureAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .reply()
            .editBodyAndReply(body, "Robot Reply With Attachment ")
            .navigateUpToSent()
            .verify { messageWithSubjectExists(TestData.reSubject(subject)) }
    }

    @TestId("21093")
    @Test
    fun replyAll() {
        val to = TestData.internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .replyAll()
            .editBodyAndReply(body, "Robot ReplyAll ")
            .navigateUpToSent()
            .verify { messageWithSubjectExists(TestData.reSubject(subject)) }
    }
}
