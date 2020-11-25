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
package ch.protonmail.android.uitests.tests.inbox

import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.longClickedMessageDate
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.longClickedMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.selectedMessageDate
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.selectedMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.swipeLeftMessageDate
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.swipeLeftMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.externalGmailPGPEncrypted
import ch.protonmail.android.uitests.testsHelper.TestData.externalOutlookPGPSigned
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.mailer.Mail
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class InboxTests : BaseTest() {

    private lateinit var inboxRobot: InboxRobot
    private val loginRobot = LoginRobot()
    private lateinit var subject: String
    private lateinit var body: String
    private lateinit var pgpEncryptedBody: String
    private lateinit var pgpSignedBody: String

    @Before
    override fun setUp() {
        super.setUp()
        subject = TestData.messageSubject
        body = TestData.messageBody
        pgpEncryptedBody = TestData.pgpEncryptedText
        pgpSignedBody = TestData.pgpSignedText
        inboxRobot = loginRobot.loginUser(onePassUser)
    }

    @TestId("29746")
    @Test
    fun receiveMessageFromGmail() {
        val from = externalGmailPGPEncrypted
        val to = onePassUser
        Mail.gmail.from(from).to(to).withSubject(subject).withBody(body).send()
        inboxRobot
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .verify { messageWebViewContainerShown() }
    }

    @TestId("29747")
    @Test
    fun receiveMessageFromOutlook() {
        val from = externalOutlookPGPSigned
        val to = onePassUser
        Mail.outlook.from(from).to(to).withSubject(subject).withBody(body).send()
        inboxRobot
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .verify { messageWebViewContainerShown() }
    }

    @TestId("1486")
    @Test
    fun receiveMessageOnPmMe() {
        val from = externalOutlookPGPSigned
        val to = onePassUser
        Mail.outlook.from(from).to(to).withSubject(subject).withBody(body).sendToPmMe()
        inboxRobot
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .verify { messageWebViewContainerShown() }
    }

    @TestId("1487")
    @Test
    fun receiveMessageWithAttachmentOnPmMe() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(TestData.twoPassUser)
            .compose()
            .sendMessageWithFileAttachment(onePassUser.pmMe, subject, body)
            .menuDrawer()
            .accountsList()
            .switchToAccount(onePassUser.email)
            .inbox()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1304")
    @Test
    fun receiveExternalPGPEncryptedMessage() {
        val from = externalGmailPGPEncrypted
        val to = onePassUser
        Mail.gmail.from(from).to(to).withSubject(subject).withBody(pgpEncryptedBody).send()
        inboxRobot
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .verify { pgpEncryptedMessageDecrypted() }
    }

    @TestId("1302")
    @Test
    fun receiveExternalPGPSignedMessage() {
        val from = externalOutlookPGPSigned
        val to = onePassUser
        Mail.outlook.from(from).to(to).withSubject(subject).withBody(pgpSignedBody).send()
        inboxRobot
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .verify { pgpSignedMessageDecrypted() }
    }

    @TestId("1307")
    @Test
    fun receiveMessageFromPMUser() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(TestData.twoPassUser)
            .compose()
            .sendMessage(onePassUser.email, subject, body)
            .menuDrawer()
            .accountsList()
            .switchToAccount(onePassUser.email)
            .inbox()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("29723")
    @Category(SmokeTest::class)
    @Test
    fun deleteMessageWithSwipe() {
        inboxRobot
            .menuDrawer()
            .sent()
            .deleteMessageWithSwipe(1)
            .verify {
                messageDeleted(swipeLeftMessageSubject, swipeLeftMessageDate)
            }
    }

    @TestId("29724")
    @Test
    fun deleteMultipleMessages() {
        inboxRobot
            .menuDrawer()
            .sent()
            .longClickMessageOnPosition(0)
            .selectMessage(1)
            .moveToTrash()
            .verify {
                messageDeleted(longClickedMessageSubject, longClickedMessageDate)
                messageDeleted(selectedMessageSubject, selectedMessageDate)
            }
    }

    @TestId("29725")
    @Test
    fun starMessageWithSwipe() {
        inboxRobot
            .menuDrawer()
            .sent()
            .swipeLeftMessageAtPosition(0)
            .verify { messageStarred() }
    }

    @TestId("29726")
    @Test
    fun changeMessageFolder() {
        val folder = "Folder 1"
        inboxRobot
            .menuDrawer()
            .sent()
            .longClickMessageOnPosition(0)
            .addFolder()
            .moveToExistingFolder(folder)
            .menuDrawer()
            .labelOrFolder(folder)
            .verify { messageMoved(longClickedMessageSubject) }
    }

    @Test
    fun messageDetailsViewHeaders() {
        inboxRobot
            .menuDrawer()
            .sent()
            .clickMessageByPosition(0)
            .moreOptions()
            .viewHeaders()
            .verify { messageHeadersDisplayed() }
    }

    @TestId("29722")
    @Test
    fun deleteMessageLongClick() {
        inboxRobot
            .menuDrawer()
            .sent()
            .longClickMessageOnPosition(0)
            .moveToTrash()
            .verify {
                messageDeleted(longClickedMessageSubject, longClickedMessageDate)
            }
    }
}
