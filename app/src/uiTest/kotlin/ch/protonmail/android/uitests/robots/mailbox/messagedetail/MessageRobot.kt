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
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.ApplyLabelRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withFolderName
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.search.SearchRobot
import ch.protonmail.android.uitests.robots.mailbox.sent.SentRobot
import ch.protonmail.android.uitests.robots.mailbox.spam.SpamRobot
import ch.protonmail.android.uitests.testsHelper.StringUtils.stringFromResource
import ch.protonmail.android.uitests.testsHelper.TestData.pgpEncryptedTextDecrypted
import ch.protonmail.android.uitests.testsHelper.TestData.pgpSignedTextDecrypted
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import ch.protonmail.android.uitests.testsHelper.uiactions.click
import me.proton.core.test.android.instrumented.CoreRobot
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf

/**
 * [MessageRobot] class contains actions and verifications for Message detail view functionality.
 */
class MessageRobot : CoreRobot {

    fun selectFolder(folderName: String): MessageRobot {
        view.withId(R.id.folder_name).withText(folderName).click()
        return this
    }

    // TODO: Rewrite this function when attachments UI on v4 is finalized - MAILAND-1545
    fun expandAttachments(): MessageRobot {
//        view.withId(R.id.attachments_toggle).click()
        return this
    }

    fun clickAttachment(attachmentFileName: String): MessageRobot {
        view.withId(R.id.attachment_name).withText(attachmentFileName).click()
        return this
    }

    fun clickLink(linkText: String): LinkNavigationDialogRobot {
        view.withId(R.id.messageWebViewContainer).wait()
        onWebView(withTagValue(`is`(messageWebViewTag)))
            .forceJavascriptEnabled()
            .withElement(DriverAtoms.findElement(Locator.LINK_TEXT, linkText))
            .perform(DriverAtoms.webClick())
        return LinkNavigationDialogRobot()
    }

    fun moveFromSpamToFolder(folderName: String): SpamRobot {
        view.withId(R.id.folder_name).withText(folderName).click()
        return SpamRobot()
    }

    fun moveToTrash(): InboxRobot {
        view.withId(R.id.move_to_trash).click()
        return InboxRobot()
    }

    fun openFoldersModal(): FoldersDialogRobot {
        view.withId(R.id.add_folder).click()
        return FoldersDialogRobot()
    }

    fun openLabelsModal(): LabelsDialogRobot {
        view.withId(R.id.add_label).click()
        return LabelsDialogRobot()
    }

    fun reply(): ComposerRobot {
        view.withId(R.id.replyActionImageView).wait()
        UIActions.wait.untilViewWithIdEnabled(R.id.replyActionImageView).click()
        return ComposerRobot()
    }

    fun replyAll(): ComposerRobot {
        view.withId(R.id.replyActionImageView).wait()
        UIActions.wait.untilViewWithIdEnabled(R.id.replyActionImageView).click()
        return ComposerRobot()
    }

    // TODO: Rewrite when action sheet is finished
    fun forward(): ComposerRobot {
//        view.withId(R.id.forward).wait()
//        UIActions.wait.untilViewWithIdEnabled(R.id.forward).click()
        return ComposerRobot()
    }

    fun moreOptions(): MessageMoreOptions {
        view.instanceOf(AppCompatImageView::class.java).isDescendantOf(view.withId(R.id.toolbar)).click()
        return MessageMoreOptions()
    }

    fun navigateUpToSearch(): SearchRobot {
        view.withId(R.id.messageWebViewContainer).wait()
        view.instanceOf(AppCompatImageButton::class.java).withParent(view.withId(R.id.toolbar)).click()
        return SearchRobot()
    }

    fun navigateUpToSent(): SentRobot {
        view.withId(R.id.replyActionImageView).wait()
        view.instanceOf(AppCompatImageButton::class.java).withParent(view.withId(R.id.toolbar)).click()
        return SentRobot()
    }

    fun navigateUpToInbox(): InboxRobot {
        view.withId(R.id.replyActionImageView).wait()
        view.instanceOf(AppCompatImageButton::class.java).withParent(view.withId(R.id.toolbar)).click()
        return InboxRobot()
    }

    fun clickSendButtonFromDrafts(): DraftsRobot {
        view.withId(sendMessageId).click()
        view.withText(R.string.message_sent).checkDisplayed()
        return DraftsRobot()
    }

    fun clickLoadEmbeddedImagesButton(): MessageRobot {
        view
            .withId(R.id.loadContentButton)
            .isDescendantOf(view.withId(R.id.containerLoadEmbeddedImagesContainer))
            .click()
        return this
    }

    class LabelsDialogRobot : ApplyLabelRobotInterface {

        override fun addLabel(name: String): LabelsDialogRobot {
            super.addLabel(name)
            return this
        }

        override fun selectLabelByName(name: String): LabelsDialogRobot {
            super.selectLabelByName(name)
            return this
        }

        override fun checkAlsoArchiveCheckBox(): LabelsDialogRobot {
            super.checkAlsoArchiveCheckBox()
            return this
        }

        override fun apply(): MessageRobot {
            super.apply()
            return MessageRobot()
        }

        override fun applyAndArchive(): InboxRobot {
            super.apply()
            return InboxRobot()
        }

        override fun closeLabelModal(): MessageRobot {
            super.closeLabelModal()
            return MessageRobot()
        }
    }

    class FoldersDialogRobot : CoreRobot {

        fun clickCreateFolder(): AddFolderRobot {
            view.withId(R.id.folders_list_view).wait()
            UIActions.listView
                .clickListItemByText(
                    withFolderName(stringFromResource(R.string.create_new_folder)),
                    R.id.folders_list_view
                )
            return AddFolderRobot()
        }

        fun moveMessageFromSpamToFolder(folderName: String): SpamRobot {
            selectFolder(folderName)
            return SpamRobot()
        }

        fun moveMessageFromSentToFolder(folderName: String): SentRobot {
            selectFolder(folderName)
            return SentRobot()
        }

        fun moveMessageFromInboxToFolder(folderName: String): InboxRobot {
            selectFolder(folderName)
            return InboxRobot()
        }

        fun moveMessageFromMessageToFolder(folderName: String): MessageRobot {
            selectFolder(folderName)
            return MessageRobot()
        }

        private fun selectFolder(folderName: String) {
            view.withId(R.id.folders_list_view).wait()
            UIActions.listView
                .clickListItemByText(
                    withFolderName(folderName),
                    R.id.folders_list_view
                )
        }

        class Verify : CoreRobot {

            fun folderExistsInFoldersList(folderName: String) {
                view.withId(R.id.folders_list_view).wait()
                UIActions.listView.checkItemWithTextExists(R.id.folders_list_view, folderName)
            }
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class AddFolderRobot : CoreRobot {

        fun addFolderWithName(name: String): FoldersDialogRobot = typeName(name).saveNewFolder()

        private fun saveNewFolder(): FoldersDialogRobot {
            view.withId(R.id.save_new_label).click()
            return FoldersDialogRobot()
        }

        private fun typeName(folderName: String): AddFolderRobot {
            view.withId(R.id.label_name).typeText(folderName)
            return this
        }
    }

    class MessageMoreOptions : CoreRobot {

        fun viewHeaders(): ViewHeadersRobot {
            view.withId(R.id.title).withText(R.string.view_headers).click()
            return ViewHeadersRobot()
        }
    }

    class LinkNavigationDialogRobot {

        class Verify : CoreRobot {

            fun linkIsPresentInDialogMessage(link: String) {
                view.withId(android.R.id.message).inRoot(rootView.isDialog()).checkContains(link)
            }
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class Verify : CoreRobot {

        fun publicKeyIsAttached(publicKey: String) {
            view.withText(publicKey).checkDisplayed()
        }

        fun messageContainsAttachment() {
            view.withId(R.id.attachmentsView).checkDisplayed()
        }

        fun messageContainsOneAttachment() {
            view.withId(R.id.attachmentsTextView).withSubstring("1 Attachment").checkDisplayed()
            view.withId(R.id.attachment_name).checkDisplayed()
        }

        fun quotedHeaderShown() {
            view.withId(R.id.quoted_header).checkDisplayed()
        }

        fun attachmentsNotAdded() {
            view.withId(R.id.attachment_count).checkNotDisplayed()
        }

        fun attachmentsAdded() {
            view.withId(R.id.attachment_count).checkDisplayed()
        }

        fun pgpIconShown() {
            view.withId(R.id.pgp_icon).checkDisplayed()
        }

        fun pgpEncryptedMessageDecrypted() {
            UIActions.wait.forViewWithTextByUiAutomator(pgpEncryptedTextDecrypted)
        }

        fun pgpSignedMessageDecrypted() {
            UIActions.wait.forViewWithTextByUiAutomator(pgpSignedTextDecrypted)
        }

        fun messageWebViewContainerShown() {
            view.withId(R.id.messageWebViewContainer).wait().checkDisplayed()
        }

        fun loadEmbeddedImagesButtonIsGone() {
            view.withId(R.id.messageWebViewContainer).wait().checkDisplayed()
            view.withId(R.id.containerLoadEmbeddedImagesContainer).checkNotDisplayed()
        }

        fun showRemoteContentButtonIsGone() {
            view.withId(R.id.messageWebViewContainer).wait().checkDisplayed()
            view.withId(R.id.containerDisplayImages).checkNotDisplayed()
        }

        fun intentWithActionFileNameAndMimeTypeSent(mimeType: String) {
            UIActions.wait.forIntent(
                allOf(
                    hasAction(Intent.ACTION_VIEW),
                    hasType(mimeType)
                )
            )
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {
        const val sendMessageId = R.id.send_message
        const val messageWebViewTag = "messageWebView"
    }
}
