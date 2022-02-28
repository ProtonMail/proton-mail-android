/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.uitests.tests.settings

import android.app.Activity
import android.app.Instrumentation
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.robots.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestUser.twoPassUser
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.hamcrest.CoreMatchers
import kotlin.test.BeforeTest
import kotlin.test.Test

class PrivacyAccountSettingsTests : BaseTest() {

    private val loginRobot = LoginMailRobot()
    private val accountSettingsRobot = AccountSettingsRobot()

    @BeforeTest
    override fun setUp() {
        super.setUp()

        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal()))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        loginRobot
            .loginTwoPassUser()
            .skipOnboarding()
            .menuDrawer()
            .settings()
            .openUserAccountSettings(twoPassUser)
    }

    @TestId("1672")
    @Test
    fun enableAutoDownloadMessages() {
        accountSettingsRobot
            .privacy()
            .autoDownloadMessages()
            .enableAutoDownloadMessages()
            .navigateUpToPrivacySettings()
            .verify { autoDownloadImagesIsEnabled() }
    }

    @TestId("1673")
    @Test
    fun enableBackgroundSync() {
        accountSettingsRobot
            .privacy()
            .backgroundSync()
            .enableBackgroundSync()
            .navigateUpToPrivacySettings()
            .verify { backgroundSyncIsEnabled() }
    }

    @TestId("1674")
    @Test
    fun enableAutoShowRemoteImages() {
        val messageSubject = "Fw: Plan your travel risk-free with Agoda. We've got you!"
        val remoteContent = "Remote content"
        accountSettingsRobot
            .privacy()
            .enableAutoShowRemoteImages()
            .navigateUpToAccountSettings()
            .navigateUpToSettings()
            .navigateUpToInbox()
            .menuDrawer()
            .labelOrFolder(remoteContent)
            .clickMessageBySubject(messageSubject)
            .verify { showRemoteContentButtonIsGone() }
    }

    @TestId("1675")
    @Test
    fun enableAutoShowEmbeddedImages() {
        val messageSubject = "Test Embedded Image"
        val embeddedImages = "Embedded images"
        accountSettingsRobot
            .privacy()
            .enableAutoShowEmbeddedImages()
            .navigateUpToAccountSettings()
            .navigateUpToSettings()
            .navigateUpToInbox()
            .menuDrawer()
            .labelOrFolder(embeddedImages)
            .clickMessageBySubject(messageSubject)
            .verify { loadEmbeddedImagesButtonIsGone() }
    }

    @TestId("1676")
    @Test
    fun enableAndDisablePreventTakingScreenshots() {
        accountSettingsRobot
            .privacy()
            .enablePreventTakingScreenshots()
            .disablePreventTakingScreenshots()
            .verify { takingScreenshotIsDisabled() }
    }

    @TestId("1677")
    @Test
    fun enableRequestLinkConfirmation() {
        val folder = "Link confirmation"
        val linkText = "www.wikipedia.org"
        val messageTitle = "Wiki link"
        accountSettingsRobot
            .privacy()
            .enableRequestLinkConfirmation()
            .navigateUpToAccountSettings()
            .navigateUpToSettings()
            .navigateUpToInbox()
            .menuDrawer()
            .labelOrFolder(folder)
            .clickMessageBySubject(messageTitle)
            .clickLink(linkText)
            .verify { linkIsPresentInDialogMessage(linkText) }
    }
}
