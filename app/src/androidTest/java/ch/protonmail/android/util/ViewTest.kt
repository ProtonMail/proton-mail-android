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

package ch.protonmail.android.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import ch.protonmail.android.ViewTestActivity
import org.junit.Rule
import kotlin.test.BeforeTest

open class ViewTest<V : View>(
    private val buildView: (Context) -> V,
    private val width: Int = FrameLayout.LayoutParams.WRAP_CONTENT,
    private val height: Int = FrameLayout.LayoutParams.WRAP_CONTENT
) {

    @get:Rule
    val activityScenarioRule = viewScenarioRule()

    protected lateinit var testView: V
    protected val context: Context get() = testView.context

    @BeforeTest
    open fun setupView() {
        activityScenarioRule.scenario.onActivity { activity ->
            val params = FrameLayout.LayoutParams(width, height)
            testView = activity.setView(buildView, params)
            testView.id = TEST_VIEW_ID
        }
    }

    fun onTestView(): ViewInteraction =
        onView(withId(TEST_VIEW_ID))

    fun runOnActivityThread(block: () -> Unit) {
        activityScenarioRule.scenario.onActivity {
            block()
        }
    }

    private companion object {
        const val TEST_VIEW_ID = 7_435_838
    }
}

fun viewScenarioRule(
    intent: Intent? = null,
    activityOptions: Bundle? = null
): ActivityScenarioRule<ViewTestActivity> = activityScenarioRule(intent, activityOptions)
