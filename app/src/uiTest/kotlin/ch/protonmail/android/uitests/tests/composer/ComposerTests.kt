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

import androidx.test.filters.LargeTest
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.externalEmailPGPEncrypted
import ch.protonmail.android.uitests.testsHelper.TestData.internalEmailNotTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestData.internalEmailTrustedKeys
import org.junit.Test

@LargeTest
class ComposerTests : BaseTest() {

    private val loginRobot = LoginRobot()

    @Test
    fun sendMessageToInternalTrustedContact() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageToInternalTrustedAddress()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToInternalNotTrustedContact() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageToInternalNotTrustedAddress()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToPGPEncryptedContact() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageToExternalAddressPGPEncrypted()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToPGPSignedContact() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageToExternalAddressPGPSigned()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageTOandCC() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageTOandCC(internalEmailTrustedKeys, externalEmailPGPEncrypted)
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageTOandCCandBCC() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageTOandCCandBCC(internalEmailTrustedKeys, externalEmailPGPEncrypted, internalEmailNotTrustedKeys)
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageEO() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageWithPassword()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageExpiryTime() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageExpiryTimeInDays(2)
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageEOAndExpiryTime() {
        loginRobot
            .loginTwoPasswordUser(TestData.twoPassUser)
            .compose()
            .sendMessageEOAndExpiryTimeAndPGPConfirmation(1)
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageEOAndExpiryTimeWithAttachment() {
        loginRobot
            .loginTwoPasswordUser(TestData.twoPassUser)
            .compose()
            .sendMessageEOAndExpiryTimeWithAttachmentAndPGPConfirmation(1)
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToInternalTrustedContactCameraCaptureAttachment() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageCameraCaptureAttachment()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToInternalNotTrustedContactChooseAttachment() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageChooseAttachment()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToInternalContactWithTwoAttachments() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageToInternalContactWithTwoAttachments()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToExternalContactWithOneAttachment() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageToExternalContactWithOneAttachment()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToExternalContactWithTwoAttachments() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .compose()
            .sendMessageToExternalContactWithTwoAttachments()
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }
}
