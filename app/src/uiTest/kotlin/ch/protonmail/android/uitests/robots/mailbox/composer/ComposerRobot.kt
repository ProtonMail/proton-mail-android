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
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot.MessageExpirationRobot
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot.MessagePasswordRobot
import ch.protonmail.android.uitests.robots.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.UIActions
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.setValueInNumberPicker
import ch.protonmail.android.uitests.testsHelper.click
import ch.protonmail.android.uitests.testsHelper.type
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf

/**
 * [ComposerRobot] class contains actions and verifications for email composer functionality.
 * Inner classes: [MessagePasswordRobot], [MessageExpirationRobot], [MessageAttachmentsRobot].
 */
class ComposerRobot {

    fun sendMessage(to: String, subject: String, body: String): InboxRobot =
        recipients(to)
            .subject(subject)
            .body(body)
            .send()

    fun sendMessageToContact(subject: String, body: String): ContactsRobot =
        subject(subject)
            .body(body)
            .sendToContact()

    fun forwardMessage(to: String, body: String): MessageRobot =
        recipients(to)
            .body(body)
            .forward()

    fun changeSubjectAndForwardMessage(to: String, subject: String, body: String): MessageRobot =
        recipients(to)
            .subject(subject)
            .body(body)
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

    fun draftSubjectBody(messageSubject: String, body: String): ComposerRobot =
        subject(messageSubject)
            .body(body)

    fun draftSubjectBodyAttachment(to: String, messageSubject: String, body: String): ComposerRobot {
        return draftToSubjectBody(to, messageSubject, body)
            .attachments()
            .addImageCaptureAttachment(logoDrawable)
    }

    fun editBodyAndReply(messageBody: String): MessageRobot = body(messageBody).reply()

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

    private fun composeMessage(to: String, subject: String, body: String): ComposerRobot =
        recipients(to)
            .subject(subject)
            .body(body)

    fun recipients(email: String): ComposerRobot {
        UIActions.id.typeTextIntoFieldWithIdAndPressImeAction(R.id.to_recipients, email)
        return this
    }

    fun changeSenderTo(email: String): ComposerRobot = clickFromField().selectSender(email)

    private fun clickFromField(): ComposerRobot {
        UIActions.wait.forViewWithId(R.id.addresses_spinner).click()
        return this
    }

    private fun selectSender(email: String): ComposerRobot {
        UIActions.wait.forViewWithText(email).click()
        return this
    }

    private fun ccRecipients(email: String): ComposerRobot {
        UIActions.id.typeTextIntoFieldWithIdAndPressImeAction(R.id.cc_recipients, email)
        return this
    }

    private fun bccRecipients(email: String): ComposerRobot {
        UIActions.id.typeTextIntoFieldWithIdAndPressImeAction(R.id.bcc_recipients, email)
        return this
    }

    fun subject(text: String): ComposerRobot {
        UIActions.wait.forViewWithId(R.id.message_title).type(text)
        return this
    }

    private fun body(text: String): ComposerRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.message_body, text)
        return this
    }

    private fun showAdditionalRows(): ComposerRobot {
        UIActions.id.clickViewWithId(R.id.show_additional_rows)
        return this
    }

    private fun setMessagePassword(): MessagePasswordRobot {
        UIActions.id.clickViewWithId(R.id.set_message_password)
        return MessagePasswordRobot()
    }

    private fun messageExpiration(): MessageExpirationRobot {
        UIActions.id.clickViewWithId(R.id.set_message_expiration)
        return MessageExpirationRobot()
    }

    private fun hideExpirationView(): ComposerRobot {
        UIActions.id.clickViewWithId(R.id.hide_view)
        return this
    }

    private fun attachments(): MessageAttachmentsRobot {
        UIActions.id.clickViewWithId(R.id.add_attachments)
        return MessageAttachmentsRobot()
    }

    fun send(): InboxRobot {
        waitForConditionAndSend()
        return InboxRobot()
    }

    private fun sendWithNotSupportedExpiryConfirmation(): NotSupportedExpirationRobot {
        waitForConditionAndSend()
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
        UIActions.wait.forViewWithId(R.id.tokenPgpText)
        UIActions.id.clickViewWithId(sendMessageId)
        UIActions.wait.forViewWithText(R.string.message_sent)
        UIActions.wait.untilViewWithTextIsGone(R.string.message_sent)
    }

    /**
     * Class represents Message Password dialog.
     */
    class MessagePasswordRobot {

        fun definePasswordWithHint(password: String, hint: String): ComposerRobot {
            return definePassword(password)
                .confirmPassword(password)
                .defineHint(hint)
                .applyPassword()
        }

        private fun definePassword(password: String): MessagePasswordRobot {
            UIActions.id.insertTextIntoFieldWithId(R.id.define_password, password)
            return this
        }

        private fun confirmPassword(password: String): MessagePasswordRobot {
            UIActions.id.insertTextIntoFieldWithId(R.id.confirm_password, password)
            return this
        }

        private fun defineHint(hint: String): MessagePasswordRobot {
            UIActions.id.insertTextIntoFieldWithId(R.id.define_hint, hint)
            return this
        }

        private fun applyPassword(): ComposerRobot {
            UIActions.id.clickViewWithId(R.id.apply)
            return ComposerRobot()
        }
    }

    /**
     * Class represents Message Expiration dialog.
     */
    class MessageExpirationRobot {

        fun setExpirationInDays(days: Int): ComposerRobot =
            expirationDays(days)
                .confirmMessageExpiration()

        private fun expirationDays(days: Int): MessageExpirationRobot {
            onView(allOf(withClassName(`is`(NumberPicker::class.java.canonicalName)), withId(R.id.days_picker)))
                .perform(setValueInNumberPicker(days))
            return this
        }

        private fun confirmMessageExpiration(): ComposerRobot {
            UIActions.system.clickPositiveDialogButton()
            return ComposerRobot()
        }
    }

    /**
     * Class represents Message Expiration dialog.
     */
    class NotSupportedExpirationRobot {

        fun sendAnyway(): InboxRobot {
            UIActions.wait.forViewWithId(ok).click()
            return InboxRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [ComposerRobot].
     */
    class Verify {

        fun messageWithSubjectOpened(subject: String) {
            UIActions.wait.forViewWithIdAndText(R.id.message_title, subject)
        }

        fun toFieldContains(email: String) {
            UIActions.check.viewWithIdIsContainsText(R.id.to_recipients, email)
        }

        fun fromEmailIs(email: String): DraftsRobot {
            UIActions.wait.forViewWithTextAndParentId(email, addressSpinnerId)
            return DraftsRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {
        const val sendMessageId = R.id.send_message
        const val logoDrawable = R.drawable.logo
        const val welcomeDrawable = R.drawable.welcome
        const val addressSpinnerId = R.id.addresses_spinner
        const val ok = R.id.ok
    }
}
