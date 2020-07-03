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

import androidx.test.espresso.action.ViewActions.click
import androidx.test.filters.LargeTest
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.twoPassUser
import ch.protonmail.android.uitests.testsHelper.UICustomViewActionsAndMatchers.waitUntilObjectWithIdAppears
import org.junit.Test

@LargeTest
class ComposerTests : BaseTest() {

    private val composerRobot = ComposerRobot()
    private val loginRobot = LoginRobot()

    @Test
    fun sendMessageToInternalTrustedContact() {
        loginRobot
            .loginUser(onePassUser())
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
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
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
        composerRobot
            .sendMessageToExternalContactWithTwoAttachments(TestData.composerData())
            .verify {
                sendingMessageToastShown()
                messageSentToastShown()
            }
    }
}
