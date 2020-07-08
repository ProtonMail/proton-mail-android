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

import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.NumberPicker
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.core.ProtonMailApplication
import com.azimolabs.conditionwatcher.ConditionWatcher
import com.azimolabs.conditionwatcher.Instruction
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.jetbrains.annotations.Contract
import org.junit.Assert
import java.util.concurrent.CountDownLatch

/**
 * Created by Nikola Nolchevski on 12-Apr-20.
 */
internal object UICustomViewActionsAndMatchers {
    fun getResourceName(objectId: Int): String {
        return InstrumentationRegistry.getInstrumentation().targetContext.resources.getResourceName(objectId)
    }

    private val window: Window
        get() = ProtonMailApplication.getApplication().currentActivity.window

    /**
     * @param matcher matcher
     * @return String from TextView
     */
    fun getTextFromTextView(matcher: Matcher<View>?): String? {
        val stringHolder = arrayOf<String?>(null)
        onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(TextView::class.java)
            }

            override fun getDescription(): String {
                return "getting text from a TextView"
            }

            override fun perform(uiController: UiController, view: View) {
                val tv = view as TextView //Save, because of check in getConstraints()
                stringHolder[0] = tv.text.toString()
            }
        })
        return stringHolder[0]
    }

    fun waitUntilObjectWithTextAppears(objectText: String) {
        val instruction: Instruction = object : Instruction() {
            override fun getDescription(): String {
                return "Waiting until object appears"
            }

            override fun checkCondition(): Boolean {
                return try {
                    onView(withText(objectText)).check(matches(isDisplayed()))
                    true
                } catch (e: NoMatchingViewException) {
                    e.printStackTrace()
                    false
                }
            }
        }
        checkCondition(instruction, objectText)
    }

    fun waitUntilObjectWithIdAppears(objectId: Int): ViewInteraction {
        val viewInteraction = onView(withId(objectId))
        val instruction: Instruction = object : Instruction() {
            override fun getDescription(): String {
                return "Waiting until object appears"
            }

            override fun checkCondition(): Boolean {
                return try {
                    viewInteraction.check(matches(isDisplayed()))
                    true
                } catch (e: NoMatchingViewException) {
                    e.printStackTrace()
                    false
                }
            }
        }
        checkCondition(instruction, getResourceName(objectId))
        return viewInteraction
    }

    private fun checkCondition(instruction: Instruction, element: String) {
        try {
            ConditionWatcher.waitForCondition(instruction)
        } catch (e: Exception) {
            Assert.fail("$element was not found")
            e.printStackTrace()
        }
    }

    fun waitUntilObjectWithIdAndTextAppears(objectId: Int, text: String): ViewInteraction {
        val viewInteraction = onView(allOf(withId(objectId), withText(text))).inRoot(isPlatformPopup())
        val instruction: Instruction = object : Instruction() {
            override fun getDescription(): String {
                return "Waiting until object appears"
            }

            override fun checkCondition(): Boolean {
                return try {
                    viewInteraction.check(matches(isDisplayed()))
                    true
                } catch (e: NoMatchingViewException) {
                    e.printStackTrace()
                    false
                }
            }
        }
        ConditionWatcher.waitForCondition(instruction)
        return viewInteraction
    }

    fun waitUntilObjectWithContentDescriptionAppears(contentDescription: String) {
        val instruction: Instruction = object : Instruction() {
            override fun getDescription(): String {
                return "Waiting until object appears"
            }

            override fun checkCondition(): Boolean {
                return try {
                    onView(withContentDescription(contentDescription)).check(matches(isDisplayed()))
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }
        checkCondition(instruction, contentDescription)
    }

    fun waitUntilToastAppears(objectId: Int) {
        val instruction: Instruction = object : Instruction() {
            override fun getDescription(): String {
                return "Waiting until object appears"
            }

            override fun checkCondition(): Boolean {
                return try {
                    onView(withText(objectId)).inRoot(withDecorView(Matchers.not(window.decorView)))
                        .check(matches(isDisplayed()))
                    true
                } catch (e: NoMatchingViewException) {
                    e.printStackTrace()
                    false
                }
            }
        }
        checkCondition(instruction, getResourceName(objectId))
    }

    fun waitUntilToastAppears(objectText: String) {
        val instruction: Instruction = object : Instruction() {
            override fun getDescription(): String {
                return "Waiting until object appears"
            }

            override fun checkCondition(): Boolean {
                return try {
                    onView(withText(objectText)).inRoot(withDecorView(not(window.decorView)))
                        .check(matches(isDisplayed()))
                    true
                } catch (e: NoMatchingViewException) {
                    e.printStackTrace()
                    false
                }
            }
        }
        checkCondition(instruction, objectText)
    }

    @get:Contract(pure = true)
    val isNotDisplayed: ViewAssertion
        get() = ViewAssertion { view: View?, noView: NoMatchingViewException? ->
            if (view != null && isDisplayed().matches(view)) {
                throw AssertionError("View is present in the hierarchy and Displayed: "
                    + HumanReadables.describe(view))
            }
        }

    fun viewExists(viewMatcher: Matcher<View>, millis: Long): Boolean {
        val found = arrayOfNulls<Boolean>(1)
        val latch = CountDownLatch(1)
        val action: ViewAction = object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot()
            }

            override fun getDescription(): String {
                return "wait for a specific view with id <$viewMatcher> during $millis millis."
            }

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadUntilIdle()
                val startTime = System.currentTimeMillis()
                val endTime = startTime + millis
                do {
                    for (child in TreeIterables.breadthFirstViewTraversal(view)) {
                        if (viewMatcher.matches(child)) {
                            found[0] = true
                            latch.countDown()
                            return
                        }
                    }
                    uiController.loopMainThreadForAtLeast(50)
                } while (System.currentTimeMillis() < endTime)
                found[0] = false
                latch.countDown()
            }
        }
        onView(isRoot()).perform(action)
        try {
            latch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return found[0]!!
    }

    @Contract("_, _ -> new")
    fun isChildOf(parentMatcher: Matcher<View?>, childPosition: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("position $childPosition of parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                if (view.parent !is ViewGroup) return false
                val parent = view.parent as ViewGroup
                return (parentMatcher.matches(parent)
                    && parent.childCount > childPosition && parent.getChildAt(childPosition) == view)
            }
        }
    }

    @Contract(value = "_ -> new", pure = true)
    fun setValueInNumberPicker(num: Int): ViewAction {
        return object : ViewAction {
            override fun perform(uiController: UiController, view: View) {
                val np = view as NumberPicker
                np.value = num
            }

            override fun getDescription(): String {
                return "Set the passed number into the NumberPicker"
            }

            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(NumberPicker::class.java)
            }
        }
    }
}