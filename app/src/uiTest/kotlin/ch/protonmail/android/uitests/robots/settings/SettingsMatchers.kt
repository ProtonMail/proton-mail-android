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
package ch.protonmail.android.uitests.robots.settings

import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import ch.protonmail.android.R
import ch.protonmail.android.adapters.SettingsAdapter
import me.proton.core.presentation.ui.adapter.ClickableAdapter
import me.proton.fusion.utils.StringUtils.stringFromResource
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Matchers that are used by Settings robots.
 */
object SettingsMatchers {

    fun withSettingsHeader(@StringRes valueId: Int): Matcher<RecyclerView.ViewHolder> =
        withSettingsHeader(stringFromResource(valueId))

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

    fun withSettingsValue(@StringRes valueId: Int): Matcher<RecyclerView.ViewHolder> =
        withSettingsValue(stringFromResource(valueId))

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
        return object : BoundedMatcher<RecyclerView.ViewHolder, ClickableAdapter.ViewHolder<*, *>>(
            ClickableAdapter.ViewHolder::class.java
        ) {

            override fun describeTo(description: Description) {
                description.appendText("Label with name: $name")
            }

            override fun matchesSafely(item: ClickableAdapter.ViewHolder<*, *>): Boolean {
                val labelName = item.itemView.findViewById<TextView>(R.id.label_name_text_view).text.toString()
                return name == labelName
            }
        }
    }
}
