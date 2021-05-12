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

package ch.protonmail.android.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import ch.protonmail.android.ViewTestActivity
import org.junit.Rule
import kotlin.test.BeforeTest

open class ViewTest<V : View>(
    private val buildView: (Context) -> V
) {

    @get:Rule
    val activityScenarioRule = viewScenarioRule()

    lateinit var testView: V

    @BeforeTest
    fun setupView() {
        activityScenarioRule.scenario.onActivity { activity ->
            testView = activity.setView(buildView)
            testView.id = TEST_VIEW_ID
        }
    }

    fun onTestView(): ViewInteraction =
        onView(withId(TEST_VIEW_ID))

    private companion object {
        const val TEST_VIEW_ID = 7_435_838
    }
}

fun viewScenarioRule(
    intent: Intent? = null,
    activityOptions: Bundle? = null
): ActivityScenarioRule<ViewTestActivity> = activityScenarioRule(intent, activityOptions)
