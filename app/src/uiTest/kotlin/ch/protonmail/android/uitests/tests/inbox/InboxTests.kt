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
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class InboxTests : BaseTest() {

    private lateinit var inboxRobot: InboxRobot
    private val loginRobot = LoginRobot()
    private lateinit var subject: String
    private lateinit var body: String

    @Before
    override fun setUp() {
        super.setUp()
        subject = TestData.messageSubject
        body = TestData.messageBody
        inboxRobot = loginRobot.loginUser(TestData.onePassUser)
    }

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

    @Test
    fun starMessageWithSwipe() {
        inboxRobot
            .menuDrawer()
            .sent()
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
            .verify { messageMoved(longClickedMessageSubject) }
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
            .menuDrawer()
            .sent()
            .clickMessageByPosition(0)
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
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .verify { draftMessageSaved(draftSubject) }
    }

    @Category(SmokeTest::class)
    @Test
    fun saveDraftWithAttachment() {
        val draftSubject = "Draft ${TestData.messageSubject}"
        inboxRobot
            .compose()
            .draftSubjectBodyAttachment(draftSubject)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .verify { draftWithAttachmentSaved(draftSubject) }
    }
}
