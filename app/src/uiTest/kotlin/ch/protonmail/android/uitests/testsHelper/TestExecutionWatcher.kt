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
package ch.protonmail.android.uitests.testsHelper

import android.content.Context
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.tests.BaseTest.Companion.artifactsPath
import ch.protonmail.android.uitests.tests.BaseTest.Companion.automation
import ch.protonmail.android.uitests.testsHelper.testRail.TestRailService
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

/**
 * Monitors test run results and performs actions on Success or Failure.
 */
class TestExecutionWatcher : TestWatcher() {

    override fun failed(e: Throwable?, description: Description?) {

        val logcatFile = File(artifactsPath, "${description?.methodName}-logcat.txt")
        automation.executeShellCommand("run-as ch.protonmail.android.beta logcat -d -f $logcatFile")

        if (BaseTest.shouldReportToTestRail) {
            description!!.annotations.forEach {
                if (it is TestId) {
                    TestRailService.addResultForTestCase(it.id, 5, "Failed", getRunId())
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

    private fun getRunId(): String = BaseTest.targetContext.getSharedPreferences(BaseTest.testApp, Context.MODE_PRIVATE)
        .getString(BaseTest.testRailRunId, "")!!
}
