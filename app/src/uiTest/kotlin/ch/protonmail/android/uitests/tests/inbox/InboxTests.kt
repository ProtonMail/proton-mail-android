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
package ch.protonmail.android.uitests.tests.inbox

import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.longClickedMessageDate
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.longClickedMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.selectedMessageSubject
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestUser.externalGmailPGPEncrypted
import ch.protonmail.android.uitests.testsHelper.TestUser.externalOutlookPGPSigned
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import ch.protonmail.android.uitests.testsHelper.mailer.Mail
import kotlin.test.BeforeTest
import kotlin.test.Test

class InboxTests : BaseTest() {

    private val loginRobot = LoginMailRobot()
    private lateinit var subject: String
    private lateinit var body: String
    private lateinit var pgpEncryptedBody: String
    private lateinit var pgpSignedBody: String

    @BeforeTest
    override fun setUp() {
        super.setUp()
        subject = TestData.messageSubject
        body = TestData.messageBody
        pgpEncryptedBody = TestData.pgpEncryptedText
        pgpSignedBody = TestData.pgpSignedText
    }

    @TestId("29746")
    fun receiveMessageFromGmail() {
        val from = externalGmailPGPEncrypted
        val to = onePassUser
        Mail.gmail.from(from).to(to).withSubject(subject).withBody(body).send()
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .clickMessageBySubject(subject)
            .verify { messageWebViewContainerShown() }
    }

    @TestId("29747")
    @Test
    fun receiveMessageFromOutlook() {
        val from = externalOutlookPGPSigned
        val to = onePassUser
        Mail.outlook.from(from).to(to).withSubject(subject).withBody(body).send()
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .clickMessageBySubject(subject)
            .verify { messageWebViewContainerShown() }
    }

    @TestId("1486")
    @Test
    fun receiveMessageOnPmMe() {
        val from = externalOutlookPGPSigned
        val to = onePassUser
        Mail.outlook.from(from).to(to).withSubject(subject).withBody(body).sendToPmMe()
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .verify { messageWebViewContainerShown() }
    }

    @TestId("1487")
    @Test
    fun receiveMessageWithAttachmentOnPmMe() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .loginTwoPassUserAsSecondUser()
            .compose()
            .sendMessageWithFileAttachment(onePassUser.pmMe, subject, body)
            .menuDrawer()
            .accountsList()
            .switchToAccount(2)
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1304")
    fun receiveExternalPGPEncryptedMessage() {
        val from = externalGmailPGPEncrypted
        val to = onePassUser
        Mail.gmail.from(from).to(to).withSubject(subject).withBody(pgpEncryptedBody).send()
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .clickMessageBySubject(subject)
            .verify { pgpEncryptedMessageDecrypted() }
    }

    @TestId("1302")
    @Test
    fun receiveExternalPGPSignedMessage() {
        val from = externalOutlookPGPSigned
        val to = onePassUser
        Mail.outlook.from(from).to(to).withSubject(subject).withBody(pgpSignedBody).send()
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .clickMessageBySubject(subject)
            .verify { pgpSignedMessageDecrypted() }
    }

    @TestId("1307")
    @Test
    fun receiveMessageFromPMUser() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .loginTwoPassUserAsSecondUser()
            .compose()
            .sendMessage(onePassUser.email, subject, body)
            .menuDrawer()
            .accountsList()
            .switchToAccount(2)
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("29724")
    @Test
    fun deleteMultipleMessages() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .sent()
            .longClickMessageOnPosition(0)
            .selectMessageAtPosition(1)
            .deleteSelection()
            .confirmDeletion()
            .verify {
                multipleMessagesDeleted(longClickedMessageSubject, selectedMessageSubject)
            }
    }

    @TestId("29726")
    @Test
    fun changeMessageFolder() {
        val folder = "Folder 1"
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .sent()
            .longClickMessageOnPosition(0)
            .addFolder()
            .moveToExistingFolder(folder)
            .menuDrawer()
            .labelOrFolder(folder)
            .verify { messageWithSubjectExists(longClickedMessageSubject) }
    }

    @TestId("29722")
    @Test
    fun deleteMessageLongClick() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .sent()
            .longClickMessageOnPosition(0)
            .deleteSelection()
            .confirmDeletion()
            .verify {
                messageDeleted(longClickedMessageSubject)
            }
    }
}
