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

import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matcher

object AllOf {
    fun clickMatchedView(viewMatcher: Matcher<View>): ViewInteraction =
        onView(viewMatcher).perform(click())

    fun clickViewWithIdAndAncestorTag(@IdRes id: Int, ancestorTag: String): ViewInteraction =
        onView(allOf(withId(id), isDescendantOfA(withTagValue(`is`(ancestorTag)))))
            .perform(click())

    fun clickViewWithIdAndText(@IdRes id: Int, text: String): ViewInteraction =
        onView(allOf(withId(id), withText(text))).perform(click())

    fun clickVisibleViewWithId(@IdRes id: Int): ViewInteraction =
        onView(allOf(withId(id), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(click())

    fun clickViewWithParentIdAndClass(@IdRes id: Int, clazz: Class<*>): ViewInteraction =
        onView(allOf(instanceOf(clazz), withParent(withId(id)))).perform(click())

    fun clickViewByClassAndParentClass(clazz: Class<*>, parentClazz: Class<*>): ViewInteraction =
        onView(allOf(instanceOf(clazz), withParent(instanceOf(parentClazz)))).perform(click())!!

    fun clickViewWithIdAndText(@IdRes id: Int, @StringRes stringRes: Int): ViewInteraction =
        onView(allOf(withId(id), withText(stringRes)))
            .check(matches(isDisplayed()))
            .perform(click())

    fun setTextIntoFieldWithIdAndAncestorTag(
        @IdRes id: Int,
        ancestorTag: String,
        text: String
    ): ViewInteraction =
        onView(allOf(withId(id), isDescendantOfA(withTagValue(`is`(ancestorTag)))))
            .perform(replaceText(text))

    fun setTextIntoFieldWithIdAndHint(
        @IdRes id: Int,
        @StringRes stringId: Int,
        text: String
    ): ViewInteraction =
        onView(allOf(withId(id), withHint(stringId))).perform(replaceText(text))

    fun setTextIntoFieldByIdAndParent(
        @IdRes id: Int,
        @IdRes ancestorId: Int,
        text: String
    ): ViewInteraction =
        onView(allOf(withId(id), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE), isDescendantOfA(withId(ancestorId))))
            .perform(replaceText(text))
}
