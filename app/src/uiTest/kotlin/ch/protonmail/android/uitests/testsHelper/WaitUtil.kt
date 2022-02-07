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

import android.util.Log
import me.proton.core.test.android.instrumented.ProtonTest
import me.proton.core.test.android.instrumented.utils.Shell
import java.util.concurrent.TimeoutException

fun waitForCondition(
    conditionBlock: () -> Unit,
    watchTimeout: Long = ProtonTest.commandTimeout,
    watchInterval: Long = 250L,
) {
    var throwable: Throwable = TimeoutException("Condition was not met in $watchTimeout ms. No exceptions caught.")
    var currentTimestamp = System.currentTimeMillis()
    val timeoutTimestamp = currentTimestamp + watchTimeout

    while (currentTimestamp < timeoutTimestamp) {
        currentTimestamp = System.currentTimeMillis()
        try {
            return conditionBlock()
        } catch (e: Throwable) {
            val firstLine = e.message?.split("\n")?.get(0)
            Log.v(ProtonTest.testTag, "Waiting for condition. ${timeoutTimestamp - currentTimestamp}ms remaining. Status: $firstLine")
            throwable = e
        }
        Thread.sleep(watchInterval)
    }
    Log.d(ProtonTest.testTag, "Test \"${ProtonTest.testName.methodName}\" failed. Saving screenshot")
    Shell.takeScreenshot()
    throw throwable
}
