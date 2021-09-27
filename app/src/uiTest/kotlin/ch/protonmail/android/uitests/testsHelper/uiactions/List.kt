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

package ch.protonmail.android.uitests.testsHelper.uiactions

import android.widget.ListView
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matcher

object List {

    fun checkItemWithTextExists(@IdRes adapterId: Int, text: String) {
        onData(withText(text))
            .inAdapterView(withId(adapterId))
            .check(matches(isDisplayed()))
    }

    fun clickListItemByPosition(position: Int) {
        onData(anything())
            .inAdapterView(instanceOf(ListView::class.java))
            .inRoot(isPlatformPopup())
            .atPosition(position)
            .perform(click())
    }

    fun clickListItemByText(matcher: Matcher<out Any?>?, @IdRes adapterId: Int) {
        onData(matcher)
            .inAdapterView(withId(adapterId))
            .perform(click())
    }

    fun clickListItemChildByTextAndId(matcher: Matcher<out Any?>?, @IdRes childId: Int, @IdRes adapterId: Int) {
        onData(matcher)
            .inAdapterView(withId(adapterId))
            .onChildView(withId(childId))
            .perform(click())
    }
}
