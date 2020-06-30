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
import androidx.test.espresso.intent.Intents
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.ActivityTestRule
import ch.protonmail.android.R
import ch.protonmail.android.activities.guest.LoginActivity
import ch.protonmail.android.uitests.actions.ComposerRobot
import ch.protonmail.android.uitests.actions.LoginRobot
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.twoPassUser
import ch.protonmail.android.uitests.testsHelper.UICustomViewActionsAndMatchers.waitUntilObjectWithIdAppears
import org.junit.*
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4ClassRunner::class)
@LargeTest
class ComposerTests {

    private val composerRobot = ComposerRobot()
    private val loginRobot = LoginRobot()

    @get:Rule
    var activityTestRule = ActivityTestRule(LoginActivity::class.java)

    @Before
    fun setUp() {
        Intents.init()
        loginRobot
            .loginUser(onePassUser())
        waitUntilObjectWithIdAppears(R.id.compose).perform(click())
    }

    @After
    fun cleanUp() {
        Intents.release()
    }

    @Test
    fun sendMessageToInternalTrustedContact() {
        val result = composerRobot.sendMessageToInternalTrustedAddress(TestData.composerData())
        result.isMessageSent
    }

    @Test
    fun sendMessageToInternalNotTrustedContact() {
        val result = composerRobot.sendMessageToInternalNotTrustedAddress(TestData.composerData())
        result.isMessageSent
    }

    @Test
    fun sendMessageToPGPEncryptedContact() {
        val result = composerRobot.sendMessageToExternalAddressPGPEncrypted(TestData.composerData())
        result.isMessageSent
    }

    @Test
    fun sendMessageToPGPSignedContact() {
        val result = composerRobot.sendMessageToExternalAddressPGPSigned(TestData.composerData())
        result.isMessageSent
    }

    @Test
    fun sendMessageTOandCC() {
        val result = composerRobot.sendMessageTOandCC(TestData.composerData())
        result.isMessageSent
    }

    @Test
    fun sendMessageTOandCCandBCC() {
        val result = composerRobot.sendMessageTOandCCandBCC(TestData.composerData())
        result.isMessageSent
    }

    @Test
    fun sendMessageEO() {
        val result = composerRobot.sendMessageWithPassword(TestData.composerData())
        result.isMessageSent
    }

    @Test
    fun sendMessageExpiryTime() {
        val result = composerRobot.sendMessageExpiryTimeInDays(TestData.composerData(), 2)
        result.isMessageSent
    }

    @Test
    fun sendMessageEOAndExpiryTime() {
        val result = composerRobot.sendMessageEOAndExpiryTime(TestData.composerData(), 1)
        result.isMessageSent
    }

    @Test
    fun sendMessageEOAndExpiryTimeWithAttachment() {
        val result = composerRobot
            .sendMessageEOAndExpiryTimeWithAttachment(TestData.composerData(), 1)
        result.isMessageSent
    }

    @Test
    fun sendMessageToInternalTrustedContactCameraCaptureAttachment() {
        val result = composerRobot.sendMessageCameraCaptureAttachment(TestData.composerData())
        result.isMessageSent
    }

    @Test
    fun sendMessageToInternalNotTrustedContactChooseAttachment() {
        val result = composerRobot.sendMessageChooseAttachment(TestData.composerData())
        result.isMessageSent
    }

    @Test
    fun sendMessageToInternalContactWithTwoAttachments() {
        val result = composerRobot
            .sendMessageToInternalContactWithTwoAttachments(TestData.composerData())
        result.isMessageSent
    }

    @Test
    fun sendMessageToExternalContactWithOneAttachment() {
        val result = composerRobot
            .sendMessageToExternalContactWithOneAttachment(TestData.composerData())
        result.isMessageSent
    }

    @Test
    fun sendMessageToExternalContactWithTwoAttachments() {
        val result = composerRobot
            .sendMessageToExternalContactWithTwoAttachments(TestData.composerData())
        result.isMessageSent
    }
}
