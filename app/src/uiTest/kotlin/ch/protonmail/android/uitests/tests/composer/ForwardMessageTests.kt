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
import ch.protonmail.android.uitests.testsHelper.TestData.fwSubject
import ch.protonmail.android.uitests.testsHelper.TestData.updatedSubject
import ch.protonmail.android.uitests.testsHelper.TestUser.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
import kotlin.test.BeforeTest
import kotlin.test.Test

class ForwardMessageTests : BaseTest() {

    private val loginRobot = LoginMailRobot()
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
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .openActionSheet()
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
    @Test
    fun forwardMessageWithAttachment() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessageWithFileAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .openActionSheet()
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
            .loginOnePassUser()
            .skipOnboarding()
            .searchBar()
            .searchMessageText(searchMessageSubject)
            .clickSearchedMessageBySubjectPart(searchMessageSubjectPart)
            .openActionSheet()
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
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessageTwoImageCaptureAttachments(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .openActionSheet()
            .forward()
            .attachments()
            .removeLastAttachment()
            .goBackToComposer()
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
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessageWithFileAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .openActionSheet()
            .forward()
            .changeSenderTo(onePassUser.pmMe)
            .forwardMessage(to, body)
            .navigateUpToSent()
            .clickMessageBySubject(fwSubject(subject))
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }
}
