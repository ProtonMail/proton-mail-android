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

package ch.protonmail.android.uitests.tests.settings

import android.app.Activity
import android.app.Instrumentation
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import org.hamcrest.CoreMatchers
import org.junit.Test
import kotlin.test.BeforeTest

class PrivacyAccountSettingsTests : BaseTest() {

    private val loginRobot = LoginRobot()
    private val accountSettingsRobot = AccountSettingsRobot()

    @BeforeTest
    override fun setUp() {
        super.setUp()

        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal()))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        loginRobot
            .loginTwoPasswordUser(TestData.twoPassUser)
            .decryptMailbox(TestData.twoPassUser.mailboxPassword)
            .menuDrawer()
            .settings()
            .openUserAccountSettings(TestData.twoPassUser)
    }

    @Test
    fun enableAutoDownloadMessages() {
        accountSettingsRobot
            .privacy()
            .autoDownloadMessages()
            .enableAutoDownloadMessages()
            .navigateUpToPrivacySettings()
            .verify { autoDownloadImagesIsEnabled() }
    }

    @Test
    fun enableBackgroundSync() {
        accountSettingsRobot
            .privacy()
            .backgroundSync()
            .enableBackgroundSync()
            .navigateUpToPrivacySettings()
            .verify { backgroundSyncIsEnabled() }
    }

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

    @Test
    fun enableAutoShowEmbeddedImages() {
        val messageSubject = "2020 Lifetime account auction and raffle, and new feature announcements"
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

    @Test
    fun enableAndDisablePreventTakingScreenshots() {
        accountSettingsRobot
            .privacy()
            .enablePreventTakingScreenshots()
            .disablePreventTakingScreenshots()
            .verify { takingScreenshotIsDisabled() }
    }

    @Test
    fun enableRequestLinkConfirmation() {
        val folder = "Link confirmation"
        val linkText = "www.wikipedia.org"
        val messageTitle = "wiki"
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
