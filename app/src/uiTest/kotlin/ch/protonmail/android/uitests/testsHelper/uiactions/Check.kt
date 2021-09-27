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
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import ch.protonmail.android.uitests.testsHelper.StringUtils
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not

object Check {

    fun viewWithIdAndTextIsDisplayed(@IdRes id: Int, text: String): ViewInteraction =
        onView(allOf(withId(id), withText(text))).check(matches(isDisplayed()))

    fun viewWithIdIsNotDisplayed(@IdRes id: Int): ViewInteraction =
        onView(withId(id)).check(matches(not(isDisplayed())))

    fun viewWithIdIsContainsText(@IdRes id: Int, text: String): ViewInteraction =
        onView(withId(id)).check(matches(withText(containsString(text))))

    fun viewWithIdAndTextDoesNotExist(@IdRes id: Int, text: String): ViewInteraction =
        onView(allOf(withId(id), withText(text))).check(doesNotExist())

    fun viewWithIdAndAncestorTagIsChecked(
        @IdRes id: Int,
        ancestorTag: String,
        state: Boolean
    ): ViewInteraction {
        return when (state) {
            true ->
                onView(allOf(withId(id), isDescendantOfA(withTagValue(`is`(ancestorTag)))))
                    .check(matches(isChecked()))
            false ->
                onView(allOf(withId(id), isDescendantOfA(withTagValue(`is`(ancestorTag)))))
                    .check(matches(isNotChecked()))
        }
    }

    fun viewWithIdIsDisplayed(@IdRes id: Int): ViewInteraction = onView(withId(id)).check(matches(isDisplayed()))

    fun viewWithTextIsDisplayed(text: String): ViewInteraction =
        onView(withText(text)).check(matches(isDisplayed()))

    fun viewWithTextDoesNotExist(@StringRes textId: Int): ViewInteraction =
        onView(withText(StringUtils.stringFromResource(textId))).check(doesNotExist())

    fun viewWithIdAndTextIsDisplayed(@IdRes id: Int, @StringRes text: Int): ViewInteraction =
        onView(allOf(withId(id), withText(text))).check(matches(isDisplayed()))

    fun alertDialogWithTextIsDisplayed(@StringRes textId: Int): ViewInteraction =
        onView(withText(textId)).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))

    fun alertDialogWithPartialTextIsDisplayed(text: String): ViewInteraction =
        onView(withText(containsString(text))).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))

    fun viewWithTextIsChecked(@StringRes textId: Int): ViewInteraction =
        onView(withText(textId)).inRoot(RootMatchers.isDialog()).check(matches(isChecked()))
}
