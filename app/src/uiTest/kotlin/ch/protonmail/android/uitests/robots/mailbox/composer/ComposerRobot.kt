/*
 * Copyright (c) 2022 Proton Technologies AG
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
package ch.protonmail.android.uitests.robots.mailbox.composer

import androidx.appcompat.widget.AppCompatImageButton
import androidx.test.rule.ActivityTestRule
import ch.protonmail.android.R
import ch.protonmail.android.mailbox.presentation.ui.MailboxActivity
import ch.protonmail.android.uitests.robots.contacts.ContactsRobot
import ch.protonmail.android.uitests.robots.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitests.robots.mailbox.sent.SentRobot
import ch.protonmail.android.uitests.testsHelper.TestData
import me.proton.core.test.android.instrumented.Robot

/**
 * [ComposerRobot] class contains actions and verifications for email composer functionality.
 * Inner classes: [MessagePasswordRobot], [MessageExpirationRobot].
 */
class ComposerRobot : Robot {

    fun sendAndLaunchApp(to: String, subject: String, body: String): InboxRobot =
        recipients(to)
            .subject(subject)
            .body(body)
            .sendAndLaunchApp()

    fun sendMessage(to: String, subject: String, body: String): InboxRobot =
        recipients(to)
            .subject(subject)
            .body(body)
            .send()

    fun sendMessageToContact(subject: String, body: String): ContactsRobot =
        subject(subject)
            .body(body)
            .sendToContact()

    fun sendMessageToGroup(subject: String, body: String): ContactsRobot.ContactsGroupView {
        subject(subject)
            .body(body)
            .sendToContact()
        return ContactsRobot.ContactsGroupView()
    }

    fun forwardMessage(to: String, body: String): MessageRobot =
        recipients(to)
            .body(body)
            .forward()

    fun changeSubjectAndForwardMessage(to: String, subject: String): MessageRobot =
        recipients(to)
            .updateSubject(subject)
            .forward()

    fun sendMessageTOandCC(to: String, cc: String, subject: String, body: String): InboxRobot =
        recipients(to)
            .showAdditionalRows()
            .ccRecipients(cc)
            .subject(subject)
            .body(body)
            .send()

    fun sendMessageTOandCCandBCC(to: String, cc: String, bcc: String, subject: String, body: String): InboxRobot =
        recipients(to)
            .showAdditionalRows()
            .ccRecipients(cc)
            .bccRecipients(bcc)
            .subject(subject)
            .body(body)
            .send()

    fun sendMessageWithPassword(to: String, subject: String, body: String, password: String, hint: String): InboxRobot =
        composeMessage(to, subject, body)
            .setMessagePassword()
            .definePasswordWithHint(password, hint)
            .send()

    fun sendMessageExpiryTimeInDays(to: String, subject: String, body: String, days: Int): InboxRobot =
        composeMessage(to, subject, body)
            .messageExpiration()
            .setExpirationInDays(days)
            .send()

    fun sendMessageExpiryTimeInDaysWithConfirmation(to: String, subject: String, body: String, days: Int): InboxRobot =
        composeMessage(to, subject, body)
            .messageExpiration()
            .setExpirationInDays(days)
            .sendWithNotSupportedExpiryConfirmation()
            .sendAnyway()

    fun sendMessageEOAndExpiryTime(
        to: String,
        subject: String,
        body: String,
        days: Int,
        password: String,
        hint: String
    ): InboxRobot {
        return composeMessage(to, subject, body)
            .setMessagePassword()
            .definePasswordWithHint(password, hint)
            .messageExpiration()
            .setExpirationInDays(days)
            .send()
    }

    fun sendMessageEOAndExpiryTimeWithConfirmation(
        to: String,
        subject: String,
        body: String,
        days: Int,
        password: String,
        hint: String
    ): InboxRobot {
        return composeMessage(to, subject, body)
            .setMessagePassword()
            .definePasswordWithHint(password, hint)
            .messageExpiration()
            .setExpirationInDays(days)
            .sendWithNotSupportedExpiryConfirmation()
            .sendAnyway()
    }

    fun sendMessageEOAndExpiryTimeWithAttachment(
        to: String,
        subject: String,
        body: String,
        days: Int,
        password: String,
        hint: String
    ): InboxRobot =
        composeMessage(to, subject, body)
            .setMessagePassword()
            .definePasswordWithHint(password, hint)
            .messageExpiration()
            .setExpirationInDays(days)
            .hideExpirationView()
            .attachments()
            .addImageCaptureAttachment(logoDrawable)
            .send()

    fun sendMessageEOAndExpiryTimeWithAttachmentAndConfirmation(
        to: String,
        subject: String,
        body: String,
        days: Int,
        password: String,
        hint: String
    ): InboxRobot =
        composeMessage(to, subject, body)
            .setMessagePassword()
            .definePasswordWithHint(password, hint)
            .messageExpiration()
            .setExpirationInDays(days)
            .hideExpirationView()
            .attachments()
            .addImageCaptureAttachment(logoDrawable)
            .sendWithNotSupportedExpiryConfirmation()
            .sendAnyway()

    fun sendMessageCameraCaptureAttachment(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addImageCaptureAttachment(logoDrawable)
            .send()

    fun sendMessageWithFileAttachment(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addFileAttachment(logoDrawable)
            .goBackToComposer()
            .send()

    fun sendMessageTwoImageCaptureAttachments(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addTwoImageCaptureAttachments(logoDrawable, welcomeDrawable)
            .send()

    fun addAndRemoveAttachmentAndSend(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addFileAttachment(logoDrawable)
            .removeLastAttachment()
            .goBackToComposer()
            .send()

    fun draftToSubjectBody(to: String, messageSubject: String, body: String): ComposerRobot =
        recipients(to)
            .subject(messageSubject)
            .body(TestData.messageBody)

    fun draftToBody(to: String, body: String): ComposerRobot =
        recipients(to)
            .body(body)

    fun draftSubjectBody(messageSubject: String, body: String): ComposerRobot =
        subject(messageSubject)
            .body(body)

    fun draftSubjectBodyAttachment(to: String, messageSubject: String, body: String): ComposerRobot {
        return draftToSubjectBody(to, messageSubject, body)
            .attachments()
            .addImageCaptureAttachment(logoDrawable)
    }

    fun editBodyAndReply(newBody: String): MessageRobot =
        body(newBody).reply()

    fun clickUpButton(): ComposerRobot {
        view.instanceOf(AppCompatImageButton::class.java).withParent(view.withId(R.id.composer_toolbar)).click()
        return this
    }

    fun confirmDraftSaving(): InboxRobot {
        clickPositiveDialogButton()
        return InboxRobot()
    }

    fun confirmDraftSavingFromDrafts(): DraftsRobot {
        clickPositiveDialogButton()
        return DraftsRobot()
    }

    fun confirmDraftSavingFromSent(): SentRobot {
        clickPositiveDialogButton()
        return SentRobot()
    }

    private fun composeMessage(to: String, subject: String, body: String): ComposerRobot =
        recipients(to)
            .subject(subject)
            .body(body)

    fun recipients(email: String): ComposerRobot {
        view.withId(toRecipientsId).typeText(email).pressImeActionBtn()
        return this
    }

    fun changeSenderTo(email: String): ComposerRobot = clickFromField().selectSender(email)

    private fun clickPositiveDialogButton() {
        view.withId(android.R.id.button1).longClick()
    }

    private fun clickFromField(): ComposerRobot {
        view.withId(addressSpinnerId).click()
        return this
    }

    private fun selectSender(email: String): ComposerRobot {
        view.withId(android.R.id.text1).withText(email).click()
        return this
    }

    private fun ccRecipients(email: String): ComposerRobot {
        view.withId(R.id.composer_cc_recipient_view).typeText(email).pressImeActionBtn()
        return this
    }

    private fun bccRecipients(email: String): ComposerRobot {
        view.withId(R.id.composer_bcc_recipient_view).typeText(email).pressImeActionBtn()
        return this
    }

    private fun subject(text: String): ComposerRobot {
        view.withId(subjectId).click().clearText().typeText(text)
        return this
    }

    fun updateSubject(text: String): ComposerRobot {
        view.withId(subjectId).clearText().typeText("Updated: $text")
        return this
    }

    private fun body(text: String): ComposerRobot {
        view.withId(messageBodyId).clearText().typeText(text)
        return this
    }

    private fun showAdditionalRows(): ComposerRobot {
        view.withId(R.id.composer_expand_recipients_button).click()
        return this
    }

    private fun setMessagePassword(): SetPasswordRobot {
        view.withId(R.id.composer_password_button).click()
        return SetPasswordRobot()
    }

    private fun messageExpiration(): MessageExpirationRobot {
        view.withId(R.id.composer_expiration_button).click()
        return MessageExpirationRobot()
    }

    private fun hideExpirationView(): ComposerRobot {
        view.withId(R.id.hide_view).click()
        return this
    }

    fun attachments(): MessageAttachmentsRobot {
        view.withId(R.id.composer_attachments_button).click()
        return MessageAttachmentsRobot()
    }

    fun send(): InboxRobot {
        waitForConditionAndSend()
        return InboxRobot()
    }

    private fun sendAndLaunchApp(): InboxRobot {
        view.withId(sendMessageId).click()
        launchApp()
        return InboxRobot()
    }

    private fun launchApp(): MailboxActivity =
        ActivityTestRule(MailboxActivity::class.java).launchActivity(null)

    private fun sendWithNotSupportedExpiryConfirmation(): NotSupportedExpirationRobot {
        view.withId(sendMessageId).click()
        return NotSupportedExpirationRobot()
    }

    private fun sendToContact(): ContactsRobot {
        waitForConditionAndSend()
        return ContactsRobot()
    }

    private fun reply(): MessageRobot {
        waitForConditionAndSend()
        return MessageRobot()
    }

    private fun forward(): MessageRobot {
        waitForConditionAndSend()
        return MessageRobot()
    }

    private fun waitForConditionAndSend() {
        view.withId(sendMessageId).checkEnabled().click()
    }

    /**
     * Class represents Message Expiration dialog.
     */
    class NotSupportedExpirationRobot : Robot {

        fun sendAnyway(): InboxRobot {
            view.withId(ok).click()
//            view.withText(R.string.message_sent).inRoot(rootView.withNotCurrentActivityDecorView())
            return InboxRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [ComposerRobot].
     */
    class Verify : Robot {

        fun messageWithSubjectOpened(subject: String) {
            view.withId(subjectId).withText(subject).checkDisplayed()
        }

        fun bodyWithText(text: String) {
            view.withId(messageBodyId).withText(text).checkDisplayed()
        }

        fun fromEmailIs(email: String): DraftsRobot {
            view.withText(email).withParent(view.withId(addressSpinnerId)).checkDisplayed()
            return DraftsRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {

        const val attachmentsCountViewId = R.id.composer_attachments_count_text_view
        const val sendMessageId = R.id.send_button
        const val messageBodyId = R.id.composer_message_body_edit_text
        const val subjectId = R.id.composer_subject_edit_text
        const val logoDrawable = R.drawable.logo
        const val welcomeDrawable = R.drawable.welcome
        const val addressSpinnerId = R.id.composer_from_spinner
        const val toRecipientsId = R.id.composer_to_recipient_view
        const val ok = R.id.ok
    }
}
