/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.uitests.tests.composer

import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.editedPassword
import ch.protonmail.android.uitests.testsHelper.TestData.editedPasswordHint
import ch.protonmail.android.uitests.testsHelper.TestUser.externalGmailPGPEncrypted
import ch.protonmail.android.uitests.testsHelper.TestUser.externalOutlookPGPSigned
import ch.protonmail.android.uitests.testsHelper.TestUser.internalEmailNotTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestUser.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
import kotlin.test.BeforeTest
import kotlin.test.Test

class SendNewMessageTests : BaseTest() {

    private val loginRobot = LoginMailRobot()
    private lateinit var subject: String
    private lateinit var body: String

    @BeforeTest
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
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1546")
    @Category(SmokeTest::class)
    @Test
    fun sendMessageToInternalNotTrustedContact() {
        val to = internalEmailNotTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1543")
    @Category(SmokeTest::class)
    @Test
    fun sendExternalMessageToPGPEncryptedContact() {
        val to = externalGmailPGPEncrypted.email
        loginRobot
            .loginOnePassUser()
            .nextOnboardingScreen()
            .finishOnboarding()
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1544")
    @Category(SmokeTest::class)
    @Test
    fun sendExternalMessageToPGPSignedContact() {
        val to = externalOutlookPGPSigned.email
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("29720")
    @Test
    fun sendMessageTOandCC() {
        val to = internalEmailTrustedKeys.email
        val cc = internalEmailNotTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessageTOandCC(to, cc, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("29721")
    @Category(SmokeTest::class)
    @Test
    fun sendMessageTOandCCandBCC() {
        val to = externalGmailPGPEncrypted.email
        val cc = internalEmailTrustedKeys.email
        val bcc = internalEmailNotTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .nextOnboardingScreen()
            .finishOnboarding()
            .compose()
            .sendMessageTOandCCandBCC(to, cc, bcc, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1550")
    @Test
    fun sendMessageWithExpiryTime() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessageExpiryTimeInDays(to, subject, body, 2)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("21090")
    @Test
    fun sendMessageWithPasswordAndExpiryTime() {
        val to = internalEmailTrustedKeys.email
        val password = editedPassword
        val hint = editedPasswordHint
        loginRobot
            .loginTwoPassUser()
            .skipOnboarding()
            .compose()
            .sendMessageEOAndExpiryTime(to, subject, body, 1, password, hint)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1484")
    @Test
    fun sendMessageFromPmMe() {
        val onePassUserPmMeAddress = onePassUser.pmMe
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .changeSenderTo(onePassUserPmMeAddress)
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1542")
    @Test
    fun sendMessageWithPasswordToExternalContact() {
        val to = externalOutlookPGPSigned.email
        val password = editedPassword
        val hint = editedPasswordHint
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessageWithPassword(to, subject, body, password, hint)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1548")
    @Test
    fun sendExternalMessageWithExpiryTime() {
        val to = externalGmailPGPEncrypted.email
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessageExpiryTimeInDaysWithConfirmation(to, subject, body, 2)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("4279")
    @Test
    fun changeSenderAndSendMessageToExternalContact() {
        val onePassUserPmMeAddress = onePassUser.pmMe
        val to = externalGmailPGPEncrypted.email
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .changeSenderTo(onePassUserPmMeAddress)
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("15880")
    @Test
    fun sendMessageWithInlineImageInSignature() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginTwoPassUser()
            .skipOnboarding()
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1554")
    @Test
    fun sendMessageToExternalContactWithPasswordAndExpiryTime() {
        val to = externalOutlookPGPSigned.email
        val password = editedPassword
        val hint = editedPasswordHint
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .compose()
            .sendMessageEOAndExpiryTimeWithConfirmation(to, subject, body, 1, password, hint)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1556")
    @Test
    fun sendMessageToExternalAndInternalContactsWithPasswordAndExpiryTime() {
        val toInternal = internalEmailTrustedKeys.email
        val toExternal = externalGmailPGPEncrypted.email
        val password = editedPassword
        val hint = editedPasswordHint
        loginRobot
            .loginTwoPassUser()
            .skipOnboarding()
            .compose()
            .recipients(toInternal)
            .sendMessageEOAndExpiryTimeWithConfirmation(toExternal, subject, body, 3, password, hint)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }
}
