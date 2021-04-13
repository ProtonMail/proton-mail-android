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
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.intent.matcher.UriMatchers.hasPath
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
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
import ch.protonmail.android.uitests.testsHelper.uiactions.type
import me.proton.core.test.android.instrumented.CoreRobot
import org.hamcrest.CoreMatchers.`is`
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

    fun clickLink(linkText: String): LinkNavigationDialogRobot {
        UIActions.wait.forViewWithId(R.id.messageWebViewContainer)
        onWebView(withTagValue(`is`(messageWebViewTag)))
            .forceJavascriptEnabled()
            .withElement(DriverAtoms.findElement(Locator.LINK_TEXT, linkText))
            .perform(DriverAtoms.webClick())
        return LinkNavigationDialogRobot()
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

    fun openFoldersModal(): FoldersDialogRobot {
        UIActions.wait.forViewWithId(R.id.messageWebViewContainer)
        UIActions.wait.forViewWithId(R.id.add_folder).click()
        return FoldersDialogRobot()
    }

    fun openLabelsModal(): LabelsDialogRobot {
        UIActions.wait.forViewWithId(R.id.messageWebViewContainer)
        UIActions.wait.forViewWithId(R.id.add_label).click()
        return LabelsDialogRobot()
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

    fun navigateUpToInbox(): InboxRobot {
        UIActions.wait.forViewWithId(R.id.reply_all)
        UIActions.system.clickHamburgerOrUpButton()
        return InboxRobot()
    }

    fun clickSendButtonFromDrafts(): DraftsRobot {
        UIActions.id.clickViewWithId(sendMessageId)
        UIActions.wait.forViewWithText(R.string.message_sent)
        return DraftsRobot()
    }

    fun clickLoadEmbeddedImagesButton(): MessageRobot {
        UIActions.wait.forViewWithId(R.id.messageWebViewContainer)
        UIActions.wait
            .forViewWithIdAndAncestorId(
                R.id.loadContentButton,
                R.id.containerLoadEmbeddedImagesContainer
            )
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
            UIActions.wait.forViewWithId(R.id.folders_list_view)
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
            UIActions.wait.forViewWithId(R.id.folders_list_view)
            UIActions.listView
                .clickListItemByText(
                    withFolderName(folderName),
                    R.id.folders_list_view
                )
        }

        class Verify {

            fun folderExistsInFoldersList(folderName: String) {
                UIActions.wait.forViewWithId(R.id.folders_list_view)
                UIActions.listView.checkItemWithTextExists(R.id.folders_list_view, folderName)
            }
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class AddFolderRobot : CoreRobot {

        fun addFolderWithName(name: String): FoldersDialogRobot = typeName(name).saveNewFolder()

        private fun saveNewFolder(): FoldersDialogRobot {
            UIActions.wait.forViewWithId(R.id.save_new_label).click()
            return FoldersDialogRobot()
        }

        private fun typeName(folderName: String): AddFolderRobot {
            UIActions.wait.forViewWithId(R.id.label_name).type(folderName)
            return this
        }
    }

    class MessageMoreOptions {

        fun viewHeaders(): ViewHeadersRobot {
            UIActions.allOf.clickViewWithIdAndText(R.id.title, R.string.view_headers)
            return ViewHeadersRobot()
        }
    }

    class LinkNavigationDialogRobot {

        class Verify : CoreRobot {

            fun linkIsPresentInDialogMessage(link: String) {
                UIActions.check.alertDialogWithPartialTextIsDisplayed(link)
            }
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class Verify : CoreRobot {

        fun publicKeyIsAttached(publicKey: String) {
            UIActions.check.viewWithTextIsDisplayed(publicKey)
        }

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
            view.withId(R.id.attachment_count).wait()
        }

        fun pgpIconShown() {
            UIActions.wait.forViewWithId(R.id.pgp_icon)
        }

        fun pgpEncryptedMessageDecrypted() {
            UIActions.wait.forViewWithTextByUiAutomator(pgpEncryptedTextDecrypted)
        }

        fun pgpSignedMessageDecrypted() {
            UIActions.wait.forViewWithTextByUiAutomator(pgpSignedTextDecrypted)
        }

        fun messageWebViewContainerShown() {
            UIActions.wait.forViewWithId(R.id.messageWebViewContainer)
        }

        fun loadEmbeddedImagesButtonIsGone() {
            view.withId(R.id.messageWebViewContainer).wait()
            view.withId(R.id.containerLoadEmbeddedImagesContainer).checkDisplayed()
        }

        fun showRemoteContentButtonIsGone() {
            UIActions.wait.forViewWithId(R.id.messageWebViewContainer)
            UIActions.wait.untilViewWithIdIsNotShown(R.id.containerDisplayImages)
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
