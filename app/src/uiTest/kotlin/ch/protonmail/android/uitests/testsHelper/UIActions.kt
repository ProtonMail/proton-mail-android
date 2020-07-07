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
@file:Suppress("SameParameterValue")

package ch.protonmail.android.uitests.testsHelper

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.widget.ScrollView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.uitests.testsHelper.RecyclerViewMatcher.Companion.withRecyclerView
import ch.protonmail.android.uitests.testsHelper.UICustomViewActionsAndMatchers.viewExists
import ch.protonmail.android.uitests.testsHelper.UICustomViewActionsAndMatchers.waitUntilObjectWithContentDescriptionAppears
import ch.protonmail.android.uitests.testsHelper.UICustomViewActionsAndMatchers.waitUntilObjectWithIdAppears
import ch.protonmail.android.uitests.testsHelper.UICustomViewActionsAndMatchers.waitUntilObjectWithTextAppears
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Assert


open class UIActions {

    fun waitUntilObjectWithIdAppearsInView(@IdRes objectId: Int): ViewInteraction =
        waitUntilObjectWithIdAppears(objectId)

    fun waitUntilObjectWithTextAppearsInView(objectText: String): Unit =
        waitUntilObjectWithTextAppears(objectText)

    fun waitUntilObjectWithContentDescriptionAppearsInView(@StringRes contentDescription: String): Unit =
        waitUntilObjectWithContentDescriptionAppears(contentDescription)

    fun insertTextIntoFieldWithId(@IdRes objectId: Int, textToBeTyped: String?): ViewInteraction =
        onView(withId(objectId)).perform(replaceText(textToBeTyped), closeSoftKeyboard())

    fun clickOnObjectWithId(@IdRes objectId: Int): ViewInteraction =
        onView(withId(objectId)).perform(click())

    fun clickOnObjectWithTag(tag: String): ViewInteraction =
        onView(withTagValue(`is`(tag))).perform(click())

    fun clickOnObjectWithTag(@StringRes tagStringId: Int): ViewInteraction =
        onView(withTagValue(`is`(targetContext.resources.getString(tagStringId))))
            .perform(click())

    fun clickOnObjectWithIdAndAncestorTag(@IdRes objectId: Int, ancestorTag: String): ViewInteraction =
        onView(allOf(withId(objectId), isDescendantOfA(withTagValue(`is`(ancestorTag))))).perform(click())

    fun checkObjectWithIdAndAncestorTagIsChecked(@IdRes objectId: Int, ancestorTag: String, state: Boolean): ViewInteraction {
        return when (state) {
            true -> onView(allOf(withId(objectId), isDescendantOfA(withTagValue(`is`(ancestorTag)))))
                .check(matches(isChecked()))
            false -> onView(allOf(withId(objectId), isDescendantOfA(withTagValue(`is`(ancestorTag)))))
                .check(matches(isNotChecked()))
        }
    }

    fun checkObjectWithIdAndAncestorTagIsNotChecked(@IdRes objectId: Int, ancestorTag: String): ViewInteraction =
        onView(allOf(withId(objectId), isDescendantOfA(withTagValue(`is`(ancestorTag))))).check(matches(isNotChecked()))

    fun ViewInteraction.insertText(textToBeTyped: String?): ViewInteraction =
        this.perform(replaceText(textToBeTyped), closeSoftKeyboard())

    fun ViewInteraction.verifyChecked(): ViewInteraction =
        this.check(matches(isChecked()))

    fun ViewInteraction.verifyNotChecked(): ViewInteraction =
        this.check(matches(isNotChecked()))

    fun verifyObjectWitIdAndParentId(@IdRes objectId: Int, @IdRes parentId: Int, @StringRes optionText: Int) {
        if (viewExists(allOf(withId(objectId), isNotChecked(),
                isDescendantOfA(allOf(withId(parentId), hasDescendant(withText(optionText)), isDisplayed()))), 200)) {
            onView(allOf(withId(objectId),
                isDescendantOfA(allOf(withId(parentId), hasDescendant(withText(optionText))))))
                .check(matches(isDisplayed()))
                .perform(click())
        }
    }

    fun insertTextIntoFieldWithIdAndSibling(
        @IdRes objectId: Int,
        @IdRes siblingId: Int,
        siblingText: String?,
        textToBeTyped: String?): ViewInteraction =
        onView(allOf(withId(objectId), hasSibling(allOf(withId(siblingId), withText(siblingText)))))
            .check(matches(isDisplayed()))
            .perform(clearText(), typeText(textToBeTyped), closeSoftKeyboard())

    protected fun insertTextIntoFieldWithIdAndPressImeAction(@IdRes objectId: Int, textToBeTyped: String?) {
        onView(withId(objectId)).check(matches(isDisplayed())).perform(replaceText(textToBeTyped), pressImeActionButton())
    }

    protected fun typeTextIntoFieldWithIdAndPressImeAction(@IdRes objectId: Int, textToBeTyped: String?) {
        onView(withId(objectId)).perform(click(), typeText(textToBeTyped), pressImeActionButton())
    }

    protected fun setTextIntoFieldWithIdAndAncestorTag(
        @IdRes objectId: Int,
        ancestorTag: String,
        textToBeTyped: String) {
        onView(allOf(withId(objectId), isDescendantOfA(withTagValue(`is`(ancestorTag)))))
            .perform(replaceText(textToBeTyped))
    }

    protected fun insertTextIntoFieldWithHint(@IdRes hintText: Int, textToBeTyped: String?) {
        onView(allOf(withHint(hintText), isDisplayed())).perform(replaceText(textToBeTyped))
    }

    protected fun clickOnObjectWithText(objectText: String?) {
        onView(withText(objectText)).check(matches(isDisplayed())).perform(click())
    }

    protected fun clickOnObjectWithText(@IdRes objectText: Int) {
        onView(withText(objectText)).check(matches(isDisplayed())).perform(click())
    }

    protected fun clickOnObjectWithIdAndContentDescription(@IdRes objectId: Int, @StringRes stringRes: Int) {
        onView(allOf(withId(objectId), withContentDescription(stringRes), isDisplayed())).check(matches(isDisplayed())).perform(click())
    }

    protected fun clickOnObjectWithContentDescription(contDesc: String?) {
        onView(withContentDescription(contDesc)).check(matches(isClickable())).perform(click())
    }

    protected fun clickOnObjectWithContentDescSubstring(contDesc: String?) {
        onView(withContentDescription(Matchers.containsString(contDesc))).perform(click())
    }

    protected fun clickOnObjectWithIdAndText(@IdRes objectId: Int, @StringRes stringRes: Int) {
        onView(allOf(withId(objectId), withText(stringRes)))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    protected fun clickOnObjectWithIdAndText(@IdRes objectId: Int, objectText: String?) {
        onView(allOf(withId(objectId), withText(objectText)))
            .perform(click())
    }

    protected fun checkIfObjectWithIdIsDisplayed(@IdRes objectId: Int) {
        onView(withId(objectId)).check(matches(isDisplayed()))
    }

    protected fun checkIfObjectWithTextIsDisplayed(objectText: String?) {
        onView(withText(objectText)).check(matches(isDisplayed()))
    }

    protected fun checkIfObjectWithTextIsDisplayed(@StringRes objectText: Int) {
        onView(withText(objectText)).check(matches(isDisplayed()))
    }

    protected fun checkIfObjectWithIdAndTextSubstringIsDisplayed(@IdRes objectId: Int, objectText: String?) {
        onView(allOf(withId(objectId), withSubstring(objectText))).check(matches(isDisplayed()))
    }

    protected fun checkIfObjectWithIdHasDescendantWithSubstring(@IdRes objectId: Int, text: String?) {
        onView(withId(objectId)).check(matches(allOf(isDisplayed(), hasDescendant(withSubstring(text)))))
    }

    protected fun checkIfObjectWithIdHasDescendantWithId(@IdRes objectId: Int, @IdRes descendantId: Int) {
        onView(withId(objectId)).check(matches(allOf(isDisplayed(), hasDescendant(withId(descendantId)))))
    }

    protected fun checkIfObjectWithIdAndTextIsDisplayed(@IdRes objectId: Int, text: String?) {
        onView(allOf(withId(objectId), withText(text))).check(matches(isDisplayed()))
    }

    protected fun checkIfObjectWithIdAndTextIsDisplayed(@IdRes objectId: Int, @StringRes objectText: Int) {
        onView(allOf(withId(objectId), withText(objectText))).check(matches(isDisplayed()))
    }

    fun waitWithTimeoutForObjectWithIdToAppear(@IdRes objectId: Int, timeout: Long) {
        if (!viewExists(allOf(withId(objectId), isCompletelyDisplayed(), withEffectiveVisibility(Visibility.VISIBLE)), timeout)) {
            Assert.fail(UICustomViewActionsAndMatchers.getResourceName(objectId) + " was not found")
        }
    }

    protected fun waitWithTimeoutForObjectWithIdIsClickable(@IdRes objectId: Int, timeout: Long) {
        if (!viewExists(allOf(withId(objectId), isCompletelyDisplayed(), isClickable()), timeout)) {
            Assert.fail(UICustomViewActionsAndMatchers.getResourceName(objectId) + " was not found")
        }
    }

    protected fun waitWithTimeoutForObjectWithTextToAppear(objectText: String, timeout: Long) {
        if (!viewExists(allOf(withText(objectText), isCompletelyDisplayed(), withEffectiveVisibility(Visibility.VISIBLE)), timeout)) {
            Assert.fail("$objectText was not found")
        }
    }

    protected fun waitWithTimeoutForObjectWithIdAndTextToAppear(@IdRes objectId: Int, objectText: String, timeout: Long) {
        if (!viewExists(allOf(withId(objectId), withText(objectText), withEffectiveVisibility(Visibility.VISIBLE)), timeout)) {
            Assert.fail("$objectText was not found")
        }
    }

    protected fun waitWithTimeoutForObjectWithIdAndTextToAppear(@IdRes objectId: Int, @StringRes objectText: Int, timeout: Long) {
        if (!viewExists(allOf(withId(objectId), withText(objectText), withEffectiveVisibility(Visibility.VISIBLE)), timeout)) {
            Assert.fail(UICustomViewActionsAndMatchers.getResourceName(objectText) + " was not found")
        }
    }

    protected fun checkIfViewWithIdExists(@IdRes objectId: Int): Boolean {
        return viewExists(withId(objectId), 5000)
    }

    /**
     * Returns the next position of an element in a RecyclerView which doesn't contain certain view with given resId
     */
    protected fun positionOfObjectWhichNotContainsObjectWithId(@IdRes recyclerViewId: Int, @IdRes notContainsObjectWithId: Int): Int {
        var position = 0
        while (viewExists(allOf(isDisplayed(),
                withRecyclerView(recyclerViewId).atPositionOnView(position, notContainsObjectWithId)), 200)) {
            position++
        }
        return position
    }

    protected fun positionOfObjectWhichContainsObjectWithIdAndText(@IdRes recyclerViewId: Int, @IdRes containsObjectWithId: Int, @StringRes containsObjectWithText: Int): Int {
        var position = 0
        while (!viewExists(allOf(isDisplayed(),
                withRecyclerView(recyclerViewId).atPositionOnView(position, containsObjectWithId),
                withText(containsObjectWithText)), 200)) {
            position++
        }
        return position
    }

    protected fun positionOfObjectWhichNotContainsAnyOfGivenElements(@IdRes recyclerViewId: Int, @IdRes notContainsFirstElement: Int, @IdRes notContainsSecondElement: Int): Int {
        var position = 0
        while (viewExists(allOf(isDisplayed(),
                withRecyclerView(recyclerViewId).atPositionOnView(position, notContainsFirstElement)), 200) ||
            viewExists(allOf(isDisplayed(),
                withRecyclerView(recyclerViewId).atPositionOnView(position, notContainsSecondElement)), 200)) {
            position++
        }
        return position
    }

    protected fun checkIfObjectWithPositionInRecyclerViewIsDisplayed(@IdRes recyclerViewId: Int, position: Int, @IdRes objectId: Int) {
        onView(withRecyclerView(recyclerViewId).atPositionOnView(position, objectId)).check(matches(isDisplayed()))
    }

    protected fun checkIfObjectWithIdNotDisplayed(@IdRes objectId: Int) {
        onView(withId(objectId)).check(UICustomViewActionsAndMatchers.isNotDisplayed)
    }

    protected fun checkIfObjectWithIdAndTextIsNotDisplayed(@IdRes objectId: Int, objectText: String?) {
        onView(allOf(withId(objectId), withText(objectText))).check(UICustomViewActionsAndMatchers.isNotDisplayed)
    }

    protected fun clickEmptyCacheButton(@IdRes objectId: Int) {
        onView(allOf(withId(objectId), withEffectiveVisibility(Visibility.VISIBLE))).perform(click())
    }

    protected fun clickChildInViewGroup(@IdRes parentId: Int, childPosition: Int) {
        onView(UICustomViewActionsAndMatchers.isChildOf(withId(parentId), childPosition)).check(matches(isDisplayed())).perform(click())
    }

    protected fun clickChildInRecyclerView(@IdRes recyclerViewId: Int, childPosition: Int) {
        onView(withId(recyclerViewId)).check(matches(isDisplayed())).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(childPosition, click()))
    }

    protected fun clickObjectWithDescendantInRecyclerView(@IdRes objectID: Int, title: String?) {
        onView(allOf(withId(objectID), hasDescendant(withText(title)))).perform(click())
    }

    protected fun clickObjectWithDescendantInRecyclerView(@IdRes objectID: Int, @StringRes title: Int) {
        onView(allOf(withId(objectID), hasDescendant(withText(title)))).perform(click())
    }

    protected fun clickObjectWithParentIdAndClass(@IdRes objectId: Int, clazz: Class<*>) {
        onView(allOf(instanceOf(clazz), withParent(withId(objectId)))).perform(click())
    }

    protected fun longClickItemInRecyclerView(@IdRes recyclerViewId: Int, childPosition: Int) {
        onView(withId(recyclerViewId)).check(matches(isDisplayed())).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(childPosition, longClick()))
    }

    protected fun clickChildInListView(@IdRes objectId: Int, childPosition: Int) {
        onData(anything()).inAdapterView(withId(objectId)).atPosition(childPosition).perform(click())
    }

    protected fun swipeLeftObjectWithId(@IdRes objectId: Int) {
        onView(withId(objectId)).perform(swipeLeft())
    }

    protected fun swipeLeftToRightObjectWithId(@IdRes objectId: Int, childPosition: Int) {
        onView(withId(objectId)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(childPosition, swipeRight()))
    }

    protected fun swipeRightToLeftObjectWithIdAtPosition(@IdRes objectId: Int, childPosition: Int) {
        onView(withId(objectId)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(childPosition, swipeLeft()))
    }

    protected fun scrollDownObjectWithId(@IdRes objectId: Int) {
        onView(withId(objectId)).check(matches(isDisplayed())).perform(swipeUp())
    }

    protected fun pressSpace() {
        onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_SPACE))
    }

    protected fun typeTextIntoField(@IdRes objectId: Int, text: String?) {
        onView(withId(objectId)).perform(click(), typeText(text), closeSoftKeyboard())
    }

    protected fun checkIfToastMessageIsDisplayed(@StringRes toastMessage: Int) {
        UICustomViewActionsAndMatchers.waitUntilToastAppears(toastMessage)
    }

    protected fun checkIfToastMessageIsDisplayed(toastMessage: String) {
        UICustomViewActionsAndMatchers.waitUntilToastAppears(toastMessage)
    }

    protected fun scrollDownAndClickObjectWithIdAndTextIsFound(@IdRes recyclerViewId: Int, @IdRes objectId: Int, objectContainsText: String?) {
        var position = 1
        while (!viewExists(allOf(isDisplayed(), withRecyclerView(recyclerViewId).atPosition(position),
                hasDescendant(withSubstring(objectContainsText))), 100)) {
            onView(allOf(isDisplayed(), withRecyclerView(recyclerViewId).atPosition(position + 1), hasDescendant(withId(objectId))))
                .perform(swipeUp())
            position++
        }
        onView(withId(recyclerViewId)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(position, click()))
    }

    protected fun clickComposeToGroupButtonAtPosition(@IdRes recyclerViewId: Int, @IdRes objectId: Int, position: Int) {
        onView(withRecyclerView(recyclerViewId).atPositionOnView(position, objectId)).perform(click())
    }

    protected fun getTextFromObject(@IdRes objectId: Int): String? {
        return UICustomViewActionsAndMatchers.getTextFromTextView(withId(objectId))
    }

    protected fun getTextFromObject(@IdRes objectId: Int, @IdRes siblingId: Int, siblingText: String?): String? {
        return UICustomViewActionsAndMatchers.getTextFromTextView(allOf(withId(objectId), hasSibling(allOf(withId(siblingId), withSubstring(siblingText)))))
    }

    protected fun getTextFromObjectInRecyclerViewAtPosition(@IdRes recyclerViewId: Int, @IdRes objectId: Int, position: Int): String? {
        return UICustomViewActionsAndMatchers.getTextFromTextView(withRecyclerView(recyclerViewId).atPositionOnView(position, objectId))
    }

    protected fun scrollDownElementInScrollView(@IdRes objectId: Int) {
        onView(allOf(hasDescendant(withId(objectId)), withClassName(Matchers.`is`(ScrollView::class.java.canonicalName)))).perform(swipeUp())
    }

    protected fun checkSignatureState(@IdRes objectId: Int, @IdRes parentId: Int, @StringRes signatureType: Int, state: Matcher<View>) {
        if (!viewExists(allOf(withId(objectId), state,
                isDescendantOfA(allOf(withId(parentId), hasDescendant(withText(signatureType)), isDisplayed()))), 200)) {
            Assert.fail("Signature toggle state not saved. Expected state: $state")
        }
    }

    protected fun switchOnSignature(@IdRes objectId: Int, @IdRes parentId: Int, @StringRes signatureType: Int) {
        if (!viewExists(allOf(withId(objectId), isNotChecked(),
                isDescendantOfA(allOf(withId(parentId), hasDescendant(withText(signatureType)), isDisplayed()))), 200)) {
            switchOffSignature(objectId, parentId, signatureType)
        }
        onView(allOf(withId(objectId), isNotChecked(),
            isDescendantOfA(allOf(withId(parentId), hasDescendant(withText(signatureType))))))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    protected fun switchOffSignature(@IdRes objectId: Int, @IdRes parentId: Int, @StringRes signatureType: Int) {
        if (!viewExists(allOf(withId(objectId), isChecked(),
                isDescendantOfA(allOf(withId(parentId), hasDescendant(withText(signatureType)), isDisplayed()))), 200)) {
            switchOnSignature(objectId, parentId, signatureType)
        }
        onView(allOf(withId(objectId), isChecked(),
            isDescendantOfA(allOf(withId(parentId), hasDescendant(withText(signatureType))))))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    protected fun switchOnAutoShowEmbeddedImages(@IdRes objectId: Int, @IdRes parentId: Int, @StringRes optionText: Int) {
        if (viewExists(allOf(withId(objectId), isNotChecked(),
                isDescendantOfA(allOf(withId(parentId), hasDescendant(withText(optionText)), isDisplayed()))), 200)) {
            onView(allOf(withId(objectId),
                isDescendantOfA(allOf(withId(parentId), hasDescendant(withText(optionText))))))
                .check(matches(isDisplayed()))
                .perform(click())
        }
    }

    protected fun switchOffAutoShowEmbeddedImages(@IdRes objectId: Int, @IdRes parentId: Int, @StringRes optionText: Int) {
        if (viewExists(allOf(withId(objectId), isChecked(),
                isDescendantOfA(allOf(withId(parentId), hasDescendant(withText(optionText)), isDisplayed()))), 200)) {
            onView(allOf(withId(objectId),
                isDescendantOfA(allOf(withId(parentId), hasDescendant(withText(optionText))))))
                .check(matches(isDisplayed()))
                .perform(click())
        }
    }

    companion object {
        val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    }
}