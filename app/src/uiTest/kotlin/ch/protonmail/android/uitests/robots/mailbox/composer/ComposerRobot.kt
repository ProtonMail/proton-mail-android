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
package ch.protonmail.android.uitests.robots.mailbox.composer

import android.widget.NumberPicker
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.contacts.ContactsRobot
import ch.protonmail.android.uitests.robots.device.DeviceRobot
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot.MessageExpirationRobot
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot.MessagePasswordRobot
import ch.protonmail.android.uitests.robots.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitests.robots.mailbox.sent.SentRobot
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.setValueInNumberPicker
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import me.proton.core.test.android.instrumented.CoreRobot
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf

/**
 * [ComposerRobot] class contains actions and verifications for email composer functionality.
 * Inner classes: [MessagePasswordRobot], [MessageExpirationRobot], [MessageAttachmentsRobot].
 */
class ComposerRobot : CoreRobot, DeviceRobot() {

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
            .send()

    fun sendMessageTwoImageCaptureAttachments(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addTwoImageCaptureAttachments(logoDrawable, welcomeDrawable)
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
        UIActions.system.clickHamburgerOrUpButton()
        return this
    }

    fun confirmDraftSaving(): InboxRobot {
        UIActions.system.clickPositiveDialogButton()
        return InboxRobot()
    }

    fun confirmDraftSavingFromDrafts(): DraftsRobot {
        UIActions.system.clickPositiveDialogButton()
        return DraftsRobot()
    }

    fun confirmDraftSavingFromSent(): SentRobot {
        UIActions.system.clickPositiveDialogButton()
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

    private fun clickFromField(): ComposerRobot {
        UIActions.wait.untilViewWithIdEnabled(sendMessageId)
        view.withId(R.id.addresses_spinner).click()
        return this
    }

    private fun selectSender(email: String): ComposerRobot {
        view.withText(email).click()
        return this
    }

    private fun ccRecipients(email: String): ComposerRobot {
        view.withId(R.id.cc_recipients).typeText(email).pressImeActionBtn()
        return this
    }

    private fun bccRecipients(email: String): ComposerRobot {
        view.withId(R.id.bcc_recipients).typeText(email).pressImeActionBtn()
        return this
    }

    fun subject(text: String): ComposerRobot {
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
        view.withId(R.id.show_additional_rows).click()
        return this
    }

    private fun setMessagePassword(): MessagePasswordRobot {
        view.withId(R.id.set_message_password).click()
        return MessagePasswordRobot()
    }

    private fun messageExpiration(): MessageExpirationRobot {
        view.withId(R.id.set_message_expiration).click()
        return MessageExpirationRobot()
    }

    private fun hideExpirationView(): ComposerRobot {
        view.withId(R.id.hide_view).click()
        return this
    }

    fun attachments(): MessageAttachmentsRobot {
        UIActions.wait.untilViewWithIdEnabled(sendMessageId)
        view.withId(R.id.add_attachments).click()
        return MessageAttachmentsRobot()
    }

    fun send(): InboxRobot {
        waitForConditionAndSend()
        return InboxRobot()
    }

    private fun sendWithNotSupportedExpiryConfirmation(): NotSupportedExpirationRobot {
        view.withId(R.id.text1).waitUntilGone()
        UIActions.wait.untilViewWithIdEnabled(sendMessageId)
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
        view.withId(R.id.text1).waitUntilGone()
        UIActions.wait.untilViewWithIdEnabled(sendMessageId)
        view.withId(sendMessageId).click()
        UIActions.wait.forToastWithText(R.string.message_sent)
    }

    /**
     * Class represents Message Password dialog.
     */
    class MessagePasswordRobot : CoreRobot {

        fun definePasswordWithHint(password: String, hint: String): ComposerRobot {
            return definePassword(password)
                .confirmPassword(password)
                .defineHint(hint)
                .applyPassword()
        }

        private fun definePassword(password: String): MessagePasswordRobot {
            view.withId(R.id.define_password).replaceText(password)
            return this
        }

        private fun confirmPassword(password: String): MessagePasswordRobot {
            view.withId(R.id.confirm_password).replaceText(password)
            return this
        }

        private fun defineHint(hint: String): MessagePasswordRobot {
            view.withId(R.id.define_hint).replaceText(hint)
            return this
        }

        private fun applyPassword(): ComposerRobot {
            view.withId(R.id.apply).click()
            return ComposerRobot()
        }
    }

    /**
     * Class represents Message Expiration dialog.
     */
    class MessageExpirationRobot {

        fun setExpirationInDays(days: Int): ComposerRobot =
            expirationDays(days)
                .confirmsetMessageExpiration()

        private fun expirationDays(days: Int): MessageExpirationRobot {
            onView(allOf(withClassName(`is`(NumberPicker::class.java.canonicalName)), withId(R.id.days_picker)))
                .perform(setValueInNumberPicker(days))
            return this
        }

        private fun confirmsetMessageExpiration(): ComposerRobot {
            UIActions.system.clickPositiveDialogButton()
            return ComposerRobot()
        }
    }

    /**
     * Class represents Message Expiration dialog.
     */
    class NotSupportedExpirationRobot : CoreRobot {

        fun sendAnyway(): InboxRobot {
            view.withId(ok).click()
            UIActions.wait.forToastWithText(R.string.message_sent)
            return InboxRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [ComposerRobot].
     */
    class Verify : CoreRobot {

        fun messageWithSubjectOpened(subject: String) {
            view.withId(subjectId).withText(subject).wait().checkDisplayed()
        }

        fun bodyWithText(text: String) {
            view.withId(messageBodyId).withText(text).wait().checkDisplayed()
        }

        fun fromEmailIs(email: String): DraftsRobot {
            view.withText(email).withParent(view.withId(addressSpinnerId)).wait().checkDisplayed()
            return DraftsRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {
        const val sendMessageId = R.id.send_message
        const val messageBodyId = R.id.message_body
        const val subjectId = R.id.message_title
        const val logoDrawable = R.drawable.logo
        const val welcomeDrawable = R.drawable.welcome
        const val addressSpinnerId = R.id.addresses_spinner
        const val toRecipientsId = R.id.to_recipients
        const val ok = R.id.ok
    }
}
