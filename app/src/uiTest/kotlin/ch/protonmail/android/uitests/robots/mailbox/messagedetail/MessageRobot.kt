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
package ch.protonmail.android.uitests.robots.mailbox.messagedetail

import android.content.Intent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.intent.matcher.UriMatchers.hasPath
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.search.SearchRobot
import ch.protonmail.android.uitests.robots.mailbox.sent.SentRobot
import ch.protonmail.android.uitests.robots.mailbox.spam.SpamRobot
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.UIActions
import ch.protonmail.android.uitests.testsHelper.click
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString

/**
 * [MessageRobot] class contains actions and verifications for Message detail view functionality.
 */
class MessageRobot {

    fun selectFolder(folderName: String): MessageRobot {
        UIActions.wait.forViewWithId(R.id.folders_list_view)
        UIActions.allOf.clickViewWithIdAndText(R.id.folder_name, folderName)
        return this
    }

    fun expandAttachments(): MessageRobot {
        UIActions.wait.forViewWithId(R.id.attachments_toggle).click()
        return this
    }

    fun clickAttachment(attachmentFileName: String): MessageRobot {
        UIActions.wait.forViewWithIdAndText(R.id.attachment_name, attachmentFileName).click()
        return this
    }

    fun moveFromSpamToFolder(folderName: String): SpamRobot {
        UIActions.wait.forViewWithId(R.id.folders_list_view)
        UIActions.allOf.clickViewWithIdAndText(R.id.folder_name, folderName)
        return SpamRobot()
    }

    fun moveToTrash(): InboxRobot {
        UIActions.wait.forViewWithId(R.id.messageWebViewContainer)
        UIActions.wait.forViewWithId(R.id.move_to_trash).click()
        return InboxRobot()
    }

    fun openFoldersModal(): MessageRobot {
        UIActions.wait.forViewWithId(R.id.add_folder).click()
        return this
    }

    fun reply(): ComposerRobot {
        UIActions.wait.forViewWithId(R.id.reply)
        UIActions.wait.untilViewWithIdEnabled(R.id.reply).click()
        return ComposerRobot()
    }

    fun replyAll(): ComposerRobot {
        UIActions.wait.forViewWithId(R.id.reply_all)
        UIActions.wait.untilViewWithIdEnabled(R.id.reply_all).click()
        return ComposerRobot()
    }

    fun forward(): ComposerRobot {
        UIActions.wait.forViewWithId(R.id.forward)
        UIActions.wait.untilViewWithIdEnabled(R.id.forward).click()
        return ComposerRobot()
    }

    fun moreOptions(): MessageMoreOptions {
        UIActions.system.clickMoreOptionsButton()
        return MessageMoreOptions()
    }

    fun navigateUpToSearch(): SearchRobot {
        UIActions.wait.forViewWithId(R.id.messageWebViewContainer)
        UIActions.system.clickHamburgerOrUpButton()
        return SearchRobot()
    }

    fun navigateUpToSent(): SentRobot {
        UIActions.wait.forViewWithId(R.id.reply_all)
        UIActions.system.clickHamburgerOrUpButton()
        return SentRobot()
    }

    fun clickSendButtonFromDrafts(): DraftsRobot {
        UIActions.id.clickViewWithId(sendMessageId)
        UIActions.wait.forViewWithText(R.string.message_sent)
        return DraftsRobot()
    }

    fun clickLoadEmbeddedImagesButton(): MessageRobot {
        UIActions.wait.forViewWithId(R.id.messageWebViewContainer)
        UIActions.wait.forViewWithIdAndAncestorId(R.id.loadContentButton, R.id.containerLoadEmbeddedImagesContainer)
            .click()
        return this
    }

    inner class MessageMoreOptions {

        fun viewHeaders(): ViewHeadersRobot {
            UIActions.allOf.clickViewWithIdAndText(R.id.title, R.string.view_headers)
            return ViewHeadersRobot()
        }
    }

    class Verify {
        fun messageContainsAttachment() {
            UIActions.wait.forViewWithId(R.id.attachment_title)
        }

        fun quotedHeaderShown() {
            UIActions.wait.forViewWithId(R.id.quoted_header)
        }

        fun attachmentsNotAdded() {
            UIActions.check.viewWithIdIsNotDisplayed(R.id.attachment_count)
        }

        fun attachmentsAdded() {
            UIActions.check.viewWithIdIsDisplayed(R.id.attachment_count)
        }

        fun pgpIconShown() {
            UIActions.wait.forViewWithId(R.id.pgp_icon)
        }

        fun pgpEncryptedMessageDecrypted() {
            UIActions.wait.forViewWithTextByUiAutomator(TestData.pgpEncryptedTextDecrypted)

        }

        fun pgpSignedMessageDecrypted() {
            UIActions.wait.forViewWithTextByUiAutomator(TestData.pgpSignedTextDecrypted)
        }

        fun messageWebViewContainerShown() {
            UIActions.wait.forViewWithId(R.id.messageWebViewContainer)
        }

        fun loadEmbeddedImagesButtonIsGone() {
            UIActions.wait.forViewWithId(R.id.messageWebViewContainer)
            UIActions.wait.untilViewWithIdIsNotShown(R.id.containerLoadEmbeddedImagesContainer)
        }

        fun intentWithActionFileNameAndMimeTypeSent(fileName: String, mimeType: String) {
            UIActions.wait.forIntent(allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(hasPath(containsString(fileName.split('.')[0]))),
                hasData(hasPath(containsString(fileName.split('.')[1]))),
                hasType(mimeType))
            )
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {
        const val sendMessageId = R.id.send_message
        const val attachmentToggleId = R.id.attachments_toggle
        const val attachmentTitleId = R.id.attachment_title
        const val attachmentNameId = R.id.attachment_name
        const val downloadAttachmentButtonId = R.id.attachment_name

    }
}
