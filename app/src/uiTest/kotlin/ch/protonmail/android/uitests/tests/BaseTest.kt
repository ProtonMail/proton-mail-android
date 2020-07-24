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
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.intent.Intents
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import ch.protonmail.android.activities.guest.LoginActivity
import ch.protonmail.android.uitests.testsHelper.ProtonFailureHandler
import ch.protonmail.android.uitests.testsHelper.ProtonServicesIdlingResource
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
        IdlingRegistry.getInstance().register(ProtonServicesIdlingResource())
        Intents.init()
        clearLogcat()
        Log.d(testTag, "Starting test execution for test: ${testName.methodName}")
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
        Log.d(testTag, "Finished test execution: ${testName.methodName}")
    }

    companion object {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext!!
        var shouldReportToTestRail = false
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation!!
        val testName = TestName()
        private lateinit var args: Bundle
        private val testExecutionWatcher = TestExecutionWatcher()
        private const val oneTimeRunFlag = "oneTimeRunFlag"
        private const val reportToTestRail = "reportToTestRail"
        val artifactsPath = "${targetContext.filesDir.path}/artifacts"
        private const val downloadArtifactsPath = "/sdcard/Download/artifacts"
        const val testApp = "testApp"
        const val testRailRunId = "testRailRunId"
        const val testTag = "PROTON_UI_TEST"

        @JvmStatic
        @BeforeClass
        fun setUpBeforeClass() {
            getArguments()
            populateUsers()
            populateTestRailVariables()

            val sharedPrefs = targetContext.getSharedPreferences(testApp, Context.MODE_PRIVATE)

            // BeforeClass workaround for Android Test Orchestrator - shared prefs are not cleared
            val isFirstRun = sharedPrefs.getBoolean(oneTimeRunFlag, true)
            if (isFirstRun) {
                prepareArtifactsDir(artifactsPath)
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
            TestData.onePassUser = setUser("USER1")
            TestData.twoPassUser = setUser("USER2")
            TestData.onePassUserWith2FA = setUser("USER3")
            TestData.twoPassUserWith2FA = setUser("USER4")

            TestData.internalEmailTrustedKeys = setUser("RECIPIENT1")
            TestData.internalEmailNotTrustedKeys = setUser("RECIPIENT2")
            TestData.externalEmailPGPEncrypted = setUser("RECIPIENT3")
            TestData.externalEmailPGPSigned = setUser("RECIPIENT4")
        }

        private fun setUser(key: String): User {
            val userData = args.getString(key)
            return if (userData != null) {
                val user = args.getString(key)!!.split(",")
                User(user[0], user[1], user[2], user[3])
            } else {
                throw NullPointerException("Test instrumentation 'argument with key: $key' must not be null")
            }
        }

        private fun populateTestRailVariables() {
            shouldReportToTestRail = args.getBoolean(reportToTestRail)
            if (shouldReportToTestRail) {
                val testRailProjectId = args.getString("TESTRAIL_PROJECT_ID")
                val testRailUsername = args.getString("TESTRAIL_USERNAME")
                val testRailPassword = args.getString("TESTRAIL_PASSWORD")
                if (testRailProjectId != null || testRailUsername != null || testRailPassword != null) {
                    TestRailService.TESTRAIL_PROJECT_ID = testRailProjectId!!
                    TestRailService.TESTRAIL_USERNAME = testRailUsername!!
                    TestRailService.TESTRAIL_PASSWORD = testRailPassword!!
                } else {
                    throw NullPointerException("Test instrumentation 'TestRail arguments' must not be null")
                }
            }
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

        private fun clearLogcat() {
            automation.executeShellCommand("logcat -c")
        }

        private fun deleteDownloadArtifactsFolder() {
            automation.executeShellCommand("rm -rf $downloadArtifactsPath")
        }
    }
}
