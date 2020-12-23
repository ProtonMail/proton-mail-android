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
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnHolderItem
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToHolder
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactEmail
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactEmailInManageAddressesView
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactGroupName
import ch.protonmail.android.uitests.robots.manageaccounts.ManageAccountsMatchers.withAccountEmailInAccountManager
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.checkContactDoesNotExist
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.checkMessageDoesNotExist
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.clickOnChildWithId
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.performActionWithRetry
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.saveMessageSubject
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.waitForAdapterItemWithIdAndText
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.waitUntilRecyclerViewPopulated
import org.hamcrest.Matcher

object Recycler {

    val common = Common()
    val contacts = Contacts()
    val manageAccounts = ManageAccounts()
    val messages = Messages()

    class Common {

        fun clickOnRecyclerViewMatchedItem(
            @IdRes recyclerViewId: Int,
            withMatcher: Matcher<RecyclerView.ViewHolder>
        ): ViewInteraction =
            onView(withId(recyclerViewId)).perform(actionOnHolderItem(withMatcher, click()))

        fun clickOnRecyclerViewItemChild(
            @IdRes recyclerViewId: Int,
            withMatcher: Matcher<RecyclerView.ViewHolder>,
            @IdRes childViewId: Int
        ): ViewInteraction = onView(withId(recyclerViewId))
            .perform(actionOnHolderItem(withMatcher, clickOnChildWithId(childViewId)))

        fun clickOnRecyclerViewMatchedItemWithRetry(
            @IdRes recyclerViewId: Int,
            withMatcher: Matcher<RecyclerView.ViewHolder>
        ): ViewInteraction =
            performActionWithRetry(onView(withId(recyclerViewId)), actionOnHolderItem(withMatcher, click()))

        fun clickOnRecyclerViewItemByPosition(@IdRes recyclerViewId: Int, position: Int): ViewInteraction =
            onView(withId(recyclerViewId)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(position, click()))

        fun longClickItemInRecyclerView(@IdRes recyclerViewId: Int, childPosition: Int): ViewInteraction =
            onView(withId(recyclerViewId))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(childPosition, longClick()))

        fun scrollToRecyclerViewMatchedItem(
            @IdRes recyclerViewId: Int,
            withMatcher: Matcher<RecyclerView.ViewHolder>
        ): ViewInteraction =
            onView(withId(recyclerViewId)).perform(scrollToHolder(withMatcher))

        fun swipeItemLeftToRightOnPosition(@IdRes recyclerViewId: Int, childPosition: Int): ViewInteraction =
            onView(withId(recyclerViewId))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(childPosition, swipeRight()))

        fun swipeDownToRightOnPosition(@IdRes recyclerViewId: Int, childPosition: Int): ViewInteraction =
            onView(withId(recyclerViewId))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(childPosition, swipeDown()))

        fun swipeRightToLeftObjectWithIdAtPosition(@IdRes recyclerViewId: Int, childPosition: Int): ViewInteraction =
            onView(withId(recyclerViewId))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(childPosition, swipeLeft()))

        fun waitForBeingPopulated(@IdRes recyclerViewId: Int): Recycler {
            waitUntilRecyclerViewPopulated(recyclerViewId)
            return Recycler
        }

        fun waitForItemWithIdAndText(@IdRes recyclerViewId: Int, @IdRes viewId: Int, text: String): Recycler {
            waitForAdapterItemWithIdAndText(recyclerViewId, viewId, text)
            return Recycler
        }
    }

    class Contacts {

        fun clickContactItem(@IdRes recyclerViewId: Int, withEmail: String): ViewInteraction =
            onView(withId(recyclerViewId)).perform(actionOnHolderItem(withContactEmail(withEmail), click()))

        fun clickContactItemWithRetry(@IdRes recyclerViewId: Int, withEmail: String): ViewInteraction =
            performActionWithRetry(
                onView(withId(recyclerViewId)),
                actionOnHolderItem(withContactEmail(withEmail), click())
            )

        fun clickContactItemView(
            @IdRes recyclerViewId: Int,
            withEmail: String,
            @IdRes childViewId: Int
        ): ViewInteraction = onView(withId(recyclerViewId))
            .perform(actionOnHolderItem(withContactEmail(withEmail), clickOnChildWithId(childViewId)))

        fun clickContactsGroupItemView(
            @IdRes recyclerViewId: Int,
            withName: String,
            @IdRes childViewId: Int
        ): ViewInteraction = onView(withId(recyclerViewId))
            .perform(actionOnHolderItem(withContactGroupName(withName), clickOnChildWithId(childViewId)))

        fun selectContactsInManageAddresses(@IdRes recyclerViewId: Int, withEmail: String): ViewInteraction =
            onView(withId(recyclerViewId))
                .perform(actionOnHolderItem(withContactEmailInManageAddressesView(withEmail), click()))

        fun checkDoesNotContainContact(@IdRes recyclerViewId: Int, name: String, email: String):
            ViewInteraction = onView(withId(recyclerViewId)).perform(checkContactDoesNotExist(name, email))

        fun clickContactsGroupItem(@IdRes recyclerViewId: Int, withName: String): ViewInteraction =
            onView(withId(recyclerViewId)).perform(actionOnHolderItem(withContactGroupName(withName), click()))
    }

    class ManageAccounts {

        fun clickAccountManagerViewItem(
            @IdRes recyclerViewId: Int,
            email: String,
            @IdRes childViewId: Int
        ): ViewInteraction = onView(withId(recyclerViewId))
            .perform(actionOnHolderItem(withAccountEmailInAccountManager(email), clickOnChildWithId(childViewId)))
    }

    class Messages {

        fun checkDoesNotContainMessage(@IdRes recyclerViewId: Int, subject: String, date: String):
            ViewInteraction = onView(withId(recyclerViewId)).perform(checkMessageDoesNotExist(subject, date))

        fun saveMessageSubjectAtPosition(
            @IdRes recyclerViewId: Int,
            position: Int,
            method: (String, String) -> Unit
        ): ViewInteraction = onView(withId(recyclerViewId)).perform(saveMessageSubject(position, method))
    }
}
