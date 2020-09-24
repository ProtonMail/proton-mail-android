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
import ch.protonmail.android.uitests.testsHelper.TestData.editedPassword
import ch.protonmail.android.uitests.testsHelper.TestData.editedPasswordHint
import ch.protonmail.android.uitests.testsHelper.TestData.externalGmailPGPEncrypted
import ch.protonmail.android.uitests.testsHelper.TestData.externalOutlookPGPSigned
import ch.protonmail.android.uitests.testsHelper.TestData.internalEmailNotTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestData.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestData.twoPassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class SendNewMessageTests : BaseTest() {

    private val loginRobot = LoginRobot()
    private lateinit var subject: String
    private lateinit var body: String

    @Before
    override fun setUp() {
        super.setUp()
        subject = TestData.messageSubject
        body = TestData.messageBody
    }

    @TestId("1545")
    @Test
    fun sendMessageToInternalTrustedContact() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1546")
    @Test
    fun sendMessageToInternalNotTrustedContact() {
        val to = internalEmailNotTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1543")
    @Category(SmokeTest::class)
    @Test
    fun sendMessageToPGPEncryptedContact() {
        val to = externalGmailPGPEncrypted.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1544")
    @Category(SmokeTest::class)
    @Test
    fun sendMessageToPGPSignedContact() {
        val to = externalGmailPGPEncrypted.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @Test
    fun sendMessageTOandCC() {
        val to = internalEmailTrustedKeys.email
        val cc = externalOutlookPGPSigned.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageTOandCC(to, cc, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @Category(SmokeTest::class)
    @Test
    fun sendMessageTOandCCandBCC() {
        val to = externalGmailPGPEncrypted.email
        val cc = internalEmailTrustedKeys.email
        val bcc = internalEmailNotTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageTOandCCandBCC(to, cc, bcc, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1542")
    @Test
    fun sendMessageEO() {
        val to = externalOutlookPGPSigned.email
        val password = editedPassword
        val hint = editedPasswordHint
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageWithPassword(to, subject, body, password, hint)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1550")
    @Test
    fun sendMessageExpiryTime() {
        val to = externalOutlookPGPSigned.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageExpiryTimeInDays(to, subject, body, 2)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1548")
    @Test
    fun sendMessageExpiryTimeExternalContact() {
        val to = externalGmailPGPEncrypted.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageExpiryTimeInDaysWithConfirmation(to, subject, body, 2)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("21090")
    @Test
    fun sendMessageEOAndExpiryTime() {
        val to = externalOutlookPGPSigned.email
        val password = editedPassword
        val hint = editedPasswordHint
        loginRobot
            .loginTwoPasswordUser(twoPassUser)
            .decryptMailbox(password)
            .compose()
            .sendMessageEOAndExpiryTime(to, subject, body, 1, password, hint)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("21091")
    @Test
    fun sendMessageEOAndExpiryTimeWithAttachment() {
        val to = externalOutlookPGPSigned.email
        val password = editedPassword
        val hint = editedPasswordHint
        loginRobot
            .loginTwoPasswordUser(twoPassUser)
            .decryptMailbox(password)
            .compose()
            .sendMessageEOAndExpiryTimeWithAttachment(to, subject, body, 1, password, hint)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }

    }

    @Test
    fun sendMessageToInternalTrustedContactCameraCaptureAttachment() {
        val to = externalOutlookPGPSigned.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageCameraCaptureAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }

    }

    @Test
    fun sendMessageToInternalNotTrustedContactChooseAttachment() {
        val to = externalOutlookPGPSigned.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageWithFileAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }

    }

    @Test
    fun sendMessageToInternalContactWithTwoAttachments() {
        val to = externalOutlookPGPSigned.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageTwoImageCaptureAttachments(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("15539")
    @Test
    fun sendMessageToExternalContactWithOneAttachment() {
        val to = externalOutlookPGPSigned.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageCameraCaptureAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("15540")
    @Test
    fun sendMessageToExternalContactWithTwoAttachments() {
        val to = externalOutlookPGPSigned.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageTwoImageCaptureAttachments(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }
}
