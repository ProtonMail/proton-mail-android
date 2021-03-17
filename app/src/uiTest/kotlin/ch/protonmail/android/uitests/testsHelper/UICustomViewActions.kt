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

import android.content.Intent
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.contrib.RecyclerViewActions.PositionableRecyclerViewAction
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.R
import ch.protonmail.android.contacts.list.listView.ContactsListAdapter
import junit.framework.AssertionFailedError
import me.proton.core.test.android.instrumented.uiwaits.UIWaits.waitUntilLoaded
import me.proton.core.test.android.instrumented.utils.ActivityProvider.currentActivity
import me.proton.core.test.android.instrumented.watchers.ProtonWatcher
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.jetbrains.annotations.Contract
import kotlin.test.assertFalse

object UICustomViewActions {

    const val TIMEOUT_15S = 15_000L
    const val TIMEOUT_10S = 10_000L
    private const val TIMEOUT_5S = 5_000L
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    fun waitUntilIntentMatcherFulfilled(
        matcher: Matcher<Intent>,
        timeout: Long = TIMEOUT_5S
    ) {
        ProtonWatcher.setTimeout(timeout)
        ProtonWatcher.waitForCondition(object : ProtonWatcher.Condition {
            var errorMessage = ""

            override fun getDescription() = "UICustomViewActions.waitUntilIntentMatcherFulfilled $errorMessage"

            override fun checkCondition(): Boolean {
                return try {
                    intended(matcher)
                    true
                } catch (e: AssertionFailedError) {
                    if (ProtonWatcher.status == ProtonWatcher.TIMEOUT) {
                        throw e
                    } else {
                        false
                    }
                }
            }
        })
    }

    fun waitForAdapterItemWithIdAndText(
        @IdRes recyclerViewId: Int,
        @IdRes viewId: Int,
        text: String,
        timeout: Long = 5000L
    ) {
        ProtonWatcher.setTimeout(timeout)
        ProtonWatcher.waitForCondition(object : ProtonWatcher.Condition {

            override fun getDescription() =
                "RecyclerView: ${targetContext.resources.getResourceName(recyclerViewId)} was not populated with items"

            override fun checkCondition(): Boolean {
                try {
                    val rv = currentActivity!!.findViewById<RecyclerView>(recyclerViewId)
                    waitUntilLoaded { rv }

                    (rv.adapter!! as ContactsListAdapter).items.forEach {
                        return if (it.getEmail() == text) {
                            return true
                        } else {
                            if (ProtonWatcher.status == ProtonWatcher.TIMEOUT) {
                                throw Exception(getDescription())
                            } else {
                                false
                            }
                        }
                    }
                } catch (e: Exception) {
                    throw  e
                }
                return false
            }
        })
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
                ?.findViewById<TextView>(R.id.subjectTextView)?.text.toString()
            val messageDate = layoutManager.getChildAt(position)
                ?.findViewById<TextView>(R.id.subjectTextView)?.text.toString()
            saveSubject.invoke(messageSubject, messageDate)
        }
    }

    fun checkMessageDoesNotExist(subject: String, date: String): PositionableRecyclerViewAction =
        CheckMessageDoesNotExist(subject, date)

    class CheckMessageDoesNotExist(
        private val subject: String,
        private val date: String
    ) : PositionableRecyclerViewAction {

        override fun atPosition(position: Int): PositionableRecyclerViewAction =
            checkMessageDoesNotExist(subject, date)

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
                    messageSubject = item.findViewById<TextView>(R.id.subjectTextView).text.toString()
                    messageDate = item.findViewById<TextView>(R.id.subjectTextView).text.toString()
                    isMatches = messageSubject == subject && messageDate == date
                    if (isMatches) {
                        break
                    }
                }
            }
            assertFalse(isMatches, "RecyclerView should not contain item with subject: \"$subject\"")
        }
    }

    fun checkContactDoesNotExist(name: String, email: String): PositionableRecyclerViewAction =
        CheckContactDoesNotExist(name, email)

    class CheckContactDoesNotExist(
        private val name: String,
        private val email: String
    ) : PositionableRecyclerViewAction {

        override fun atPosition(position: Int): PositionableRecyclerViewAction =
            checkContactDoesNotExist(name, email)

        override fun getDescription(): String = "Checking if contact with name and email exists in the list."

        override fun getConstraints(): Matcher<View> = allOf(isAssignableFrom(RecyclerView::class.java), isDisplayed())

        override fun perform(uiController: UiController?, view: View?) {
            var isMatches = true
            var contactName = ""
            var contactEmail = ""
            val recyclerView = view as RecyclerView
            for (i in 0..recyclerView.adapter!!.itemCount) {
                val item = recyclerView.getChildAt(i)
                if (item != null) {
                    contactName = item.findViewById<TextView>(R.id.contact_name)?.text.toString()
                    contactEmail = item.findViewById<TextView>(R.id.email)?.text.toString()
                    isMatches = contactName == name && contactEmail == email
                    if (isMatches) {
                        break
                    }
                }
            }
            assertFalse(isMatches, "RecyclerView should not contain contact with name: \"$name\"")
        }
    }


    fun checkGroupDoesNotExist(name: String, email: String): PositionableRecyclerViewAction =
        CheckGroupDoesNotExist(name, email)

    class CheckGroupDoesNotExist(
        private val name: String,
        private val email: String
    ) : PositionableRecyclerViewAction {

        override fun atPosition(position: Int): PositionableRecyclerViewAction =
            checkGroupDoesNotExist(name, email)

        override fun getDescription(): String = "Checking if contact with name and email exists in the list."

        override fun getConstraints(): Matcher<View> = allOf(isAssignableFrom(RecyclerView::class.java), isDisplayed())

        override fun perform(uiController: UiController?, view: View?) {
            var isMatches = true
            var contactName = ""
            var contactEmail = ""
            val recyclerView = view as RecyclerView
            for (i in 0..recyclerView.adapter!!.itemCount) {
                val item = recyclerView.getChildAt(i)
                if (item != null) {
                    contactName = item.findViewById<TextView>(R.id.contact_name)?.text.toString()
                    contactEmail = item.findViewById<TextView>(R.id.email)?.text.toString()
                    isMatches = contactName == name && contactEmail == email
                    if (isMatches) {
                        break
                    }
                }
            }
            assertFalse(isMatches, "RecyclerView should not contain contact with name: \"$name\"")
        }
    }

    fun clickOnChildWithId(@IdRes id: Int): ViewAction {
        return object : ViewAction {
            override fun perform(uiController: UiController, view: View) {
                view.findViewById<View>(id).performClick()
            }

            override fun getDescription(): String = "Click child view with id."

            override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)
        }
    }
}
