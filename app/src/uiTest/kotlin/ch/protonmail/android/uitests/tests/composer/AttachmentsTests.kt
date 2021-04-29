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
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.settings.autolock.PinRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.autoAttachPublicKeyUser
import ch.protonmail.android.uitests.testsHelper.TestData.docxFile
import ch.protonmail.android.uitests.testsHelper.TestData.editedPassword
import ch.protonmail.android.uitests.testsHelper.TestData.editedPasswordHint
import ch.protonmail.android.uitests.testsHelper.TestData.externalOutlookPGPSigned
import ch.protonmail.android.uitests.testsHelper.TestData.internalEmailNotTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestData.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestData.jpegFile
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestData.pdfFile
import ch.protonmail.android.uitests.testsHelper.TestData.pngFile
import ch.protonmail.android.uitests.testsHelper.TestData.twoPassUser
import ch.protonmail.android.uitests.testsHelper.TestData.zipFile
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import me.proton.core.test.android.instrumented.intentutils.MimeTypes
import org.hamcrest.CoreMatchers.not
import org.junit.experimental.categories.Category
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

class AttachmentsTests : BaseTest() {

    private val loginRobot = LoginRobot()
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
            .loginUser(onePassUser)
            .searchBar()
            .searchMessageText(messageSubject)
            .clickSearchedMessageBySubject(messageSubject)
            .clickLoadEmbeddedImagesButton()
            .verify { loadEmbeddedImagesButtonIsGone() }
    }

    @Ignore("Enable when attachments UI on v4 is finalized - MAILAND-1545")
    @TestId("1336")
    @Test
    fun sharePngFile() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(MimeTypes.image.png, pngFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(pngFile)
            .clickAttachment(pngFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(MimeTypes.image.png) }
    }

    @Ignore("Enable when attachments UI on v4 is finalized - MAILAND-1545")
    @TestId("1354")
    @Test
    fun shareJpegFile() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(MimeTypes.image.jpeg, jpegFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(jpegFile)
            .clickAttachment(jpegFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(MimeTypes.image.jpeg) }
    }

    @Ignore("Enable when attachments UI on v4 is finalized - MAILAND-1545")
    @TestId("1338")
    @Test
    fun shareDocxFile() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(MimeTypes.application.docx, docxFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(docxFile)
            .clickAttachment(docxFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(MimeTypes.application.docx) }
    }

    @Ignore("Enable when attachments UI on v4 is finalized - MAILAND-1545")
    @TestId("1334")
    @Test
    fun shareZipFile() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(MimeTypes.application.zip, zipFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(zipFile)
            .clickAttachment(zipFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(MimeTypes.application.zip) }
    }

    @Ignore("Enable when attachments UI on v4 is finalized - MAILAND-1545")
    @TestId("53709")
    @Test
    fun sharePdfFile() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(MimeTypes.application.pdf, pdfFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(pdfFile)
            .clickAttachment(pdfFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(MimeTypes.application.pdf) }
    }

    @Ignore("Enable when attachments UI on v4 is finalized - MAILAND-1545")
    @TestId("1356")
    @Test
    fun sharePngFileWithPinUnlocked() {
        val to = internalEmailTrustedKeys.email
        val pin = "1234"
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .settings()
            .selectAutoLock()
            .enableAutoLock()
            .setPin(pin)
            .changeAutoLockTimer()
            .selectFiveMinutesAutoLockTimeout()

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(MimeTypes.image.png, pngFile)
            .clickShareDialogJustOnceButton()

        composeRobot
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(pngFile)
            .clickAttachment(pngFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(MimeTypes.image.png) }
    }

    @Ignore("Enable when attachments UI on v4 is finalized - MAILAND-1545")
    @TestId("1355")
    @Test
    fun sharePngFileWithPinLocked() {
        val to = internalEmailTrustedKeys.email
        val pin = "1234"
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .settings()
            .selectAutoLock()
            .enableAutoLock()
            .setPin(pin)
            .changeAutoLockTimer()
            .selectImmediateAutoLockTimeout()

        deviceRobot
            .clickHomeButton()
            .sendShareIntent(MimeTypes.image.png, pngFile)
            .clickShareDialogJustOnceButton()

        pinRobot
            .providePin(pin)
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .clickAttachment(pngFile)
            .clickAttachment(pngFile)
            .verify { intentWithActionFileNameAndMimeTypeSent(MimeTypes.image.png) }
    }

    @TestId("29717")
    @Test
    fun sendMessageToInternalTrustedContactWithCameraCaptureAttachment() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
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
            .loginUser(onePassUser)
            .compose()
            .sendMessageWithFileAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("29719")
    @Test
    fun sendMessageToInternalContactWithTwoAttachments() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .sendMessageTwoImageCaptureAttachments(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1485")
    @Test
    fun sendMessageWithAttachmentFromPmMe() {
        val onePassUserPmMeAddress = onePassUser.pmMe
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .changeSenderTo(onePassUserPmMeAddress)
            .sendMessageWithFileAttachment(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("21091")
    @Test
    fun sendExternalMessageWithPasswordExpiryTimeAndAttachment() {
        val to = externalOutlookPGPSigned.email
        val password = editedPassword
        val hint = editedPasswordHint
        loginRobot
            .loginTwoPasswordUser(twoPassUser)
            .decryptMailbox(password)
            .compose()
            .sendMessageEOAndExpiryTimeWithAttachment(to, subject, body, 1, password, hint)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @Ignore("Enable when attachments UI on v4 is finalized - MAILAND-1545")
    @TestId("15539")
    @Category(SmokeTest::class)
    @Test
    fun sendExternalMessageWithOneAttachment() {
        val to = externalOutlookPGPSigned.email
        loginRobot
            .loginUser(onePassUser)
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
            .loginUser(onePassUser)
            .compose()
            .sendMessageTwoImageCaptureAttachments(to, subject, body)
            .menuDrawer()
            .sent()
            .verify { messageWithSubjectExists(subject) }
    }

    @Ignore("Enable when attachments UI on v4 is finalized - MAILAND-1545")
    @TestId("1558")
    @Test
    fun automaticallyAttachPublicKey() {
        val to = internalEmailNotTrustedKeys.email
        val publicKey = "publickey - EmailAddress(s=${autoAttachPublicKeyUser.email}) - 0xA9FF792E.asc"
        loginRobot
            .loginUser(autoAttachPublicKeyUser)
            .compose()
            .sendMessage(to, subject, body)
            .menuDrawer()
            .sent()
            .clickMessageBySubject(subject)
            .expandAttachments()
            .verify { publicKeyIsAttached(publicKey) }
    }
}
