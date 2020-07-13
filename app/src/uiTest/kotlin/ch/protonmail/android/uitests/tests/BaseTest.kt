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

import android.preference.PreferenceManager
import android.util.Log
import androidx.test.espresso.intent.Intents
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import ch.protonmail.android.activities.guest.LoginActivity
import ch.protonmail.android.uitests.testsHelper.testRail.TestRailService
import ch.protonmail.android.uitests.testsHelper.TestExecutionWatcher
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
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation!!
        var runId = ""

        @BeforeClass
        @JvmStatic
        fun setUpBeforeClass() {
            runId = TestRailService.createTestRun();
        }
    }
}
