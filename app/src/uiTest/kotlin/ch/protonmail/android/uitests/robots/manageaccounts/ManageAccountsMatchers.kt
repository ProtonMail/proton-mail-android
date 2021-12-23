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
package ch.protonmail.android.uitests.robots.manageaccounts

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Matchers that are used by Manage accounts functionality.
 */
object ManageAccountsMatchers {

    /**
     * Matches account at position with specific viewMatcher represented by [RecyclerView.ViewHolder].
     * @param position Expected account position
     * @param itemMatcher ViewMatcher
     */
    fun withViewAtPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?>? {
        return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("Account position and ViewMatcher in accounts panel")
            }

            override fun matchesSafely(recyclerView: RecyclerView): Boolean {
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                return viewHolder != null && itemMatcher.matches(viewHolder.itemView)
            }
        }
    }
}

/**
 * Matches the logged out Account with specific email represented by [AccountsAdapter.ViewHolder].
 * @param expectedName - account email
 */
//    fun withLoggedOutAccountNameInDrawer(expectedName: String): Matcher<RecyclerView.ViewHolder> {
//        return object : BoundedMatcher<RecyclerView.ViewHolder,
//            AccountsAdapter.ViewHolder<*>>(AccountsAdapter.ViewHolder::class.java) {
//
//            override fun describeTo(description: Description) {
//                description.appendText("With logged out user account email in menu drawer account manager.")
//            }
//
//            override fun matchesSafely(item: AccountsAdapter.ViewHolder<*>): Boolean {
//                val drawerUserItem = item.itemView.drawerUserItem
//                return if (drawerUserItem != null) {
//                    val name = drawerUserItem.userDetailsParent.userName.text.toString()
//                    val signInButtonVisible =
//                        drawerUserItem.userLoginStatusParent.userSignIn.visibility == View.VISIBLE
//                    name == expectedName && signInButtonVisible
//                } else {
//                    false
//                }
//            }
//        }
//    }

/**
 * Matches primary Account with specific username represented by [AccountsAdapter.ViewHolder].
 * @param name - account name
 */
//    fun withPrimaryAccountInAccountManager(name: String): Matcher<RecyclerView.ViewHolder> {
//        return object : BoundedMatcher<RecyclerView.ViewHolder,
//            AccountsAdapter.ViewHolder<*>>(AccountsAdapter.ViewHolder::class.java) {
//
//            override fun describeTo(description: Description) {
//                description.appendText("With primary account in menu drawer account manager.")
//            }
//
//            override fun matchesSafely(item: AccountsAdapter.ViewHolder<*>): Boolean {
//                val accountItem = item.itemView.accItem
//                return if (accountItem != null) {
//                    val accountName = accountItem.userDetailsParent.accUserName.text.toString()
//                    return name == accountName
//                } else {
//                    false
//                }
//            }
//        }
//    }
// }
