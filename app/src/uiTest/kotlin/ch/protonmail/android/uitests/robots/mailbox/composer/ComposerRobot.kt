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
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot.MessageExpirationRobot
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot.MessagePasswordRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.UIActions
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.setValueInNumberPicker
import ch.protonmail.android.uitests.testsHelper.User
import ch.protonmail.android.uitests.testsHelper.insert
import ch.protonmail.android.uitests.testsHelper.type
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf

/**
 * [ComposerRobot] class contains actions and verifications for email composer functionality.
 * Inner classes: [MessagePasswordRobot], [MessageExpirationRobot], [MessageAttachmentsRobot].
 */
class ComposerRobot {

    fun sendMessageToInternalTrustedAddress(): InboxRobot =
        composeMessageToInternalTrustedAddress().send()

    fun sendMessageToInternalNotTrustedAddress(): InboxRobot =
        composeMessageToInternalNotTrustedAddress().send()

    fun sendMessageToExternalAddressPGPEncrypted(): InboxRobot =
        composeMessageToExternalAddressPGPEncrypted().send()

    fun sendMessageToExternalAddressPGPSigned(): InboxRobot =
        composeMessageToExternalAddressPGPSigned().send()

    fun sendMessageTOandCC(recipient: User, ccRecipient: User): InboxRobot =
        recipients(recipient)
            .showAdditionalRows()
            .ccRecipients(ccRecipient)
            .subject(TestData.messageSubject)
            .body(TestData.messageBody)
            .send()

    fun sendMessageTOandCCandBCC(recipient: User, ccRecipient: User, bccRecipient: User): InboxRobot =
        recipients(recipient)
            .showAdditionalRows()
            .ccRecipients(ccRecipient)
            .bccRecipients(bccRecipient)
            .subject(TestData.messageSubject)
            .body(TestData.messageBody)
            .send()

    fun sendMessageWithPassword(): InboxRobot =
        composeMessageToExternalAddressPGPSigned()
            .setMessagePassword()
            .definePasswordWithHint()
            .send()

    fun sendMessageExpiryTimeInDays(days: Int): InboxRobot =
        composeMessageToInternalTrustedAddress()
            .messageExpiration()
            .setExpirationInDays(days)
            .verifyExpirationTimeShown(days)
            .send()

    fun sendMessageEOAndExpiryTime(days: Int): InboxRobot {
        composeMessageToExternalAddressPGPSigned()
            .setMessagePassword()
            .definePasswordWithHint()
            .messageExpiration()
            .setExpirationInDays(days)
            .verifyExpirationTimeShown(days)
            .send()
        return InboxRobot()
    }

    fun sendMessageEOAndExpiryTimeAndPGPConfirmation(days: Int): InboxRobot {
        composeMessageToExternalAddressPGPSigned()
            .setMessagePassword()
            .definePasswordWithHint()
            .messageExpiration()
            .setExpirationInDays(days)
            .verifyExpirationTimeShown(days)
            .sendWithPGPConfirmation()
            .confirmSendingWithPGP()
        return InboxRobot()
    }

    fun sendMessageEOAndExpiryTimeWithAttachment(days: Int): ComposerRobot {
        composeMessageToExternalAddressPGPSigned()
            .setMessagePassword()
            .definePasswordWithHint()
            .messageExpiration()
            .setExpirationInDays(days)
            .verifyExpirationTimeShown(days)
            .hideExpirationView()
            .attachments()
            .addImageCaptureAttachment(R.drawable.logo)
            .send()
        return ComposerRobot()
    }

    fun sendMessageEOAndExpiryTimeWithAttachmentAndPGPConfirmation(days: Int): InboxRobot {
        composeMessageToExternalAddressPGPSigned()
            .setMessagePassword()
            .definePasswordWithHint()
            .messageExpiration()
            .setExpirationInDays(days)
            .verifyExpirationTimeShown(days)
            .hideExpirationView()
            .attachments()
            .addImageCaptureAttachment(R.drawable.logo)
            .sendWithPGPConfirmation()
            .confirmSendingWithPGP()
        return InboxRobot()
    }

    fun sendMessageCameraCaptureAttachment(): InboxRobot {
        composeMessageToInternalTrustedAddress()
            .attachments()
            .addImageCaptureAttachment(R.drawable.logo)
            .send()
        return InboxRobot()
    }

    fun sendMessageChooseAttachment(): InboxRobot {
        composeMessageToInternalNotTrustedAddress()
            .attachments()
            .addFileAttachment(R.drawable.logo)
            .send()
        return InboxRobot()
    }

    fun sendMessageToInternalContactWithTwoAttachments(): InboxRobot {
        composeMessageToInternalTrustedAddress()
            .attachments()
            .addTwoImageCaptureAttachments(R.drawable.logo, R.drawable.welcome)
            .send()
        return InboxRobot()
    }

    fun sendMessageToExternalContactWithOneAttachment(): InboxRobot {
        composeMessageToExternalAddressPGPEncrypted()
            .attachments()
            .addImageCaptureAttachment(R.drawable.logo)
            .send()
        return InboxRobot()
    }

    fun sendMessageToExternalContactWithTwoAttachments(): InboxRobot {
        composeMessageToExternalAddressPGPSigned()
            .attachments()
            .addTwoImageCaptureAttachments(R.drawable.logo, R.drawable.welcome)
            .send()
        return InboxRobot()
    }

    fun draftSubjectBody(messageSubject: String): ComposerRobot {
        subject(messageSubject)
            .body(TestData.messageBody)
        return this
    }

    fun draftSubjectBodyAttachment(messageSubject: String): ComposerRobot {
        subject(messageSubject)
            .body(TestData.messageBody)
            .attachments()
            .addImageCaptureAttachment(R.drawable.logo)
        return this
    }

    fun navigateUpToInbox(): ComposerRobot {
        UIActions.system.clickHamburgerOrUpButton()
        return this
    }

    fun confirmDraftSaving(): InboxRobot {
        UIActions.system.clickPositiveDialogButton()
        return InboxRobot()
    }

    fun editSubject(messageSubject: String): ComposerRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.message_title, "Draft Edit: ")
        UIActions.id.clickViewWithId(R.id.message_title)
        UIActions.id.typeTextIntoFieldWithId(R.id.message_title, messageSubject)
        return this
    }

    fun editBody(messageBody: String): ComposerRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.message_body)
            .insert(messageBody)
            .type(messageBody)
        return this
    }

    fun editBodyAndSend(messageBody: String): ComposerRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.message_body)
            .insert(messageBody)
            .type(messageBody)
        send()
        return this
    }

    private fun recipients(user: User): ComposerRobot {
        UIActions.id.typeTextIntoFieldWithIdAndPressImeAction(R.id.to_recipients, user.email)
        return this
    }

    private fun ccRecipients(user: User): ComposerRobot {
        UIActions.id.typeTextIntoFieldWithIdAndPressImeAction(R.id.cc_recipients, user.email)
        return this
    }

    private fun bccRecipients(user: User): ComposerRobot {
        UIActions.id.typeTextIntoFieldWithIdAndPressImeAction(R.id.bcc_recipients, user.email)
        return this
    }

    private fun subject(text: String): ComposerRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.message_title, text)
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

    private fun send(): InboxRobot {
        UIActions.id.clickViewWithId(R.id.send_message)
        return InboxRobot()
    }

    private fun sendWithPGPConfirmation(): PGPConfirmationRobot {
        UIActions.id.clickViewWithId(R.id.send_message)
        return PGPConfirmationRobot()
    }

    private fun verifyExpirationTimeShown(days: Int): ComposerRobot {
        UIActions.check.viewWithIdAndTextSubstringIsDisplayed(R.id.expiration_time, days.toString())
        return this
    }

    private fun composeMessageToInternalTrustedAddress(): ComposerRobot =
        recipients(TestData.internalEmailTrustedKeys)
            .subject(TestData.messageSubject)
            .body(TestData.messageBody)

    private fun composeMessageToInternalNotTrustedAddress(): ComposerRobot =
        recipients(TestData.internalEmailNotTrustedKeys)
            .subject(TestData.messageSubject)
            .body(TestData.messageBody)

    private fun composeMessageToExternalAddressPGPEncrypted(): ComposerRobot =
        recipients(TestData.externalEmailPGPEncrypted)
            .subject(TestData.messageSubject)
            .body(TestData.messageBody)

    private fun composeMessageToExternalAddressPGPSigned(): ComposerRobot =
        recipients(TestData.externalEmailPGPSigned)
            .subject(TestData.messageSubject)
            .body(TestData.messageBody)

    /**
     * Class represents Message Password dialog.
     */
    inner class MessagePasswordRobot {

        fun definePasswordWithHint(): ComposerRobot {
            return definePassword("123")
                .confirmPassword("123")
                .defineHint("ProtonMail")
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
    inner class MessageExpirationRobot {

        fun setExpirationInDays(days: Int): ComposerRobot =
            expirationDays(days).confirmMessageExpiration()

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
     * Class represents PGP confirmation dialog.
     */
    inner class PGPConfirmationRobot {

        fun confirmSendingWithPGP(): ComposerRobot {
            UIActions.id.clickViewWithId(R.id.ok)
            return ComposerRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [ComposerRobot].
     */
    class Verify {

        fun defaultEmailAddressViewShown(): ComposerRobot {
            return ComposerRobot()
        }

        fun sendingMessageToastShown(): ComposerRobot {
            UIActions.check.toastMessageIsDisplayed(R.string.sending_message)
            return ComposerRobot()
        }

        fun messageSentToastShown(): ComposerRobot {
            UIActions.check.toastMessageIsDisplayed(R.string.message_sent)
            return ComposerRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
