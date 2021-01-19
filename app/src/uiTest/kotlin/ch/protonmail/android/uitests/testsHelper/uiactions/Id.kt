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

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId

object Id {
    fun clickViewWithId(@IdRes id: Int): ViewInteraction = onView(withId(id)).perform(click())

    fun insertTextIntoFieldWithId(@IdRes id: Int, text: String): ViewInteraction =
        onView(withId(id)).perform(replaceText(text), closeSoftKeyboard())

    fun insertTextInFieldWithIdAndPressImeAction(@IdRes id: Int, text: String): ViewInteraction =
        onView(withId(id)).check(matches(isDisplayed())).perform(replaceText(text), pressImeActionButton())

    fun openMenuDrawerWithId(@IdRes id: Int): ViewInteraction = onView(withId(id)).perform(DrawerActions.close(), DrawerActions.open())

    fun swipeLeftViewWithId(@IdRes id: Int): ViewInteraction = onView(withId(id)).perform(swipeLeft())

    fun swipeDownViewWithId(@IdRes id: Int): ViewInteraction = onView(withId(id)).perform(swipeDown())

    fun typeTextIntoFieldWithIdAndPressImeAction(@IdRes id: Int, text: String): ViewInteraction =
        onView(withId(id)).perform(click(), typeText(text), pressImeActionButton())

    fun typeTextIntoFieldWithId(@IdRes id: Int, text: String): ViewInteraction =
        onView(withId(id)).perform(click(), typeText(text), closeSoftKeyboard())
}
