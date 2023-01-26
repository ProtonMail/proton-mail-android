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
package ch.protonmail.android.uitests.tests

import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import ch.protonmail.android.core.Constants
import ch.protonmail.android.mailbox.presentation.ui.MailboxActivity
import ch.protonmail.android.uitests.testsHelper.TestExecutionWatcher
import ch.protonmail.android.uitests.testsHelper.TestUser.populateUsers
import ch.protonmail.android.uitests.testsHelper.testRail.TestRailService
import me.proton.core.data.asset.readFromAssets
import me.proton.core.test.android.instrumented.ProtonTest.Companion.testName
import me.proton.core.test.android.instrumented.utils.FileUtils
import me.proton.core.test.android.instrumented.utils.FileUtils.prepareArtifactsDir
import me.proton.core.test.android.instrumented.utils.Shell.deleteDownloadArtifactsFolder
import me.proton.core.test.quark.data.User
import me.proton.core.util.android.sharedpreferences.set
import me.proton.fusion.Fusion
import org.junit.After
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlin.test.BeforeTest

@RunWith(AndroidJUnit4::class)
open class BaseTest: Fusion {

    private val activityRule = ActivityTestRule(MailboxActivity::class.java)

    @Rule
    @JvmField
    val ruleChain = RuleChain
        .outerRule(testName)
        .around(testExecutionWatcher)
        .around(grantPermissionRule)
        .around(activityRule)!!

    @BeforeTest
    open fun setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Toast.makeText(targetContext, testName.methodName, Toast.LENGTH_LONG).show()
        }
        populateUsers()
        shouldDisableOnboarding(true)
    }

    @After
    fun tearDown() {
        uiDevice.removeWatcher("SystemDialogWatcher")
    }

    companion object {

        private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation!!
        private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        var shouldReportToTestRail = false
        private val testExecutionWatcher = TestExecutionWatcher()
        private const val oneTimeRunFlag = "oneTimeRunFlag"
        private const val email = 0
        private const val password = 1
        private const val mailboxPassword = 2
        private const val twoFaKey = 3
        val users = User.Users.fromJson(InstrumentationRegistry.getInstrumentation().context.readFromAssets("users.json"))
        private val grantPermissionRule = GrantPermissionRule.grant(
            READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, READ_CONTACTS
        )
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val artifactsPath = "${targetContext.filesDir.path}/artifacts"
        val downloadArtifactsPath = targetContext.getExternalFilesDir(null)!!.absolutePath

        @JvmStatic
        @BeforeClass
        fun setUpBeforeClass() {
            automation.executeShellCommand("mkdir /data/data/ch.protonmail.android.beta/files/")

            val sharedPrefs = targetContext.getSharedPreferences(testContext.packageName, Context.MODE_PRIVATE)

            // BeforeClass workaround for Android Test Orchestrator - shared prefs are not cleared
            val isFirstRun = sharedPrefs.getBoolean(oneTimeRunFlag, true)
            if (isFirstRun) {
                setupDeviceLocally(false)
                prepareArtifactsDir(artifactsPath)
                prepareArtifactsDir(downloadArtifactsPath)
                deleteDownloadArtifactsFolder()
                copyAssetsToDownload()
                if (shouldReportToTestRail) {
                    sharedPrefs
                        .edit()
                        .putString("123", TestRailService.createTestRun())
                        .commit()
                }
                sharedPrefs
                    .edit()
                    .putBoolean(oneTimeRunFlag, false)
                    .commit()
            }
        }

        private fun setupDeviceLocally(shouldDisableNotifications: Boolean) {
            automation.executeShellCommand("settings put global development_settings_enabled 1")
            automation.executeShellCommand("settings put secure long_press_timeout 2000")
            automation.executeShellCommand("settings put global animator_duration_scale 0.0")
            automation.executeShellCommand("settings put global transition_animation_scale 0.0")
            automation.executeShellCommand("settings put global window_animation_scale 0.0")
            automation.executeShellCommand("settings put secure show_ime_with_hard_keyboard 0")
            if (shouldDisableNotifications) {
                // Disable floating notification pop-ups.
                automation.executeShellCommand("settings put global heads_up_notifications_enabled 0")
            }
        }

        private fun copyAssetsToDownload() {
            FileUtils.copyAssetFileToInternalFilesStorage("lorem_ipsum.docx")
            FileUtils.copyAssetFileToInternalFilesStorage("lorem_ipsum.zip")
            FileUtils.copyAssetFileToInternalFilesStorage("lorem_ipsum.png")
            FileUtils.copyAssetFileToInternalFilesStorage("lorem_ipsum.jpeg")
            FileUtils.copyAssetFileToInternalFilesStorage("lorem_ipsum.pdf")
        }

        fun shouldDisableOnboarding(flag :Boolean) {
            PreferenceManager
                .getDefaultSharedPreferences(targetContext)[Constants.Prefs.PREF_NEW_USER_ONBOARDING_SHOWN] = flag
        }
    }
}
