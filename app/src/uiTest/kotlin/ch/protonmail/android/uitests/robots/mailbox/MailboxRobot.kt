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
package ch.protonmail.android.uitests.robots.mailbox

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.composer.ComposerRobot
import ch.protonmail.android.uitests.testsHelper.MockAddAttachmentIntent.mockCameraImageCapture
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [MailboxRobot] class contains actions and verifications for Mailbox functionality.
 */
open class MailboxRobot : UIActions() {

    fun goToFolderSent(): MailboxRobot {
        clickOnObjectWithContentDescription("Open")
        clickOnObjectWithIdAndText(R.id.label, "Sent")
        waitWithTimeoutForObjectWithIdToAppear(R.id.messages_list_view, 2000)
        return this
    }

    fun openSearchBar(): MailboxRobot {
        waitUntilObjectWithIdAppearsInView(R.id.search)
        clickOnObjectWithId(R.id.search)
        return this
    }

    fun searchMessageText(messageSubject: String?): MailboxRobot {
        waitUntilObjectWithIdAppearsInView(R.id.search_src_text)
        insertTextIntoFieldWithIdAndPressImeAction(R.id.search_src_text, messageSubject)
        return this
    }

    fun positionOfNotDeletedMessage(): Int {
        return positionOfObjectWhichNotContainsObjectWithId(R.id.messages_list_view, R.id.messageLabelTrashTextView)
    }

    fun selectMessage(position: Int): MailboxRobot {
        waitWithTimeoutForObjectWithIdToAppear(R.id.messages_list_view, 10000)
        clickChildInRecyclerView(R.id.messages_list_view, position)
        return this
    }

    fun longClickMessage(position: Int): MailboxRobot {
        longClickItemInRecyclerView(R.id.messages_list_view, position)
        return this
    }

    fun moveToTrash(): MailboxRobot {
        clickOnObjectWithIdAndContentDescription(R.id.move_to_trash, R.string.move_to_trash)
        return this
    }

    fun deleteMessageWithSwipe(): MailboxRobot {
        swipeLeftToRightObjectWithId(R.id.messages_list_view,
            positionOfObjectWhichNotContainsObjectWithId(R.id.messages_list_view, R.id.messageLabelTrashTextView))
        return this
    }

    fun swipeLeftMessageAtPosition(messagePosition: Int): MailboxRobot {
        waitWithTimeoutForObjectWithIdToAppear(R.id.messageTitleTextView, 5000)
        swipeRightToLeftObjectWithIdAtPosition(R.id.messages_list_view, messagePosition)
        return this
    }

    fun positionOfNotMovedMessage(): Int {
        return positionOfObjectWhichNotContainsObjectWithId(R.id.messages_list_view, R.id.messageLocationTextView)
    }

    fun getMessageSubject(positionOfNotMovedMessage: Int): String? {
        return getTextFromObjectInRecyclerViewAtPosition(R.id.messages_list_view, R.id.messageTitleTextView, positionOfNotMovedMessage)
    }

    fun openFoldersModal(): MailboxRobot {
        waitUntilObjectWithIdAppearsInView(R.id.add_folder)
        clickOnObjectWithId(R.id.add_folder)
        return this
    }

    fun selectFolder(folderName: String?): MailboxRobot {
        waitUntilObjectWithIdAppearsInView(R.id.folders_list_view)
        clickOnObjectWithIdAndText(R.id.folder_name, folderName)
        return this
    }

    fun openNavbar(): MailboxRobot {
        clickOnObjectWithContentDescription("Open")
        waitWithTimeoutForObjectWithIdToAppear(R.id.left_drawer_navigation, 10000)
        return this
    }

    fun openFolderAtNavbarPosition(position: Int): MailboxRobot {
        clickChildInRecyclerView(R.id.left_drawer_navigation, position)
        return this
    }

    fun openFolder(folderName: String?): MailboxRobot {
        clickOnObjectWithIdAndText(R.id.label, folderName)
        return this
    }

    fun moreOptions(): MailboxRobot {
        waitUntilObjectWithContentDescriptionAppearsInView("More options")
        clickOnObjectWithContentDescription("More options")
        return this
    }

    fun emptyFolder(): MailboxRobot {
        clickOnObjectWithIdAndText(R.id.title, R.string.empty_folder)
        return this
    }

    fun confirm(): MailboxRobot {
        clickOnObjectWithText("YES")
        return this
    }

    fun openSettings(): MailboxRobot {
        clickChildInRecyclerView(R.id.left_drawer_navigation, 10)
        return this
    }

    fun emptyCache(): MailboxRobot {
        clickEmptyCacheButton(R.id.clearCacheButton)
        return this
    }

    fun viewHeaders(): MailboxRobot {
        waitWithTimeoutForObjectWithIdToAppear(R.id.title, 3000)
        clickOnObjectWithIdAndText(R.id.title, R.string.view_headers)
        return this
    }

    fun compose(): MailboxRobot {
        clickOnObjectWithId(R.id.compose)
        return this
    }

    fun draftSubject(messageSubject: String): MailboxRobot {
        clickOnObjectWithId(R.id.message_title)
        typeTextIntoField(R.id.message_title, "Draft: $messageSubject")
        return this
    }

    fun body(messageBody: String?): MailboxRobot {
        insertTextIntoFieldWithId(R.id.message_body, messageBody)
        return this
    }

    fun draftSubject(): String? {
        return getTextFromObject(R.id.message_title)
    }

    fun navigateUpToInbox(): MailboxRobot {
        clickChildInViewGroup(R.id.toolbar, 0)
        return this
    }

    fun navigateUpToComposerView(): MailboxRobot {
        clickChildInViewGroup(R.id.toolbar, 1)
        return this
    }

    fun attachments(): MailboxRobot {
        clickOnObjectWithId(R.id.add_attachments)
        return this
    }

    fun addAttachment(): MailboxRobot {
        mockCameraImageCapture(R.id.take_photo, R.drawable.logo)
        return this
    }

    fun openDraftMessage(): MailboxRobot {
        waitWithTimeoutForObjectWithIdToAppear(R.id.messageTitleTextView, 10000)
        val positionOfDraftMessage = positionOfObjectWhichContainsObjectWithIdAndText(R.id.messages_list_view,
            R.id.messageLocationTextView, R.string.drafts_option)
        clickChildInRecyclerView(R.id.messages_list_view, positionOfDraftMessage)
        return this
    }

    fun toRecipient(recipient: String?): MailboxRobot {
        clickOnObjectWithId(R.id.message_title)
        typeTextIntoField(R.id.to_recipients, recipient)
        return this
    }

    fun editSubject(messageSubject: String?): MailboxRobot {
        insertTextIntoFieldWithId(R.id.message_title, "Draft Edit: ")
        clickOnObjectWithId(R.id.message_title)
        typeTextIntoField(R.id.message_title, messageSubject)
        return this
    }

    fun editBody(messageBody: String?): MailboxRobot {
        waitWithTimeoutForObjectWithIdToAppear(R.id.message_title, 10000)
        insertTextIntoFieldWithId(R.id.message_body, messageBody)
        typeTextIntoField(R.id.message_body, messageBody)
        return this
    }


    fun send(): ComposerRobot {
        clickOnObjectWithId(R.id.send_message)
        return ComposerRobot()
    }

    fun reply(): MailboxRobot {
        waitWithTimeoutForObjectWithIdIsClickable(R.id.reply, 10000)
        clickOnObjectWithId(R.id.reply)
        return this
    }

    fun replyAll(): MailboxRobot {
        waitWithTimeoutForObjectWithIdToAppear(R.id.reply_all, 10000)
        clickOnObjectWithId(R.id.reply_all)
        return this
    }

    fun forward(): MailboxRobot {
        waitWithTimeoutForObjectWithIdIsClickable(R.id.forward, 10000)
        clickOnObjectWithId(R.id.forward)
        return this
    }

    fun verifyMessageContainsAttachment(): MailboxRobot {
        waitUntilObjectWithIdAppearsInView(R.id.attachment_title)
        return this
    }

    fun verifyQuotedHeaderShown(): MailboxRobot {
        waitUntilObjectWithIdAppearsInView(R.id.quoted_header)
        return this
    }

    fun verifyAttachmentsAdded(): MailboxRobot {
        checkIfObjectWithIdIsDisplayed(R.id.attachment_count)
        return this
    }

    fun verifyAttachmentsNotAdded(): MailboxRobot {
        checkIfObjectWithIdNotDisplayed(R.id.attachment_count)
        return this
    }

    fun positionNotInTrashNoAttachment(): Int {
        return positionOfObjectWhichNotContainsAnyOfGivenElements(R.id.messages_list_view, R.id.messageLabelTrashTextView,
            R.id.messageAttachmentTextView)
    }

    /**
     * Contains all the validations that can be performed by [MailboxRobot].
     */
    inner class Verify : MailboxRobot() {

        fun searchedMessageFound(): MailboxRobot {
            waitWithTimeoutForObjectWithIdToAppear(R.id.messageTitleTextView, 10000)
            checkIfObjectWithIdHasDescendantWithSubstring(R.id.messages_list_view, TestData.searchMessageSubject)
            return this
        }

        fun searchedMessageNotFound(): MailboxRobot {
            waitWithTimeoutForObjectWithIdAndTextToAppear(R.id.no_messages, R.string.no_search_results, 10000)
            return this
        }

        fun messageDeleted(): MailboxRobot {
            waitUntilObjectWithIdAppearsInView(R.id.snackbar_text)
            checkIfObjectWithIdAndTextIsDisplayed(R.id.snackbar_text, "Message moved to trash")
            checkIfObjectWithPositionInRecyclerViewIsDisplayed(R.id.messages_list_view,
                positionOfObjectWhichNotContainsObjectWithId(R.id.messages_list_view, R.id.messageLabelTrashTextView) - 1,
                R.id.messageLabelTrashTextView)
            return this
        }

        fun multipleMessagesDeleted(): MailboxRobot {
            waitUntilObjectWithIdAppearsInView(R.id.snackbar_text)
            checkIfObjectWithIdAndTextIsDisplayed(R.id.snackbar_text, "Messages moved to trash")
            checkIfObjectWithPositionInRecyclerViewIsDisplayed(R.id.messages_list_view,
                positionOfObjectWhichNotContainsObjectWithId(R.id.messages_list_view, R.id.messageLabelTrashTextView) - 1,
                R.id.messageLabelTrashTextView)
            return this
        }

        fun messageStarred(): MailboxRobot {
            waitUntilObjectWithIdAppearsInView(R.id.snackbar_text)
            checkIfObjectWithIdAndTextIsDisplayed(R.id.snackbar_text, "Message star updated")
            return this
        }

        fun messageMoved(messageSubject: String?): MailboxRobot {
            waitWithTimeoutForObjectWithIdAndTextToAppear(R.id.messageTitleTextView, messageSubject!!, 10000)
            return this
        }

        fun folderEmpty(): MailboxRobot {
            waitWithTimeoutForObjectWithIdAndTextToAppear(R.id.no_messages, R.string.no_messages, 50000)
            return this
        }

        fun cacheCleared(): MailboxRobot {
            checkIfToastMessageIsDisplayed(R.string.processing_request)
            checkIfToastMessageIsDisplayed(R.string.cache_cleared)
            clickChildInViewGroup(R.id.toolbar, 1)
            waitWithTimeoutForObjectWithIdToAppear(R.id.messages_list_view, 5000)
            return this
        }

        fun messageHeadersDisplayed(): MailboxRobot {
            checkIfObjectWithIdIsDisplayed(R.id.viewHeadersText)
            return this
        }

        fun draftMessageSaved(draftSubject: String?): MailboxRobot {
            waitWithTimeoutForObjectWithIdAndTextToAppear(R.id.messageTitleTextView, draftSubject!!, 10000)
            return this
        }

        fun draftWithAttachmentSaved(draftSubject: String?): MailboxRobot {
            waitWithTimeoutForObjectWithIdAndTextToAppear(R.id.messageTitleTextView, draftSubject!!, 10000)
            checkIfObjectWithPositionInRecyclerViewIsDisplayed(R.id.messages_list_view, 0, R.id.messageAttachmentTextView)
            return this
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block) as MailboxRobot

}