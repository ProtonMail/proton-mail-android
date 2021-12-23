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
package ch.protonmail.android.uitests.tests.composer

import android.app.Activity
import android.app.Instrumentation
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import ch.protonmail.android.uitests.robots.device.DeviceRobot
import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.settings.autolock.PinRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.docxFile
import ch.protonmail.android.uitests.testsHelper.TestData.editedPassword
import ch.protonmail.android.uitests.testsHelper.TestData.editedPasswordHint
import ch.protonmail.android.uitests.testsHelper.TestData.jpegFile
import ch.protonmail.android.uitests.testsHelper.TestData.pdfFile
import ch.protonmail.android.uitests.testsHelper.TestData.pngFile
import ch.protonmail.android.uitests.testsHelper.TestData.zipFile
import ch.protonmail.android.uitests.testsHelper.TestUser.autoAttachPublicKeyUser
import ch.protonmail.android.uitests.testsHelper.TestUser.externalOutlookPGPSigned
import ch.protonmail.android.uitests.testsHelper.TestUser.internalEmailNotTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestUser.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import me.proton.core.test.android.instrumented.utils.FileUtils
import org.hamcrest.CoreMatchers.not
import org.junit.experimental.categories.Category
import kotlin.test.BeforeTest
import kotlin.test.Test

class AttachmentsTests : BaseTest() {

    private val loginRobot = LoginMailRobot()
    private val composeRobot = ComposerRobot()
    private val deviceRobot = DeviceRobot()
    private val pinRobot = PinRobot()
    private lateinit var subject: String
    private lateinit var body: String

    @BeforeTest
    override fun setUp() {
        super.setUp()
        subject = TestData.messageSubject
        body = TestData.messageBody
        intending(not(isInternal()))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @TestId("53707")
    @Test
    fun loadInlinePngImage() {
        val messageSubject = "One inline image and one attachment"
        loginRobot
            .loginOnePassUser()
            .searchBar()
            .searchMessageText(messageSubject)
            .clickSearchedMessageBySubject(messageSubject)
            .clickLoadEmbeddedImagesButton()
            .verify { loadEmbeddedImagesButtonIsGone() }
    }

    @TestId("1336")
    @Test
    fun sharePngFile() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .mailboxLayoutShown()

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(FileUtils.MimeTypes.image.png, pngFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendAndLaunchApp(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(pngFile)
            .clickAttachment(pngFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(FileUtils.MimeTypes.image.png) }
    }

    @TestId("1354")
    @Test
    fun shareJpegFile() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .mailboxLayoutShown()

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(FileUtils.MimeTypes.image.jpeg, jpegFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendAndLaunchApp(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(jpegFile)
            .clickAttachment(jpegFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(FileUtils.MimeTypes.image.jpeg) }
    }

    @TestId("1338")
    @Test
    fun shareDocxFile() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .mailboxLayoutShown()

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(FileUtils.MimeTypes.application.docx, docxFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendAndLaunchApp(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(docxFile)
            .clickAttachment(docxFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(FileUtils.MimeTypes.application.docx) }
    }

    @TestId("1334")
    @Test
    fun shareZipFile() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .mailboxLayoutShown()

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(FileUtils.MimeTypes.application.zip, zipFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendAndLaunchApp(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(zipFile)
            .clickAttachment(zipFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(FileUtils.MimeTypes.application.zip) }
    }

    @TestId("53709")
    @Test
    fun sharePdfFile() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .mailboxLayoutShown()

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(FileUtils.MimeTypes.application.pdf, pdfFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendAndLaunchApp(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(pdfFile)
            .clickAttachment(pdfFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(FileUtils.MimeTypes.application.pdf) }
    }

    @TestId("1356")
    @Test
    fun sharePngFileWithPinUnlocked() {
        val to = internalEmailTrustedKeys.email
        val pin = "1234"
        loginRobot
            .loginOnePassUser()
            .menuDrawer()
            .settings()
            .selectAutoLock()
            .enableAutoLock()
            .setPin(pin)
            .changeAutoLockTimer()
            .selectFiveMinutesAutoLockTimeout()

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(FileUtils.MimeTypes.image.png, pngFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendAndLaunchApp(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(pngFile)
            .clickAttachment(pngFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(FileUtils.MimeTypes.image.png) }
    }

    @TestId("1355")
    @Test
    fun sharePngFileWithPinLocked() {
        val to = internalEmailTrustedKeys.email
        val pin = "1234"
        loginRobot
            .loginOnePassUser()
            .menuDrawer()
            .settings()
            .selectAutoLock()
            .enableAutoLock()
            .setPin(pin)
            .changeAutoLockTimer()
            .selectImmediateAutoLockTimeout()

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(FileUtils.MimeTypes.image.png, pngFile)
            .clickShareDialogJustOnceButton()

        pinRobot
            .providePinToComposer(pin)
            .sendAndLaunchApp(to, subject, body)

        pinRobot
            .providePinToInbox(pin)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(pngFile)
            .clickAttachment(pngFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(FileUtils.MimeTypes.image.png) }
    }

    @TestId("29717")
    @Test
    fun sendMessageToInternalTrustedContactWithCameraCaptureAttachment() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .sendMessageCameraCaptureAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("29718")
    @Test
    fun sendMessageToInternalNotTrustedContactWithAttachment() {
        val to = internalEmailNotTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .sendMessageWithFileAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }

    @TestId("29719")
    @Test
    fun sendMessageToInternalContactWithTwoAttachments() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .sendMessageTwoImageCaptureAttachments(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }

    @TestId("1485")
    @Test
    fun sendMessageWithAttachmentFromPmMe() {
        val onePassUserPmMeAddress = onePassUser.pmMe
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .changeSenderTo(onePassUserPmMeAddress)
            .sendMessageWithFileAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }

    @TestId("21091")
    @Test
    fun sendExternalMessageWithPasswordExpiryTimeAndAttachment() {
        val to = externalOutlookPGPSigned.email
        val password = editedPassword
        val hint = editedPasswordHint
        loginRobot
            .loginTwoPassUser()
            .compose()
            .sendMessageEOAndExpiryTimeWithAttachment(to, subject, body, 1, password, hint)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }

    @TestId("15539")
    @Test
    fun sendExternalMessageWithOneAttachment() {
        val to = externalOutlookPGPSigned.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .sendMessageCameraCaptureAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .verify { messageContainsOneAttachment() }
    }

    @TestId("15540")
    @Test
    fun sendExternalMessageWithTwoAttachments() {
        val to = externalOutlookPGPSigned.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .sendMessageTwoImageCaptureAttachments(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .verify { messageContainsTwoAttachments() }
    }

    @TestId("1558")
    @Test
    fun automaticallyAttachPublicKey() {
        val to = internalEmailNotTrustedKeys.email
        val publicKey = "publickey - EmailAddress(s=${autoAttachPublicKeyUser.email}) - 0xA9FF792E.asc"
        loginRobot
            .loginAutoAttachPublicKeyUser()
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .verify { publicKeyIsAttached(publicKey) }
    }

    @TestId("67578")
    @Test
    fun removeAttachmentAndSendMessage() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginOnePassUser()
            .compose()
            .addAndRemoveAttachmentAndSend(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }
}
