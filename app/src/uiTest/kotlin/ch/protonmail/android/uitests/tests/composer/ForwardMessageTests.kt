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
import ch.protonmail.android.uitests.testsHelper.TestData.updatedSubject
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
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
            .clickMessageBySubject(subject)
            .forward()
            .forwardMessage(to, body)
            .navigateUpToSent()
            .refreshMessageList()
            .verify {
                messageWithSubjectExists(fwSubject(subject))
                messageWithSubjectHasForwardedFlag(subject)
            }
    }

    @TestId("1950")
    @Category(SmokeTest::class)
    @Test
    fun forwardMessageWithAttachment() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageWithFileAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .forward()
            .forwardMessage(to, body)
            .navigateUpToSent()
            .refreshMessageList()
            .clickMessageBySubject(fwSubject(subject))
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }

    @TestId("1361")
    @Test
    fun forwardMessageFromSearchWithAttachment() {
        val to = internalEmailTrustedKeys.email
        val searchMessageSubject = "ProtonMail Logo attachment"
        val searchMessageSubjectPart = "ProtonMail Logo"
        loginRobot
            .loginUser(onePassUser)
            .searchBar()
            .searchMessageText(searchMessageSubject)
            .clickSearchedMessageBySubjectPart(searchMessageSubjectPart)
            .forward()
            .changeSubjectAndForwardMessage(to, subject)
            .navigateUpToSearch()
            .navigateUpToInbox()
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageBySubject(updatedSubject(subject))
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }

    @TestId("43000")
    @Test
    fun forwardMessageWithAttachmentsAndRemoveAttachment() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageTwoImageCaptureAttachments(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .forward()
            .attachments()
            .removeAttachment()
            .navigateUpToComposerView()
            .forwardMessage(to, body)
            .navigateUpToSent()
            .clickMessageBySubject(fwSubject(subject))
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }

    @TestId("43001")
    @Test
    fun forwardMessageWithAttachmentAndChangeSender() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageWithFileAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .forward()
            .changeSenderTo(onePassUser.pmMe)
            .forwardMessage(to, body)
            .navigateUpToSent()
            .clickMessageBySubject(fwSubject(subject))
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }
}
