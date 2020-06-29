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
package ch.protonmail.android.uitests.actions

import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import ch.protonmail.android.R
import ch.protonmail.android.uitests.results.ComposerResult
import ch.protonmail.android.uitests.testsHelper.MockAddAttachmentIntent
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.UIActions
import ch.protonmail.android.uitests.testsHelper.UICustomViewActionsAndMatchers.isChildOf
import ch.protonmail.android.uitests.testsHelper.UICustomViewActionsAndMatchers.setValueInNumberPicker
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf

/**
 * [ComposerRobot] class contains actions and verifications for email composer functionality.
 * Inner classes: [MessagePasswordRobot], [MessageExpirationRobot], [MessageAttachmentsRobot].
 */
open class ComposerRobot : UIActions() {

    fun sendMessageToInternalTrustedAddress(composerData: TestData): ComposerResult =
        composeMessageToInternalTrustedAddress(composerData).send()

    fun sendMessageToInternalNotTrustedAddress(composerData: TestData): ComposerResult =
        composeMessageToInternalNotTrustedAddress(composerData).send()

    fun sendMessageToExternalAddressPGPEncrypted(composerData: TestData): ComposerResult =
        composeMessageToExternalAddressPGPEncrypted(composerData).send()

    fun sendMessageToExternalAddressPGPSigned(composerData: TestData): ComposerResult =
        composeMessageToExternalAddressPGPSigned(composerData).send()

    fun sendMessageTOandCC(composerData: TestData): ComposerResult =
        recipients(composerData.internalEmailAddressTrustedKeys)
            .showAdditionalRows()
            .ccRecipients(composerData.externalEmailAddressPGPEncrypted)
            .subject(composerData.messageSubject)
            .body(composerData.messageBody)
            .send()

    fun sendMessageTOandCCandBCC(composerData: TestData): ComposerResult =
        recipients(composerData.internalEmailAddressTrustedKeys)
            .showAdditionalRows()
            .ccRecipients(composerData.externalEmailAddressPGPEncrypted)
            .bccRecipients(composerData.internalEmailAddressNotTrustedKeys)
            .subject(composerData.messageSubject)
            .body(composerData.messageBody)
            .send()

    fun sendMessageWithPassword(composerData: TestData): ComposerResult =
        composeMessageToExternalAddressPGPSigned(composerData)
            .setMessagePassword()
            .definePasswordWithHint()
            .send()

    fun sendMessageExpiryTimeInDays(composerData: TestData, days: Int): ComposerResult =
        composeMessageToInternalTrustedAddress(composerData)
            .messageExpiration()
            .setExpirationInDays(days)
            .verifyExpirationTimeShown(days)
            .send()

    fun sendMessageEOAndExpiryTime(composerData: TestData, days: Int): ComposerResult {
        composeMessageToExternalAddressPGPSigned(composerData)
            .setMessagePassword()
            .definePasswordWithHint()
            .messageExpiration()
            .setExpirationInDays(days)
            .verifyExpirationTimeShown(days)
            .send()
        return ComposerResult()
    }

    fun sendMessageEOAndExpiryTimeWithAttachment(composerData: TestData, days: Int): ComposerResult {
        composeMessageToExternalAddressPGPSigned(composerData)
            .setMessagePassword()
            .definePasswordWithHint()
            .messageExpiration()
            .setExpirationInDays(days)
            .verifyExpirationTimeShown(days)
            .hideExpirationView()
            .attachments()
            .addImageCaptureAttachment(R.drawable.logo)
            .send()

        return ComposerResult()
    }

    fun sendMessageCameraCaptureAttachment(composerData: TestData): ComposerResult {
        composeMessageToInternalTrustedAddress(composerData)
            .attachments()
            .addImageCaptureAttachment(R.drawable.logo)
            .send()
        return ComposerResult()
    }

    fun sendMessageChooseAttachment(composerData: TestData): ComposerResult {
        composeMessageToInternalNotTrustedAddress(composerData)
            .attachments()
            .addFileAttachment(R.drawable.logo)
            .send()
        return ComposerResult()
    }

    fun sendMessageToInternalContactWithTwoAttachments(composerData: TestData): ComposerResult {
        composeMessageToInternalTrustedAddress(composerData)
            .attachments()
            .addTwoImageCaptureAttachments(R.drawable.logo, R.drawable.welcome)
            .send()
        return ComposerResult()
    }

    fun sendMessageToExternalContactWithOneAttachment(composerData: TestData): ComposerResult {
        composeMessageToExternalAddressPGPEncrypted(composerData)
            .attachments()
            .addImageCaptureAttachment(R.drawable.logo)
            .send()
        return ComposerResult()
    }

    fun sendMessageToExternalContactWithTwoAttachments(composerData: TestData): ComposerResult {
        composeMessageToExternalAddressPGPSigned(composerData)
            .attachments()
            .addTwoImageCaptureAttachments(R.drawable.logo, R.drawable.welcome)
            .send()
        return ComposerResult()
    }

    private fun recipients(emails: String): ComposerRobot {
        typeTextIntoFieldWithIdAndPressImeAction(R.id.to_recipients, emails)
        return this
    }

    private fun ccRecipients(emails: String): ComposerRobot {
        typeTextIntoFieldWithIdAndPressImeAction(R.id.cc_recipients, emails)
        return this
    }

    private fun bccRecipients(emails: String): ComposerRobot {
        typeTextIntoFieldWithIdAndPressImeAction(R.id.bcc_recipients, emails)
        return this
    }

    private fun subject(text: String): ComposerRobot {
        typeTextIntoField(R.id.message_title, text)
        return this
    }

    private fun body(text: String): ComposerRobot {
        typeTextIntoField(R.id.message_body, text)
        return this
    }

    private fun showAdditionalRows(): ComposerRobot {
        clickOnObjectWithId(R.id.show_additional_rows)
        return this
    }

    private fun setMessagePassword(): MessagePasswordRobot {
        clickOnObjectWithId(R.id.set_message_password)
        return MessagePasswordRobot()
    }

    private fun messageExpiration(): MessageExpirationRobot {
        clickOnObjectWithId(R.id.set_message_expiration)
        return MessageExpirationRobot()
    }

    private fun hideExpirationView(): ComposerRobot {
        clickOnObjectWithId(R.id.hide_view)
        return this
    }

    private fun attachments(): MessageAttachmentsRobot {
        clickOnObjectWithId(R.id.add_attachments)
        return MessageAttachmentsRobot()
    }

    private fun send(): ComposerResult {
        clickOnObjectWithId(R.id.send_message)
        return ComposerResult()
    }

    private fun sendWithPGPConfirmation(): PGPConfirmationRobot {
        clickOnObjectWithId(R.id.send_message)
        return PGPConfirmationRobot()
    }

    private fun verifyExpirationTimeShown(days: Int): ComposerRobot {
        checkIfObjectWithIdAndTextSubstringIsDisplayed(R.id.expiration_time, days.toString())
        return this
    }

    private fun composeMessageToInternalTrustedAddress(composerData: TestData): ComposerRobot =
        recipients(composerData.internalEmailAddressTrustedKeys)
            .subject(composerData.messageSubject)
            .body(composerData.messageBody)

    private fun composeMessageToInternalNotTrustedAddress(composerData: TestData): ComposerRobot =
        recipients(composerData.internalEmailAddressNotTrustedKeys)
            .subject(composerData.messageSubject)
            .body(composerData.messageBody)

    private fun composeMessageToExternalAddressPGPEncrypted(composerData: TestData): ComposerRobot =
        recipients(composerData.externalEmailAddressPGPEncrypted)
            .subject(composerData.messageSubject)
            .body(composerData.messageBody)

    private fun composeMessageToExternalAddressPGPSigned(composerData: TestData): ComposerRobot =
        recipients(composerData.externalEmailAddressPGPSigned)
            .subject(composerData.messageSubject)
            .body(composerData.messageBody)

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
            insertTextIntoFieldWithId(R.id.define_password, password)
            return this
        }

        private fun confirmPassword(password: String): MessagePasswordRobot {
            insertTextIntoFieldWithId(R.id.confirm_password, password)
            return this
        }

        private fun defineHint(hint: String): MessagePasswordRobot {
            insertTextIntoFieldWithId(R.id.define_hint, hint)
            return this
        }

        private fun applyPassword(): ComposerRobot {
            clickOnObjectWithId(R.id.apply)
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
            onView(allOf(withClassName(`is`(NumberPicker::class.java.canonicalName)),
                isChildOf(withClassName(`is`(LinearLayout::class.java.canonicalName)), 0)))
                .check(matches(ViewMatchers.isDisplayed()))
                .perform(setValueInNumberPicker(days))
            return this
        }

        private fun confirmMessageExpiration(): ComposerRobot {
            clickOnObjectWithId(android.R.id.button1)
            return ComposerRobot()
        }
    }

    /**
     * Class represents Message Attachments.
     */
    inner class MessageAttachmentsRobot {

        fun addImageCaptureAttachment(@IdRes drawable: Int): ComposerRobot =
            mockCameraImageCapture(drawable)
                .navigateUpToComposerView()

        fun addTwoImageCaptureAttachments(
            @IdRes firstDrawable: Int,
            @IdRes secondDrawable: Int): ComposerRobot =
            mockCameraImageCapture(firstDrawable)
                .mockCameraImageCapture(secondDrawable)
                .navigateUpToComposerView()

        fun addFileAttachment(@IdRes drawable: Int): ComposerRobot =
            mockFileAttachment(drawable).navigateUpToComposerView()

        private fun navigateUpToComposerView(): ComposerRobot {
            clickObjectWithParentIdAndClass(R.id.toolbar, AppCompatImageButton::class.java)
            return ComposerRobot()
        }

        private fun mockCameraImageCapture(@IdRes drawableId: Int): MessageAttachmentsRobot {
            MockAddAttachmentIntent.mockCameraImageCapture(R.id.take_photo, drawableId)
            return this
        }

        private fun mockFileAttachment(@IdRes drawable: Int): MessageAttachmentsRobot {
            MockAddAttachmentIntent.mockChooseAttachment(R.id.attach_file, drawable)
            return this
        }
    }

    /**
     * Class represents PGP confirmation dialog.
     */
    inner class PGPConfirmationRobot {

        fun confirmSendingWithPGP(): ComposerResult {
            clickOnObjectWithId(R.id.ok)
            return ComposerResult()
        }
    }
}
