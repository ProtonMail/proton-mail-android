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
package ch.protonmail.android.uitests.robots.settings

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import ch.protonmail.android.R
import ch.protonmail.android.adapters.SettingsAdapter
import ch.protonmail.libs.core.ui.adapter.SelectableAdapter
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Matchers that are used by Settings robots.
 */
object SettingsMatchers {

    fun withSettingsHeader(header: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            SettingsAdapter.ItemViewHolder>(SettingsAdapter.ItemViewHolder::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("Account Settings item that contains header: $header")
            }

            override fun matchesSafely(item: SettingsAdapter.ItemViewHolder): Boolean {
                return item.header.contains(header)
            }
        }
    }

    fun withSettingsValue(value: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            SettingsAdapter.ItemViewHolder>(SettingsAdapter.ItemViewHolder::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("Account Settings item that contains value: $value")
            }

            override fun matchesSafely(item: SettingsAdapter.ItemViewHolder): Boolean {
                val valueTextView = item.itemView.findViewById<TextView>(R.id.valueText)
                if (valueTextView != null) {
                    return valueTextView.text.toString() == value
                }
                return false
            }
        }
    }

    fun withLabelName(name: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder, SelectableAdapter.ViewHolder<*>>(SelectableAdapter.ViewHolder::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("Label with name: $name")
            }

            override fun matchesSafely(item: SelectableAdapter.ViewHolder<*>): Boolean {
                val labelName = item.itemView.findViewById<TextView>(R.id.label_name).text.toString()
                return name == labelName
            }
        }
    }

//    fun waitForRecyclerViewBeingPopulated(): ViewAction {
//        return object : ViewAction {
//            override fun getDescription(): String = "Failed to wait for messages be added to recycler view."
//
//            override fun getConstraints(): Matcher<View> = ViewMatchers.isAssignableFrom(RecyclerView::class.java)
//
//            override fun perform(uiController: UiController?, view: View?) {
//                val recyclerView = view as RecyclerView
//                waitUntilPopulated(recyclerView)
//            }
//        }
//    }
//
//    private fun waitUntilPopulated(recyclerViewId: Int, timeout: Int = 10000) {
//        ConditionWatcher.setTimeoutLimit(timeout)
//        ConditionWatcher.waitForCondition(object : Instruction() {
//            var errorMessage = ""
//            val activity = ActivityProvider.currentActivity
//
//            override fun getDescription() = "waitForElement - $errorMessage"
//
//            override fun checkCondition() = try {
//
//                recyclerView.adapter!!.itemCount > 0
//            } catch (t: Throwable) {
//                errorMessage = t.toString()
//                false
//            }
//        })
//    }
}
