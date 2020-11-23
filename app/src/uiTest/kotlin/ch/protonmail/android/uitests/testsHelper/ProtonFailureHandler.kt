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

import android.app.Instrumentation
import android.view.View
import androidx.test.espresso.FailureHandler
import androidx.test.espresso.base.DefaultFailureHandler
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.uitests.tests.BaseTest.Companion.artifactsPath
import ch.protonmail.android.uitests.tests.BaseTest.Companion.testName
import com.jraska.falcon.Falcon
import org.hamcrest.Matcher
import java.io.File

class ProtonFailureHandler(instrumentation: Instrumentation) : FailureHandler {

    private val delegate: FailureHandler

    init {
        delegate = DefaultFailureHandler(instrumentation.targetContext)
    }

    override fun handle(error: Throwable, viewMatcher: Matcher<View>) {
        if (ProtonWatcher.status == ProtonWatcher.CONDITION_NOT_MET) {
            // just delegate as we are in the condition check loop
            delegate.handle(error, viewMatcher)
        } else {
            val file = File(artifactsPath, "${testName.methodName}-screenshot.png")
            val activity = ActivityProvider.currentActivity
            if (activity != null) {
                Falcon.takeScreenshot(activity, file)
            }
            delegate.handle(error, viewMatcher)
        }
    }
}
