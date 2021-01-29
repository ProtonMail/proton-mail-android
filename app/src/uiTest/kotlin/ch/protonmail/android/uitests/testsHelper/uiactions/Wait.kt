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

import android.content.Intent
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import ch.protonmail.android.uitests.tests.BaseTest.Companion.device
import ch.protonmail.android.uitests.testsHelper.StringUtils
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.waitUntilMatcherFulfilled
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.waitUntilViewAppears
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.waitUntilViewIsGone
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matcher
import org.junit.Assert

object Wait {

    fun forViewWithTextByUiAutomator(text: String) {
        Assert.assertTrue(device.wait(Until.hasObject(By.text(text)), 5000))
    }

    fun forViewByViewInteraction(interaction: ViewInteraction): ViewInteraction =
        waitUntilViewAppears(interaction)

    fun forViewWithContentDescription(@StringRes textId: Int): ViewInteraction =
        waitUntilViewAppears(onView(withContentDescription(containsString(StringUtils.stringFromResource(textId)))))

    fun forViewWithId(@IdRes id: Int, timeout: Long = 10_000L): ViewInteraction =
        waitUntilViewAppears(onView(withId(id)), timeout)

    fun forViewWithIdAndText(@IdRes id: Int, text: String): ViewInteraction =
        waitUntilViewAppears(onView(allOf(withId(id), withText(text))))

    fun forViewWithIdAndText(@IdRes id: Int, textId: Int, timeout: Long = 5000): ViewInteraction =
        waitUntilViewAppears(onView(allOf(withId(id), withText(textId))), timeout)

    fun forViewWithIdAndParentId(@IdRes id: Int, @IdRes parentId: Int): ViewInteraction =
        waitUntilViewAppears(onView(allOf(withId(id), withParent(withId(parentId)))))

    fun untilViewWithIdDisabled(@IdRes id: Int): ViewInteraction =
        waitUntilMatcherFulfilled(onView(withId(id)), matches(isEnabled()))

    fun untilViewWithIdIsGone(@IdRes id: Int): ViewInteraction =
        waitUntilViewIsGone(onView(withId(id)))

    fun forViewOfInstanceWithParentId(@IdRes id: Int, clazz: Class<*>, timeout: Long = 5000): ViewInteraction =
        waitUntilViewAppears(onView(allOf(instanceOf(clazz), withParent(withId(id)))), timeout)

    fun forViewWithText(@StringRes textId: Int): ViewInteraction =
        waitUntilViewAppears(onView(withText(StringUtils.stringFromResource(textId))))

    fun forViewWithText(text: String): ViewInteraction =
        waitUntilViewAppears(onView(withText(text)))

    fun forViewWithTextAndParentId(@StringRes text: Int, @IdRes parentId: Int): ViewInteraction =
        waitUntilViewAppears(onView(allOf(withText(text), withParent(withId(parentId)))))

    fun forViewWithTextAndParentId(text: String, @IdRes parentId: Int): ViewInteraction =
        waitUntilViewAppears(onView(allOf(withText(text), withParent(withId(parentId)))))

    fun untilViewWithIdEnabled(@IdRes id: Int): ViewInteraction =
        waitUntilMatcherFulfilled(onView(withId(id)), matches(isEnabled()))

    fun untilViewByViewInteractionIsGone(interaction: ViewInteraction): ViewInteraction =
        waitUntilViewIsGone(interaction)

    fun untilViewWithTextIsGone(@StringRes textId: Int): ViewInteraction =
        waitUntilViewIsGone(onView(withText(StringUtils.stringFromResource(textId))))

    fun untilViewWithTextIsGone(text: String): ViewInteraction =
        waitUntilViewIsGone(onView(withText(text)))

    fun forViewWithIdAndAncestorId(@IdRes id: Int, @IdRes parentId: Int): ViewInteraction =
        waitUntilViewAppears(onView(allOf(withId(id), ViewMatchers.isDescendantOfA(withId(parentId)))))

    fun untilViewWithIdIsNotShown(@IdRes id: Int): ViewInteraction =
        UICustomViewActions.waitUntilViewIsNotDisplayed(onView(withId(id)))

    fun forIntent(matcher: Matcher<Intent>) = UICustomViewActions.waitUntilIntentMatcherFulfilled(matcher)
}
