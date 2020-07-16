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
import androidx.test.espresso.intent.Intents
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import ch.protonmail.android.activities.guest.LoginActivity
import ch.protonmail.android.uitests.testsHelper.TestExecutionWatcher
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.User
import ch.protonmail.android.uitests.testsHelper.testRail.TestRailService
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import org.junit.runner.RunWith

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
        PreferenceManager.getDefaultSharedPreferences(targetContext).edit().clear().apply()
        Intents.init()
        Log.d(testTag, "Starting test execution for test: ${testName.methodName}")
    }

    @After
    open fun tearDown() {
        Intents.release()
        Log.d(testTag, "Finished test execution: ${testName.methodName}")
    }

    companion object {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext!!
        const val testTag = "PROTON_UI_TEST"
        private val testName = TestName()
        private val testExecutionWatcher = TestExecutionWatcher()

        private lateinit var args: Bundle
        const val testApp = "testApp"
        private const val oneTimeRunFlag = "oneTimeRunFlag"
        private const val reportToTestRail = "reportToTestRail"
        const val testRailRunId = "testRailRunId"
        var shouldReportToTestRail = false

        @JvmStatic
        @BeforeClass
        fun setUpBeforeClass() {
            getArguments()
            populateUsers()
            populateTestRailVariables()
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

                // BeforeClass workaround for Android Test Orchestrator - shared prefs are not cleared
                val isFirstRun = targetContext.getSharedPreferences(testApp, Context.MODE_PRIVATE)
                    .getBoolean(oneTimeRunFlag, true)
                if (isFirstRun) {
                    val runId = TestRailService.createTestRun()
                    targetContext.getSharedPreferences(testApp, Context.MODE_PRIVATE)
                        .edit()
                        .putString(testRailRunId, runId)
                        .putBoolean(oneTimeRunFlag, false)
                        .commit()
                }
            }
        }

        private val grantPermissionRule = GrantPermissionRule.grant(
            READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, READ_CONTACTS
        )
    }
}
