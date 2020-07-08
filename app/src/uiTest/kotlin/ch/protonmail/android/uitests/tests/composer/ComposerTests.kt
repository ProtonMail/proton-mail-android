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
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.twoPassUser
import org.junit.Test

@LargeTest
class ComposerTests : BaseTest() {

    private val loginRobot = LoginRobot()

    @Test
    fun sendMessageToInternalTrustedContact() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageToInternalTrustedAddress(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToInternalNotTrustedContact() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageToInternalNotTrustedAddress(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToPGPEncryptedContact() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageToExternalAddressPGPEncrypted(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToPGPSignedContact() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageToExternalAddressPGPSigned(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageTOandCC() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageTOandCC(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageTOandCCandBCC() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageTOandCCandBCC(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageEO() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageWithPassword(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageExpiryTime() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageExpiryTimeInDays(TestData.composerData(), 2)
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageEOAndExpiryTime() {
        loginRobot
            .loginTwoPasswordUser(twoPassUser())
            .compose()
            .sendMessageEOAndExpiryTimeAndPGPConfirmation(TestData.composerData(), 1)
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageEOAndExpiryTimeWithAttachment() {
        loginRobot
            .loginTwoPasswordUser(twoPassUser())
            .compose()
            .sendMessageEOAndExpiryTimeWithAttachmentAndPGPConfirmation(TestData.composerData(), 1)
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToInternalTrustedContactCameraCaptureAttachment() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageCameraCaptureAttachment(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToInternalNotTrustedContactChooseAttachment() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageChooseAttachment(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToInternalContactWithTwoAttachments() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageToInternalContactWithTwoAttachments(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToExternalContactWithOneAttachment() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageToExternalContactWithOneAttachment(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }

    @Test
    fun sendMessageToExternalContactWithTwoAttachments() {
        loginRobot
            .loginUser(onePassUser())
            .compose()
            .sendMessageToExternalContactWithTwoAttachments(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }
}
