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

import androidx.test.filters.LargeTest
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.longClickedMessageDate
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.longClickedMessageText
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.selectedMessageDate
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.selectedMessageText
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.swipeLeftMessageDate
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.swipeLeftMessageText
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.util.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@LargeTest
class InboxTests : BaseTest() {

    private lateinit var inboxRobot: InboxRobot
    private val loginRobot = LoginRobot()

    @Before
    override fun setUp() {
        super.setUp()
        inboxRobot = loginRobot.loginUser(TestData.onePassUser)
    }

    @Test
    fun deleteMessageLongClick() {
        inboxRobot
            .longClickMessageOnPosition(0)
            .moveToTrash()
            .verify {
                messageDeleted(longClickedMessageText, longClickedMessageDate)
            }
    }

    @Test
    fun deleteMessageWithSwipe() {
        inboxRobot
            .deleteMessageWithSwipe(0)
            .verify {
                messageDeleted(swipeLeftMessageText, swipeLeftMessageDate)
            }
    }

    @Test
    fun deleteMultipleMessages() {
        inboxRobot
            .longClickMessageOnPosition(0)
            .selectMessage(1)
            .moveToTrash()
            .verify {
                messageDeleted(longClickedMessageText, longClickedMessageDate)
                messageDeleted(selectedMessageText, selectedMessageDate)
            }
    }

    @Test
    fun starMessageWithSwipe() {
        inboxRobot
            .swipeLeftMessageAtPosition(0)
            .verify { messageStarred() }
    }

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
            .verify { messageMoved(longClickedMessageText) }
    }

    //TODO create a bug - app hangs in loading state after trying to empty the trash folder
    fun emptyTrash() {
        inboxRobot
            .menuDrawer()
            .trash()
            .moreOptions()
            .emptyFolder()
            .confirm()
            .verify { folderEmpty() }
    }

    @Test
    fun messageDetailsViewHeaders() {
        inboxRobot
            .selectMessage(0)
            .moreOptions()
            .viewHeaders()
            .verify { messageHeadersDisplayed() }
    }

    @Test
    fun saveDraft() {
        val draftSubject = "Draft ${TestData.messageSubject}"
        inboxRobot
            .compose()
            .draftSubjectBody(draftSubject)
            .navigateUpToInbox()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .verify { draftMessageSaved(draftSubject) }
    }

    @Test
    fun saveDraftWithAttachment() {
        val draftSubject = "Draft ${TestData.messageSubject}"
        inboxRobot
            .compose()
            .draftSubjectBodyAttachment(draftSubject)
            .navigateUpToInbox()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .verify { draftWithAttachmentSaved(draftSubject) }
    }

    //TODO to be enabled when https://jira.protontech.ch/browse/MAILAND-662 is fixed
    fun searchDraftMessageAndSend() {
//        inboxRobot
//            .searchBar()
//            .searchMessageText("Draft")
//            .openDraftMessage()
//            .toRecipient(TestData.composerData().internalEmailAddressTrustedKeys)
//            .editSubject(TestData.composerData().messageSubject)
//            .send()
//            .verify {
//                sendingMessageToastShown()
//                messageSentToastShown()
//            }
    }

    @Test
    fun reply() {
        inboxRobot
            .selectMessage(0)
            .reply()
            .editBodyAndSend("Robot Reply ")
            .verify {
                messageSentToastShown()
            }
    }

    @Test
    fun replyMessageWithAttachment() {
        inboxRobot
            .menuDrawer()
            .labelOrFolder("Attachments")
            .selectMessage(Random().nextInt(5))
            .verifyMessageContainsAttachment()
            .reply()
            .editBodyAndSend("Robot Reply With Attachment ")
            .verify { messageSentToastShown() }
    }

    @Test
    fun replyAll() {
        inboxRobot
            .menuDrawer()
            .sent()
            .selectMessage(0)
            .replyAll()
            .editBodyAndSend("Robot ReplyAll ")
            .verify { messageSentToastShown() }
    }

    @Test
    fun forwardMessage() {
        inboxRobot
            .menuDrawer()
            .sent()
            .selectMessage(0)
            .forward()
            .sendMessageToInternalTrustedAddress()
            .verify { }
    }

    @Test
    fun forwardMessageWithAttachment() {
        inboxRobot
            .menuDrawer()
            .labelOrFolder("Attachments")
            .selectMessage(Random().nextInt(5))
            .forward()
            .sendMessageToInternalTrustedAddress()
            .verify { }
    }
}
