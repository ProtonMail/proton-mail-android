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

import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.reSubject
import ch.protonmail.android.uitests.testsHelper.TestUser.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
import kotlin.test.BeforeTest
import kotlin.test.Test

class ReplyToMessageTests : BaseTest() {

    private val loginRobot = LoginMailRobot()
    private lateinit var subject: String
    private lateinit var body: String

    @BeforeTest
    override fun setUp() {
        super.setUp()
        subject = TestData.messageSubject
        body = TestData.messageBody
    }

    @TestId("1945")
    @Test
    fun reply() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .openActionSheet()
            .reply()
            .editBodyAndReply("Reply")
            .navigateUpToSent()
            .refreshMessageList()
            .verify {
                messageWithSubjectExists(reSubject(subject))
            }
    }

    @TestId("1946")
    @Test
    fun replyMessageWithAttachment() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .sendMessageCameraCaptureAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .openActionSheet()
            .reply()
            .editBodyAndReply("Robot Reply With Attachment")
            .navigateUpToSent()
            .verify { messageWithSubjectExists(TestData.reSubject(subject)) }
    }

    @TestId("21093")
    @Test
    fun replyAll() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .openActionSheet()
            .replyAll()
            .editBodyAndReply("Robot ReplyAll")
            .navigateUpToSent()
            .verify {
                messageWithSubjectExists(TestData.reSubject(subject))
                messageWithSubjectHasRepliedAllFlag(subject)
            }
    }
}
