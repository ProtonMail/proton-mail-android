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
package ch.protonmail.android.uitests.robots.menu

import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import ch.protonmail.android.R
import ch.protonmail.android.adapters.DrawerAdapter
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Matchers that are used by [MenuRobot].
 */
object MenuMatchers {

    /**
     * Matches menu item by its tag value.
     * @param tag - expected menu item tag (which is the same as menu item name)
     */
    fun withMenuItemTag(tag: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder, DrawerAdapter.ViewHolder<*>>(DrawerAdapter.ViewHolder::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("Menu item with tag value: $tag")
            }

            override fun matchesSafely(item: DrawerAdapter.ViewHolder<*>): Boolean {
                val menuItem = item.itemView.findViewById<RelativeLayout>(R.id.menuItem)
                return menuItem?.tag?.toString() == tag
            }
        }
    }

    /**
     * Matches menu item by its tag value.
     * @param tag - expected menu item tag (which is the same as menu item name)
     */
    fun withLabelOrFolderName(name: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder, DrawerAdapter.ViewHolder<*>>(DrawerAdapter.ViewHolder::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("Menu lable or folder with name: $name")
            }

            override fun matchesSafely(item: DrawerAdapter.ViewHolder<*>): Boolean {
                val labelOrFolder = item.itemView.findViewById<AppCompatTextView>(R.id.label)
                return labelOrFolder?.tag?.toString() == name
            }
        }
    }
}
