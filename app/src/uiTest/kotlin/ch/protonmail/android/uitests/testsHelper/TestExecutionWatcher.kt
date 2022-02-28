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
package ch.protonmail.android.uitests.testsHelper

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.tests.BaseTest.Companion.artifactsPath
import ch.protonmail.android.uitests.tests.BaseTest.Companion.targetContext
import ch.protonmail.android.uitests.tests.BaseTest.Companion.testContext
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import ch.protonmail.android.uitests.testsHelper.testRail.TestRailService
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

/**
 * Monitors test run results and performs actions on Success or Failure.
 */
class TestExecutionWatcher : TestWatcher() {

    override fun failed(e: Throwable?, description: Description?) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation!!
        val logcatFile = File(artifactsPath, "${description?.methodName}-logcat.txt")
        automation.executeShellCommand("run-as ch.protonmail.android.beta logcat -d -f $logcatFile")

        if (BaseTest.shouldReportToTestRail) {
            description!!.annotations.forEach {
                if (it is TestId) {
                    if (e != null) {
                        TestRailService.addResultForTestCase(
                            it.id,
                            5,
                            e.message ?: e::class.simpleName!!,
                            getRunId()
                        )
                    }
                }
            }
        }
    }

    override fun succeeded(description: Description?) {
        if (BaseTest.shouldReportToTestRail) {
            description!!.annotations.forEach {
                if (it is TestId) {
                    TestRailService.addResultForTestCase(it.id, 1, "Passed", getRunId())
                }
            }
        }
    }

    private fun getRunId(): String = targetContext.getSharedPreferences(testContext.packageName, Context.MODE_PRIVATE)
        .getString("233", "")!!
}
