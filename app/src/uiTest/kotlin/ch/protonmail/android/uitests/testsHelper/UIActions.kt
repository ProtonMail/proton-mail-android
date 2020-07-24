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

import android.content.Context
import android.view.KeyEvent
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnHolderItem
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToHolder
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactGroupName
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactName
import ch.protonmail.android.uitests.testsHelper.RecyclerViewMatcher.Companion.withRecyclerView
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.checkItemDoesNotExist
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.saveMessageSubject
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.waitUntilRecyclerViewPopulated
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.waitUntilViewAppears
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.waitUntilViewIsGone
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.hamcrest.Matchers

fun ViewInteraction.insert(textToBeTyped: String): ViewInteraction =
    this.perform(replaceText(textToBeTyped), closeSoftKeyboard())

fun ViewInteraction.click(): ViewInteraction =
    this.perform(ViewActions.click())

fun ViewInteraction.type(textToBeTyped: String): ViewInteraction =
    this.perform(typeText(textToBeTyped), closeSoftKeyboard())

fun ViewInteraction.verifyChecked(): ViewInteraction =
    this.check(matches(isChecked()))

fun ViewInteraction.verifyNotChecked(): ViewInteraction =
    this.check(matches(isNotChecked()))

object UIActions {

    private val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext

    val allOf = AllOf()

    class AllOf {
        fun clickViewWithIdAndAncestorTag(@IdRes id: Int, ancestorTag: String): ViewInteraction =
            onView(allOf(withId(id), isDescendantOfA(withTagValue(`is`(ancestorTag))))).perform(click())

        fun setTextIntoFieldWithIdAndAncestorTag(
            @IdRes id: Int,
            ancestorTag: String,
            textToBeTyped: String
        ): ViewInteraction =
            onView(allOf(withId(id), isDescendantOfA(withTagValue(`is`(ancestorTag)))))
                .perform(replaceText(textToBeTyped))

        fun clickViewWithIdAndText(@IdRes id: Int, text: String): ViewInteraction =
            onView(allOf(withId(id), withText(text))).perform(click())

        fun clickVisibleViewWithId(@IdRes id: Int): ViewInteraction =
            onView(allOf(withId(id), withEffectiveVisibility(Visibility.VISIBLE))).perform(click())

        fun clickViewWithIdAndContentDescription(@IdRes id: Int, @StringRes stringRes: Int): ViewInteraction =
            onView(allOf(withId(id), withContentDescription(stringRes), isDisplayed())).check(matches(isDisplayed()))
                .perform(click())

        fun clickViewWithDescendantInRecyclerView(@IdRes id: Int, title: String): ViewInteraction =
            onView(allOf(withId(id), hasDescendant(withText(title)))).perform(click())

        fun clickViewWithDescendantInRecyclerView(@IdRes id: Int, @StringRes title: Int): ViewInteraction =
            onView(allOf(withId(id), hasDescendant(withText(title)))).perform(click())

        fun clickViewWithParentIdAndClass(@IdRes id: Int, clazz: Class<*>): ViewInteraction =
            onView(allOf(instanceOf(clazz), withParent(withId(id)))).perform(click())

        fun clickViewByClassAndParentClass(clazz: Class<*>, parentClazz: Class<*>) =
            onView(allOf(instanceOf(clazz), withParent(instanceOf(parentClazz)))).perform(click())!!

        fun clickViewWithIdAndText(@IdRes id: Int, @StringRes stringRes: Int): ViewInteraction =
            onView(allOf(withId(id), withText(stringRes)))
                .check(matches(isDisplayed()))
                .perform(click())

        fun clickEmptyCacheButton(@IdRes id: Int): ViewInteraction =
            onView(allOf(withId(id), withEffectiveVisibility(Visibility.VISIBLE))).perform(click())

        fun clickMatchedView(viewMatcher: Matcher<View>): ViewInteraction =
            onView(viewMatcher).perform(click())
    }

    val check = Check()

    class Check {
        fun viewWithIdAndTextIsDisplayed(@IdRes id: Int, text: String) {
            onView(allOf(withId(id), withText(text))).check(matches(isDisplayed()))
        }

        fun viewWithIdIsNotDisplayed(@IdRes id: Int) {
            onView(withId(id)).check(matches(not(isDisplayed())))
        }

        fun viewWithIdAndTextIsNotDisplayed(@IdRes id: Int, text: String) {
            onView(allOf(withId(id), withText(text))).check(matches(not(isDisplayed())))
        }

        fun viewWithIdAndTextDoesNotExist(@IdRes id: Int, text: String) {
            onView(allOf(withId(id), withText(text))).check(doesNotExist())
        }

        fun viewWithIdAndAncestorTagIsChecked(@IdRes id: Int, ancestorTag: String, state: Boolean):
            ViewInteraction {
            return when (state) {
                true -> onView(allOf(withId(id), isDescendantOfA(withTagValue(`is`(ancestorTag)))))
                    .check(matches(isChecked()))
                false -> onView(allOf(withId(id), isDescendantOfA(withTagValue(`is`(ancestorTag)))))
                    .check(matches(isNotChecked()))
            }
        }

        fun viewWithIdAndAncestorTagIsNotChecked(@IdRes id: Int, ancestorTag: String): ViewInteraction =
            onView(allOf(withId(id), isDescendantOfA(withTagValue(`is`(ancestorTag)))))
                .check(matches(isNotChecked()))

        fun viewWithIdIsDisplayed(@IdRes id: Int) {
            onView(withId(id)).check(matches(isDisplayed()))
        }

        fun viewWithTextIsDisplayed(text: String) {
            onView(withText(text)).check(matches(isDisplayed()))
        }

        fun viewWithTextIsDisplayed(@StringRes text: Int) {
            onView(withText(text)).check(matches(isDisplayed()))
        }

        fun viewWithIdInRecyclerViewMatchesText(
            @IdRes recyclerViewId: Int,
            recyclerViewRow: Int,
            @IdRes id: Int,
            textToMatch: String
        ) {
            onView(withRecyclerView(recyclerViewId).atPositionOnView(recyclerViewRow, id))
                .check(matches(withText(textToMatch)))
        }

        fun viewWithIdInRecyclerViewRowIsDisplayed(
            @IdRes recyclerViewId: Int,
            recyclerViewRow: Int,
            @IdRes id: Int
        ) {
            onView(withRecyclerView(recyclerViewId).atPositionOnView(recyclerViewRow, id))
                .check(matches(isDisplayed()))
        }

        fun viewWithTextInRecyclerViewRowIsDisplayed(
            @IdRes recyclerViewId: Int,
            recyclerViewRow: Int,
            @StringRes text: Int
        ) {
            onView(withRecyclerView(recyclerViewId).atPositionOnView(recyclerViewRow, text))
                .check(matches(isDisplayed()))
        }

        fun viewWithTextAndParentIdIsDisplayed(@StringRes text: Int, @IdRes parentId: Int) {
            onView(allOf(withText(text), withParent(withId(parentId)))).check(matches(isDisplayed()))
        }

        fun viewWithIdAndTextSubstringIsDisplayed(@IdRes id: Int, objectText: String) {
            onView(allOf(withId(id), withSubstring(objectText))).check(matches(isDisplayed()))
        }

        fun viewWithIdHasDescendantWithSubstring(@IdRes id: Int, text: String) {
            onView(withId(id)).check(matches(allOf(isDisplayed(), hasDescendant(withSubstring(text)))))
        }

        fun viewWithIdHasDescendantWithId(@IdRes id: Int, @IdRes descendantId: Int) {
            onView(withId(id)).check(matches(allOf(isDisplayed(), hasDescendant(withId(descendantId)))))
        }

        fun viewWithIdAndTextIsDisplayed(@IdRes id: Int, @StringRes text: Int) {
            onView(allOf(withId(id), withText(text))).check(matches(isDisplayed()))
        }

        fun toastMessageIsDisplayed(@StringRes toastMessage: Int) {
            UICustomViewActions.waitUntilToastAppears(toastMessage)
        }

        fun toastMessageIsDisplayed(toastMessage: String) {
            UICustomViewActions.waitUntilToastAppears(toastMessage)
        }

        fun alertDialogWithTextIsDisplayed(@StringRes text: Int) {
            onView(withText(text))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    val classInstance = ClassInstance()

    class ClassInstance {
        fun clickViewByClassInstance(clazz: Class<*>) {
            onView(instanceOf(clazz)).perform(click())
        }
    }

    val contentDescription = ContentDescription()

    class ContentDescription {
        fun clickViewWithContentDescription(contDesc: String) {
            onView(withContentDescription(contDesc)).check(matches(isClickable())).perform(click())
        }

        fun clickViewWithContentDescSubstring(contDesc: String) {
            onView(withContentDescription(Matchers.containsString(contDesc))).perform(click())
        }
    }

    val hint = Hint()

    class Hint {
        fun insertTextIntoFieldWithHint(@IdRes hintText: Int, textToBeTyped: String) {
            onView(withHint(hintText)).perform(replaceText(textToBeTyped))
        }
    }

    val id = Id()

    class Id {
        fun insertTextIntoFieldWithId(@IdRes id: Int, textToBeTyped: String): ViewInteraction =
            onView(withId(id)).perform(replaceText(textToBeTyped), closeSoftKeyboard())

        fun clickViewWithId(@IdRes id: Int): ViewInteraction =
            onView(withId(id)).perform(click())

        fun insertTextIntoFieldWithIdPressImeAction(@IdRes id: Int, textToBeTyped: String): ViewInteraction =
            onView(withId(id))
                .perform(replaceText(textToBeTyped), pressKey(KeyEvent.KEYCODE_SPACE), closeSoftKeyboard())

        fun insertTextInFieldWithIdAndPressImeAction(@IdRes id: Int, textToBeTyped: String) {
            onView(withId(id))
                .check(matches(isDisplayed())).perform(replaceText(textToBeTyped), pressImeActionButton())
        }

        fun typeTextIntoFieldWithIdAndPressImeAction(@IdRes id: Int, textToBeTyped: String) {
            onView(withId(id)).perform(click(), typeText(textToBeTyped), pressImeActionButton())
        }

        fun typeTextIntoFieldWithId(@IdRes id: Int, textToBeTyped: String) {
            onView(withId(id)).perform(click(), typeText(textToBeTyped), closeSoftKeyboard())
        }

        fun swipeLeftViewWithId(@IdRes id: Int) {
            onView(withId(id)).perform(swipeLeft())
        }

        fun openMenuDrawerWithId(@IdRes id: Int) {
            onView(withId(id)).perform(open())
        }
    }

    val listView = ListView()

    class ListView {
        fun clickChildInListView(@IdRes id: Int, childPosition: Int): ViewInteraction =
            onData(anything()).inAdapterView(withId(id)).atPosition(childPosition).perform(click())
    }

    val recyclerView = Recycler()

    class Recycler {
        fun clickOnRecyclerViewMatchedItem(
            @IdRes recyclerViewId: Int,
            withMatcher: Matcher<RecyclerView.ViewHolder>
        ): ViewInteraction =
            onView(withId(recyclerViewId)).perform(actionOnHolderItem(withMatcher, click()))

        fun scrollToRecyclerViewMatchedItem(
            @IdRes recyclerViewId: Int,
            withMatcher: Matcher<RecyclerView.ViewHolder>
        ): ViewInteraction =
            onView(withId(recyclerViewId)).perform(scrollToHolder(withMatcher))

        fun clickOnRecyclerViewItemByPosition(@IdRes recyclerViewId: Int, position: Int): ViewInteraction =
            onView(withId(recyclerViewId)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(position, click()))

        fun swipeItemLeftToRightOnPosition(@IdRes recyclerViewId: Int, childPosition: Int): ViewInteraction =
            onView(withId(recyclerViewId))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(childPosition, swipeRight()))

        fun longClickItemInRecyclerView(@IdRes recyclerViewId: Int, childPosition: Int): ViewInteraction =
            onView(withId(recyclerViewId))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(childPosition, longClick()))

        fun swipeRightToLeftObjectWithIdAtPosition(@IdRes recyclerViewId: Int, childPosition: Int): ViewInteraction =
            onView(withId(recyclerViewId))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(childPosition, swipeLeft()))

        fun waitForBeingPopulated(@IdRes recyclerViewId: Int) = waitUntilRecyclerViewPopulated(recyclerViewId)

        fun checkDoesNotContainItemWithText(@IdRes recyclerViewId: Int, subject: String, date: String):
            ViewInteraction = onView(withId(recyclerViewId)).perform(checkItemDoesNotExist(subject, date))

        fun saveMessageSubjectAtPosition(@IdRes recyclerViewId: Int, position: Int, method: (String, String) -> Unit):
            ViewInteraction = onView(withId(recyclerViewId)).perform(saveMessageSubject(position, method))

        fun clickOnContactItem(@IdRes recyclerViewId: Int, withName: String): ViewInteraction =
            onView(withId(recyclerViewId)).perform(actionOnHolderItem(withContactName(withName), click()))

        fun clickOnGroupItem(@IdRes recyclerViewId: Int, withName: String): ViewInteraction =
            onView(withId(recyclerViewId)).perform(actionOnHolderItem(withContactGroupName(withName), click()))

        fun clickOnObjectWithIdInRecyclerViewRow(
            @IdRes recyclerViewId: Int,
            @IdRes objectId: Int,
            recyclerViewRow: Int
        ): ViewInteraction =
            onView(withRecyclerView(recyclerViewId).atPositionOnView(recyclerViewRow, objectId)).perform(click())
    }

    val system = System()

    class System {
        fun clickHamburgerOrUpButton() =
            allOf.clickViewWithParentIdAndClass(R.id.toolbar, AppCompatImageButton::class.java)

        fun clickMoreOptionsButton() =
            allOf.clickViewByClassAndParentClass(AppCompatImageView::class.java, ActionMenuView::class.java)

        fun clickPositiveDialogButton() = id.clickViewWithId(android.R.id.button1)

        fun clickNegativeDialogButton() = id.clickViewWithId(android.R.id.button2)
    }

    val tag = Tag()

    class Tag {
        fun clickViewWithTag(tag: String): ViewInteraction =
            onView(withTagValue(`is`(tag))).perform(click())

        fun clickViewWithTag(@StringRes tagStringId: Int): ViewInteraction =
            onView(withTagValue(`is`(targetContext.resources.getString(tagStringId)))).perform(click())
    }

    val text = Text()

    class Text {
        fun clickViewWithText(text: String): ViewInteraction =
            onView(withText(text)).check(matches(isDisplayed())).perform(click())

        fun clickViewWithText(@IdRes text: Int): ViewInteraction =
            onView(withText(text)).check(matches(isDisplayed())).perform(click())
    }

    val wait = Wait()

    class Wait {
        fun untilViewWithIdAndTextAppears(@IdRes id: Int, text: String): ViewInteraction =
            waitUntilViewAppears(onView(allOf(withId(id), withText(text))))

        fun untilViewWithIdAndTextAppears(@IdRes id: Int, textId: Int, timeout: Long = 5000): ViewInteraction =
            waitUntilViewAppears(onView(allOf(withId(id), withText(textId))), timeout)

        fun untilViewWithTextAppears(text: String): ViewInteraction =
            waitUntilViewAppears(onView(withText(text)))

        fun untilViewWithContentDescriptionAppears(@StringRes contentDescription: String): ViewInteraction =
            waitUntilViewAppears(onView(withContentDescription(contentDescription)))

        fun untilViewWithIdAppears(@IdRes id: Int): ViewInteraction =
            waitUntilViewAppears(onView(withId(id)))

        fun untilViewWithTextAndParentIdAppears(@StringRes text: Int, @IdRes parentId: Int) =
            waitUntilViewAppears(onView(allOf(withText(text), withParent(withId(parentId)))))

        fun untilViewWithIdIsGone(@IdRes id: Int): ViewInteraction =
            waitUntilViewIsGone(onView(withId(id)))
    }
}