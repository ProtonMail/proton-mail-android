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
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.ApplyLabelRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.search.SearchRobot
import ch.protonmail.android.uitests.robots.mailbox.sent.SentRobot
import ch.protonmail.android.uitests.robots.mailbox.spam.SpamRobot
import ch.protonmail.android.uitests.robots.mailbox.trash.TrashRobot
import ch.protonmail.android.uitests.testsHelper.StringUtils
import ch.protonmail.android.uitests.testsHelper.TestData.pgpEncryptedTextDecrypted
import ch.protonmail.android.uitests.testsHelper.TestData.pgpSignedTextDecrypted
import me.proton.core.test.android.instrumented.Robot
import org.hamcrest.CoreMatchers.`is`

/**
 * [MessageRobot] class contains actions and verifications for Message detail view functionality.
 */
class MessageRobot : Robot {

    fun selectFolder(folderName: String): MessageRobot {
        view.withId(R.id.folder_name).withText(folderName).click()
        return this
    }

    fun expandAttachments(): MessageRobot {
        view.withId(R.id.attachmentsView).click()
        return this
    }

    fun clickAttachment(attachmentFileName: String): MessageRobot {
        view.withId(R.id.attachment_name_text_view).withText(attachmentFileName).click()
        return this
    }

    fun clickLink(linkText: String): LinkNavigationDialogRobot {
        view.withId(R.id.messageWebViewContainer).checkDisplayed()
        onWebView(withTagValue(`is`(messageWebViewTag)))
            .forceJavascriptEnabled()
            .withElement(DriverAtoms.findElement(Locator.LINK_TEXT, linkText))
            .perform(DriverAtoms.webClick())
        return LinkNavigationDialogRobot()
    }

    fun moveFromSpamToFolder(folderName: String): SpamRobot {
        view.withId(R.id.textview_checkbox_manage_labels_title).withText(folderName).click()
        return SpamRobot()
    }

    fun moveToTrash(): InboxRobot {
        view.withId(R.id.messageWebViewContainer).checkDisplayed()
        view.withId(R.id.thirdActionImageButton).click()
        return InboxRobot()
    }

    fun openActionSheet(): MessageActionSheet {
        view.withId(R.id.messageWebViewContainer).checkDisplayed()
        view.withId(R.id.moreActionImageButton).click()
        return MessageActionSheet()
    }

    fun navigateUpToSearch(): SearchRobot {
        view.withId(R.id.messageWebViewContainer).checkDisplayed()
        view.instanceOf(AppCompatImageButton::class.java).withParent(view.withId(R.id.toolbar)).click()
        return SearchRobot()
    }

    fun navigateUpToSent(): SentRobot {
        view.instanceOf(AppCompatImageButton::class.java).withParent(view.withId(R.id.toolbar)).click()
        return SentRobot()
    }

    fun navigateUpToInbox(): InboxRobot {
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
            .withId(R.id.loadEmbeddedImagesButton)
            .isDescendantOf(view.withId(R.id.container))
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

        override fun applyAndArchive(): MessageRobot {
            super.apply()
            return MessageRobot()
        }

        override fun closeLabelModal(): MessageRobot {
            super.closeLabelModal()
            return MessageRobot()
        }
    }

    class FoldersDialogRobot : Robot {

        fun clickCreateFolder(): AddFolderRobot {
            view.withId(R.id.labels_sheet_new_folder_text_view).click()
            return AddFolderRobot()
        }

        fun moveMessageFromSpamToFolder(folderName: String): SpamRobot {
            selectFolder(folderName)
            return SpamRobot()
        }

        fun moveMessageFromTrashToFolder(folderName: String): TrashRobot {
            selectFolder(folderName)
            return TrashRobot()
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
            view.withId(R.id.action_sheet_header).swipeUp()
            view.withId(R.id.textview_checkbox_manage_labels_title).withText(folderName).swipeRight().click()
        }

        class Verify : Robot {

            fun folderExistsInFoldersList(folderName: String) {
                view.withId(R.id.labels_sheet_recyclerview).checkDisplayed()
                listView
                    .onListItem(withText(folderName))
                    .inAdapterView(view.withId(R.id.labels_sheet_recyclerview))
                    .checkDisplayed()
            }
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class AddFolderRobot : Robot {

        fun addFolderWithName(name: String): FoldersDialogRobot = typeName(name).saveNewFolder()

        private fun saveNewFolder(): FoldersDialogRobot {
            view.withId(R.id.add_folder).click()
            return FoldersDialogRobot()
        }

        private fun typeName(folderName: String): AddFolderRobot {
            view.withId(R.id.add_label).typeText(folderName)
            return this
        }
    }

    class MessageActionSheet : Robot {

        fun reply(): ComposerRobot {
            view.withId(R.id.text_view_details_actions_reply).click()
            return ComposerRobot()
        }

        fun replyAll(): ComposerRobot {
            view.withId(R.id.text_view_details_actions_reply_all).click()
            return ComposerRobot()
        }

        fun forward(): ComposerRobot {
            view.withId(R.id.text_view_details_actions_forward).click()
            return ComposerRobot()
        }

        fun openFoldersModal(): FoldersDialogRobot {
            val actionSheetNestedScrollView = UiScrollable(UiSelector().scrollable(true))

            actionSheetNestedScrollView.scrollToEnd(2)
            view.withId(R.id.text_view_details_actions_move_to).click()
            return FoldersDialogRobot()
        }

        fun openLabelsModal(): LabelsDialogRobot {
            view.withId(R.id.text_view_details_actions_label_as).click()
            return LabelsDialogRobot()
        }

        fun viewHeaders(): ViewHeadersRobot {
            val actionSheetNestedScrollView = UiScrollable(UiSelector().scrollable(true))

            actionSheetNestedScrollView.scrollToEnd(2)
            view.withId(R.id.text_view_details_actions_view_headers).click()
            return ViewHeadersRobot()
        }
    }

    class LinkNavigationDialogRobot {

        class Verify : Robot {

            fun linkIsPresentInDialogMessage(link: String) {
//                view.withId(android.R.id.message).inRoot(rootView.isDialog()).checkContains(link)
            }
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class Verify : Robot {

        fun labelAdded(labelName: String) {
            view.withId(R.id.label_layer_inner).withText(labelName).checkDisplayed()
        }

        fun publicKeyIsAttached(publicKey: String) {
            view.withText(publicKey).checkDisplayed()
        }

        fun messageContainsAttachment() {
            view.withId(R.id.attachmentsView).checkDisplayed()
        }

        fun messageContainsOneAttachment() {
            val oneAttachmentString = StringUtils.quantityStringFromResource(R.plurals.attachments_number, 1)
            view.withId(R.id.attachments_text_view).withSubstring(oneAttachmentString).checkDisplayed()
            view.withId(R.id.attachment_name_text_view).checkDisplayed()
        }

        fun messageContainsTwoAttachments() {
            val twoAttachmentString = StringUtils.quantityStringFromResource(R.plurals.attachments_number, 2)
                .replace("%d", "2")
            view.withId(R.id.attachments_text_view).withSubstring(twoAttachmentString).checkDisplayed()
        }

        fun quotedHeaderShown() {
            view.withId(R.id.headerView).checkDisplayed()
        }

        fun attachmentsNotAdded() {
            view.withId(R.id.composer_attachments_count_text_view).checkNotDisplayed()
        }

        fun attachmentsAdded() {
            view.withId(R.id.composer_attachments_count_text_view).checkDisplayed()
        }

        fun pgpIconShown() {
            view.withId(R.id.pgp_icon).checkDisplayed()
        }

        fun pgpEncryptedMessageDecrypted() {
            device.waitForObjectByText(pgpEncryptedTextDecrypted)
        }

        fun pgpSignedMessageDecrypted() {
            device.waitForObjectByText(pgpSignedTextDecrypted)
        }

        fun messageWebViewContainerShown() {
            view.withId(R.id.messageWebViewContainer).checkDisplayed()
        }

        fun loadEmbeddedImagesButtonIsGone() {
            view.withId(R.id.messageWebViewContainer).checkDisplayed()
            view.withId(R.id.embedded_image_attachment).checkNotDisplayed()
        }

        fun showRemoteContentButtonIsGone() {
            view.withId(R.id.messageWebViewContainer).checkDisplayed()
            view.withId(R.id.embedded_image_attachment).checkNotDisplayed()
        }

        fun intentWithActionFileNameAndMimeTypeSent(mimeType: String) {
            intent.hasAction(Intent.ACTION_VIEW).hasType(mimeType).checkSent()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {

        const val sendMessageId = R.id.send_message
        const val messageWebViewTag = "messageWebView"
    }
}
