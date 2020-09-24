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
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.intent.Intents
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.activities.guest.LoginActivity
import ch.protonmail.android.uitests.testsHelper.ProtonFailureHandler
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestExecutionWatcher
import ch.protonmail.android.uitests.testsHelper.User
import ch.protonmail.android.uitests.testsHelper.testRail.TestRailService
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4ClassRunner::class)
open class BaseTest {

    private val activityRule = ActivityTestRule(LoginActivity::class.java)

    @Rule
    @JvmField
    val ruleChain = RuleChain
        .outerRule(testName)
        .around(testExecutionWatcher)
        .around(grantPermissionRule)
        .around(activityRule)!!

    @Before
    open fun setUp() {
        Espresso.setFailureHandler(ProtonFailureHandler(InstrumentationRegistry.getInstrumentation()))
        PreferenceManager.getDefaultSharedPreferences(targetContext).edit().clear().apply()
        Intents.init()
        clearLogcat()
        Log.d(testTag, "Starting test execution for test: ${testName.methodName}")
        // Show toast with test case name for better test analysis in recorded videos especially on Firebase.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Toast.makeText(targetContext, testName.methodName, twoSeconds).show()
        }
    }

    @After
    open fun tearDown() {
        Intents.release()
        for (idlingResource in IdlingRegistry.getInstance().resources) {
            if (idlingResource == null) {
                continue
            }
            IdlingRegistry.getInstance().unregister(idlingResource)
        }
        device.removeWatcher("SystemDialogWatcher")
        Log.d(testTag, "Finished test execution: ${testName.methodName}")
    }

    companion object {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext!!
        var shouldReportToTestRail = false
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation!!
        val testName = TestName()
        val artifactsPath = "${targetContext.filesDir.path}/artifacts"
        const val testApp = "testApp"
        const val testRailRunId = "testRailRunId"
        const val testTag = "PROTON_UI_TEST"
        private lateinit var args: Bundle
        private val testExecutionWatcher = TestExecutionWatcher()
        private const val reportToTestRail = "reportToTestRail"
        private const val oneTimeRunFlag = "oneTimeRunFlag"
        private const val downloadArtifactsPath = "/sdcard/Download/artifacts"
        private const val email = 0
        private const val password = 1
        private const val mailboxPassword = 2
        private const val twoFaKey = 3
        private const val twoSeconds = 2000
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        @JvmStatic
        @BeforeClass
        fun setUpBeforeClass() {
            getArguments()
            populateUsers()

            val sharedPrefs = targetContext.getSharedPreferences(testApp, Context.MODE_PRIVATE)

            // BeforeClass workaround for Android Test Orchestrator - shared prefs are not cleared
            val isFirstRun = sharedPrefs.getBoolean(oneTimeRunFlag, true)
            if (isFirstRun) {
                setupDevice()
                prepareArtifactsDir(artifactsPath)
                prepareArtifactsDir(downloadArtifactsPath)
                deleteDownloadArtifactsFolder()
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

        private fun getArguments() {
            val arguments = InstrumentationRegistry.getArguments()
            if (arguments != null) {
                args = arguments
            } else {
                throw NullPointerException("Test instrumentation 'arguments' must not be null")
            }
        }

        private fun populateUsers() {
            TestData.onePassUser = setUser(BuildConfig.TEST_USER1)
            TestData.twoPassUser = setUser(BuildConfig.TEST_USER2)
            TestData.onePassUserWith2FA = setUser(BuildConfig.TEST_USER3)
            TestData.twoPassUserWith2FA = setUser(BuildConfig.TEST_USER4)

            TestData.externalGmailPGPEncrypted = setUser(BuildConfig.TEST_RECIPIENT1)
            TestData.externalOutlookPGPSigned = setUser(BuildConfig.TEST_RECIPIENT2)
            TestData.internalEmailTrustedKeys = setUser(BuildConfig.TEST_RECIPIENT3)
            TestData.internalEmailNotTrustedKeys = setUser(BuildConfig.TEST_RECIPIENT4)
        }

        private fun setUser(user: String): User {
            val userParams = user.split(",")
            return User(userParams[email], userParams[password], userParams[mailboxPassword], userParams[twoFaKey])
        }

        private val grantPermissionRule = GrantPermissionRule.grant(
            READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, READ_CONTACTS
        )

        private fun prepareArtifactsDir(path: String) {
            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            } else {
                if (dir.list() != null) {
                    dir.list().forEach { File(it).delete() }
                }
            }
        }

        private fun setupDevice() {
            automation.executeShellCommand("settings put secure long_press_timeout 2000")
            automation.executeShellCommand("settings put secure show_ime_with_hard_keyboard 0")
            // Disable floating notification pop-ups
            automation.executeShellCommand("settings put global heads_up_notifications_enabled 0")
        }

        private fun clearLogcat() {
            automation.executeShellCommand("logcat -c")
        }

        private fun deleteDownloadArtifactsFolder() {
            automation.executeShellCommand("rm -rf $downloadArtifactsPath")
        }
    }
}
