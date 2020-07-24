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
import android.view.Window
import android.widget.NumberPicker
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.PositionableRecyclerViewAction
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.R
import ch.protonmail.android.core.ProtonMailApplication
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.jetbrains.annotations.Contract
import org.junit.Assert
import kotlin.test.*

/**
 * Created by Nikola Nolchevski on 12-Apr-20.
 */
internal object UICustomViewActions {

    private const val TIMEOUT_5S = 5000L
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val window: Window
        get() = ProtonMailApplication.getApplication().currentActivity.window

    private fun getResourceName(objectId: Int): String {
        return targetContext.resources.getResourceName(objectId)
    }

    fun waitUntilViewAppears(interaction: ViewInteraction, timeout: Long = TIMEOUT_5S): ViewInteraction {
        ProtonWatcher.setTimeout(timeout)
        ProtonWatcher.waitForCondition(object : ProtonWatcher.Condition() {
            var errorMessage = ""

            override fun getDescription() = "waitForElement - $errorMessage"

            override fun checkCondition() = try {
                interaction.check(matches(isDisplayed()))
                true
            } catch (e: NoMatchingViewException) {
                if (ProtonWatcher.status == ProtonWatcher.TIMEOUT) {
                    throw e
                } else {
                    false
                }
            }
        })
        return interaction
    }

    fun waitUntilRecyclerViewPopulated(@IdRes id: Int, timeout: Long = TIMEOUT_5S) {
        ProtonWatcher.setTimeout(timeout)
        ProtonWatcher.waitForCondition(object : ProtonWatcher.Condition() {
            var errorMessage = ""

            override fun getDescription() =
                "RecyclerView: ${targetContext.resources.getResourceName(id)} was not populated with items"

            override fun checkCondition() = try {
                val rv = ActivityProvider.currentActivity!!.findViewById<RecyclerView>(id)
                rv.adapter!!.itemCount > 0
            } catch (e: Exception) {
                errorMessage = e.message.toString()
                false
            }
        })
    }

    fun waitUntilViewIsGone(viewInteraction: ViewInteraction, timeout: Long = TIMEOUT_5S): ViewInteraction {
        ProtonWatcher.setTimeout(timeout)
        val condition: ProtonWatcher.Condition = object : ProtonWatcher.Condition() {
            var errorMessage = ""

            override fun getDescription(): String {
                return "Waiting until view appears - $errorMessage\n"
            }

            override fun checkCondition(): Boolean {
                return try {
                    viewInteraction.check(matches(not(isDisplayed())))
                    true
                } catch (e: NoMatchingViewException) {
                    if (ProtonWatcher.status == ProtonWatcher.TIMEOUT) {
                        throw e
                    } else {
                        false
                    }
                }
            }
        }
        ProtonWatcher.waitForCondition(condition)
        return viewInteraction
    }

    private fun checkCondition(condition: ProtonWatcher.Condition, element: String) {
        try {
            ProtonWatcher.waitForCondition(condition)
        } catch (e: Exception) {
            Assert.fail("$element was not found")
            e.printStackTrace()
        }
    }

    fun waitUntilToastAppears(objectId: Int) {
        val condition: ProtonWatcher.Condition = object : ProtonWatcher.Condition() {
            override fun getDescription(): String {
                return "Waiting until object appears"
            }

            override fun checkCondition(): Boolean {
                return try {
                    onView(withText(objectId)).inRoot(withDecorView(Matchers.not(window.decorView)))
                        .check(matches(isDisplayed()))
                    return true
                } catch (e: NoMatchingViewException) {
                    e.printStackTrace()
                    false
                }
            }
        }
        checkCondition(condition, getResourceName(objectId))
    }

    fun waitUntilToastAppears(objectText: String) {
        val condition: ProtonWatcher.Condition = object : ProtonWatcher.Condition() {
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
        checkCondition(condition, objectText)
    }

    @Contract(value = "_ -> new", pure = true)
    fun setValueInNumberPicker(num: Int): ViewAction {
        return object : ViewAction {
            override fun perform(uiController: UiController, view: View) {
                (view as NumberPicker).value = num
            }

            override fun getDescription(): String = "Set the passed number into the NumberPicker"

            override fun getConstraints(): Matcher<View> = isAssignableFrom(NumberPicker::class.java)
        }
    }

    fun saveMessageSubject(position: Int, saveSubject: (String, String) -> Unit) = object : ViewAction {
        override fun getConstraints() = isAssignableFrom(RecyclerView::class.java)

        override fun getDescription() = "Fetches the message subject at position $position"

        override fun perform(uiController: UiController, view: View) {
            val recyclerView = view as RecyclerView
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val messageSubject = layoutManager.getChildAt(position)
                ?.findViewById<TextView>(R.id.messageTitleTextView)!!.text.toString()
            val messageDate = layoutManager.getChildAt(position)
                ?.findViewById<TextView>(R.id.messageDateTextView)!!.text.toString()
            saveSubject.invoke(messageSubject, messageDate)
        }
    }

    fun checkItemDoesNotExist(subject: String, date: String): PositionableRecyclerViewAction {
        return CheckItemDoesNotExist(subject, date)
    }

    class CheckItemDoesNotExist(private val subject: String, private val date: String) : PositionableRecyclerViewAction {

        override fun atPosition(position: Int): PositionableRecyclerViewAction =
            checkItemDoesNotExist(subject, date)

        override fun getDescription(): String = "Checking if message with subject exists in the list."

        override fun getConstraints(): Matcher<View> = allOf(isAssignableFrom(RecyclerView::class.java), isDisplayed())

        override fun perform(uiController: UiController?, view: View?) {
            var isMatches = true
            var messageSubject = ""
            var messageDate = ""
            val recyclerView = view as RecyclerView
            for (i in 0..recyclerView.adapter!!.itemCount) {
                val item = recyclerView.getChildAt(i)
                if (item != null) {
                    messageSubject = item.findViewById<TextView>(R.id.messageTitleTextView).text.toString()
                    messageDate = item.findViewById<TextView>(R.id.messageDateTextView).text.toString()
                    isMatches = (messageSubject == subject) && (messageDate == date)
                    if (isMatches) {
                        break
                    }
                }
            }
            assertFalse(isMatches, "RecyclerView should not contain item with subject: \"$subject\"")
        }
    }
}
