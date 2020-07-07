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
package ch.protonmail.android.uitests.tests.mailbox

import androidx.test.filters.LargeTest
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestUser
import ch.protonmail.android.uitests.testsHelper.UICustomViewActionsAndMatchers.waitUntilObjectWithIdAppears
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runners.MethodSorters
import java.util.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@LargeTest
class MailboxTests : BaseTest() {

    private val mailboxRobot = MailboxRobot()
    private val loginRobot = LoginRobot()

    @Before
    override fun setUp() {
        super.setUp()
        loginRobot
            .loginUser(TestUser.onePassUser())
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun searchFindMessage() {
        mailboxRobot
            .openSearchBar()
            .searchMessageText(TestData.searchMessageSubject)
            .verify { searchedMessageFound() }
    }

    @Test
    fun searchDontFindMessage() {
        mailboxRobot
            .openSearchBar()
            .searchMessageText(TestData.searchMessageSubjectNotFound)
            .verify { searchedMessageNotFound() }
    }

    @Test
    fun deleteMessageLongClick() {
        mailboxRobot
            .goToFolderSent()
            .longClickMessage(mailboxRobot.positionOfNotDeletedMessage())
            .moveToTrash()
            .verify { messageDeleted() }
    }

    @Test
    fun deleteMessageWithSwipe() {
        mailboxRobot
            .goToFolderSent()
            .deleteMessageWithSwipe()
            .verify { messageDeleted() }
    }

    @Test
    fun deleteMultipleMessages() {
        mailboxRobot
            .goToFolderSent()
            .longClickMessage(mailboxRobot.positionOfNotDeletedMessage())
            .selectMessage(mailboxRobot.positionOfNotDeletedMessage() + 1)
            .moveToTrash()
            .verify { multipleMessagesDeleted() }
    }

    @Test
    fun starMessageWithSwipe() {
        mailboxRobot
            .goToFolderSent()
            .swipeLeftMessageAtPosition(0)
            .verify { messageStarred() }
    }

    @Test
    fun changeMessageFolder() {
        val messageSubject = mailboxRobot.goToFolderSent()
            .getMessageSubject(mailboxRobot.positionOfNotMovedMessage())
        mailboxRobot
            .longClickMessage(mailboxRobot.positionOfNotMovedMessage())
            .openFoldersModal()
            .selectFolder("FOLDER 1")
            .openNavbar()
            .openFolderAtNavbarPosition(15) //position of "FOLDER 1"
            .verify { messageMoved(messageSubject) }
    }

    @Test
    fun emptyTrash() {
        mailboxRobot.openNavbar().openFolder("Trash")
        mailboxRobot
            .moreOptions()
            .emptyFolder()
            .confirm()
            .verify { folderEmpty() }
    }

    @Test
    fun clearCache() {
        mailboxRobot
            .openNavbar()
            .openSettings()
            .emptyCache()
            .verify { cacheCleared() }
    }

    @Test
    fun messageDetailsViewHeaders() {
        mailboxRobot
            .goToFolderSent()
            .selectMessage(0)
            .moreOptions()
            .viewHeaders()
            .verify { messageHeadersDisplayed() }
    }

    @Test
    fun saveDraft() {
        val draftSubject = TestData.composerData().messageSubject
        mailboxRobot
            .compose()
            .draftSubjectBody(draftSubject)
            .navigateUpToInbox()
            .confirm()
            .openNavbar()
            .openFolder("Drafts")
            .verify { draftMessageSaved(draftSubject) }
    }

    @Test
    fun saveDraftWithAttachment() {
        val draftSubject = TestData.composerData().messageSubject
        mailboxRobot
            .compose()
            .draftSubjectBodyAttachment(TestData.composerData().messageSubject)
            .navigateUpToInbox()
            .confirm()
            .openNavbar()
            .openFolder("Drafts")
            .verify { draftWithAttachmentSaved(draftSubject) }
    }

    @Test
    @Ignore
    //TODO to be enabled when https://jira.protontech.ch/browse/MAILAND-662 is fixed
    fun searchDraftMessageAndSend() {
        mailboxRobot
            .openSearchBar()
            .searchMessageText("Draft")
            .openDraftMessage()
            .toRecipient(TestData.composerData().internalEmailAddressTrustedKeys)
            .editSubject(TestData.composerData().messageSubject)
            .send()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun reply() {
        mailboxRobot
            .goToFolderSent()
            .selectMessage(mailboxRobot.positionNotInTrashNoAttachment())
            .reply()
            .editBody("Robot Reply ")
            .verifyQuotedHeaderShown()
            .send()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun replyMessageWithAttachment() {
        mailboxRobot
            .openNavbar()
            .openFolderAtNavbarPosition(16)
            .selectMessage(Random().nextInt(5))
            .verifyMessageContainsAttachment()
            .reply()
            .editBody("Robot Reply With Attachment ")
            .verifyAttachmentsNotAdded()
            .verifyQuotedHeaderShown()
            .send()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun replyAll() {
        mailboxRobot
            .goToFolderSent()
            .selectMessage(mailboxRobot.positionOfNotDeletedMessage())
            .replyAll()
            .editBody("Robot ReplyAll ")
            .verifyQuotedHeaderShown()
            .send()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun forwardMessage() {
        mailboxRobot
            .goToFolderSent()
            .selectMessage(mailboxRobot.positionNotInTrashNoAttachment())
            .forward()
            .toRecipient(TestData.composerData().internalEmailAddressTrustedKeys)
            .editBody("Robot Forward ")
            .verifyQuotedHeaderShown()
            .send()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun forwardMessageWithAttachment() {
        mailboxRobot.openNavbar().openFolderAtNavbarPosition(16)
        mailboxRobot
            .selectMessage(Random().nextInt(5))
            .verifyMessageContainsAttachment()
            .forward()
            .toRecipient(TestData.composerData().internalEmailAddressTrustedKeys)
            .editBody("Robot Forward With Attachment ")
            .verifyAttachmentsAdded()
            .verifyQuotedHeaderShown()
            .send()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }
}