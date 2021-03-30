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
package ch.protonmail.android.uitests.tests

import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.widget.Toast
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestExecutionWatcher
import ch.protonmail.android.uitests.testsHelper.User
import ch.protonmail.android.uitests.testsHelper.testRail.TestRailService
import ch.protonmail.android.utils.Logger
import me.proton.core.test.android.instrumented.CoreTest
import me.proton.core.test.android.instrumented.devicesetup.DeviceSetup.copyAssetFileToInternalFilesStorage
import me.proton.core.test.android.instrumented.devicesetup.DeviceSetup.deleteDownloadArtifactsFolder
import me.proton.core.test.android.instrumented.devicesetup.DeviceSetup.prepareArtifactsDir
import me.proton.core.test.android.instrumented.devicesetup.DeviceSetup.setupDevice
import org.apache.commons.logging.Log
import org.junit.After
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlin.test.BeforeTest

@RunWith(AndroidJUnit4ClassRunner::class)
open class BaseTest : CoreTest() {

    private val activityRule = ActivityTestRule(LoginActivity::class.java)

    @Rule
    @JvmField
    val ruleChain = RuleChain
        .outerRule(testName)
        .around(testExecutionWatcher)
        .around(grantPermissionRule)
        .around(activityRule)!!

    @BeforeTest
    override fun setUp() {
        super.setUp()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Toast.makeText(targetContext, testName.methodName, tenSeconds).show()
        }
    }

    @After
    override fun tearDown() {
        super.tearDown()
        device.removeWatcher("SystemDialogWatcher")
    }

    companion object {
        var shouldReportToTestRail = false
        private val testExecutionWatcher = TestExecutionWatcher()
        private const val oneTimeRunFlag = "oneTimeRunFlag"
        private const val email = 0
        private const val password = 1
        private const val mailboxPassword = 2
        private const val twoFaKey = 3
        private const val tenSeconds = 10_000
        private val grantPermissionRule = GrantPermissionRule.grant(
            READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, READ_CONTACTS
        )

        @JvmStatic
        @BeforeClass
        fun setUpBeforeClass() {
            populateUsers()
            automation.executeShellCommand("mkdir /data/data/ch.protonmail.android.beta/files/")

            val sharedPrefs = targetContext.getSharedPreferences(testApp, Context.MODE_PRIVATE)

            // BeforeClass workaround for Android Test Orchestrator - shared prefs are not cleared
            val isFirstRun = sharedPrefs.getBoolean(oneTimeRunFlag, true)
            if (isFirstRun) {
                setupDeviceLocally(false)
                setupDevice(true)
                prepareArtifactsDir(artifactsPath)
                prepareArtifactsDir(downloadArtifactsPath)
                deleteDownloadArtifactsFolder()
                copyAssetsToDownload()
                if (shouldReportToTestRail) {
                    sharedPrefs
                        .edit()
                        .putString(testRailRunId, TestRailService.createTestRun())
                        .commit()
                }
                sharedPrefs
                    .edit()
                    .putBoolean(oneTimeRunFlag, false)
                    .commit()
            }
        }

        private fun populateUsers() {
            TestData.onePassUser = setUser(BuildConfig.TEST_USER1)
            TestData.twoPassUser = setUser(BuildConfig.TEST_USER2)
            TestData.onePassUserWith2FA = setUser(BuildConfig.TEST_USER3)
            TestData.twoPassUserWith2FA = setUser(BuildConfig.TEST_USER4)
            TestData.autoAttachPublicKeyUser = setUser(BuildConfig.TEST_USER5)

            TestData.externalGmailPGPEncrypted = setUser(BuildConfig.TEST_RECIPIENT1)
            TestData.externalOutlookPGPSigned = setUser(BuildConfig.TEST_RECIPIENT2)
            TestData.internalEmailTrustedKeys = setUser(BuildConfig.TEST_RECIPIENT3)
            TestData.internalEmailNotTrustedKeys = setUser(BuildConfig.TEST_RECIPIENT4)
        }

        private fun setUser(user: String): User {
            val userParams = user.split(",")
            return User(userParams[email], userParams[password], userParams[mailboxPassword], userParams[twoFaKey])
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
            copyAssetFileToInternalFilesStorage("lorem_ipsum.docx")
            copyAssetFileToInternalFilesStorage("lorem_ipsum.zip")
            copyAssetFileToInternalFilesStorage("lorem_ipsum.png")
            copyAssetFileToInternalFilesStorage("lorem_ipsum.jpeg")
            copyAssetFileToInternalFilesStorage("lorem_ipsum.pdf")
        }
    }
}
