/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.uitests.robots.contacts

import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import ch.protonmail.android.R
import ch.protonmail.android.contacts.list.listView.ContactsListAdapter
import kotlinx.android.synthetic.main.list_item_contacts.view.*
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Matchers that are used by Contacts features.
 */
object ContactsMatchers {

    /**
     * Matches the contact represented by [ContactsListAdapter] by email address.
     *
     * @param email contact email
     */
    fun withContactEmail(email: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            RecyclerView.ViewHolder>(RecyclerView.ViewHolder::class.java) {

            val contactsList = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("Contact item with email: \"$email\"\n")
                description.appendText("Here is the actual list of contacts:\n")
                contactsList.forEach { description.appendText(" - \"$it\"\n") }
            }

            override fun matchesSafely(item: RecyclerView.ViewHolder): Boolean {
                return if (item.itemView is ConstraintLayout) {
                    val contactItem = item.itemView as ConstraintLayout
                    val actualEmail = contactItem.text_view_contact_subtitle.text.toString()
                    contactsList.add(actualEmail)
                    actualEmail == email
                } else {
                    false
                }
            }
        }
    }

    /**
     * Matches the contact represented by [ContactsListAdapter] by contact name and email address.
     *
     * @param name contact name
     * @param email contact email
     */
    fun withContactNameAndEmail(name: String, email: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            RecyclerView.ViewHolder>(RecyclerView.ViewHolder::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("Contact item with email: \"$email\"")
            }

            override fun matchesSafely(item: RecyclerView.ViewHolder): Boolean {
                return if (item.itemView is ConstraintLayout) {
                    val contactItem = item.itemView as ConstraintLayout
                    contactItem.text_view_contact_subtitle.text.toString() == email &&
                        contactItem.text_view_contact_name.text.toString() == name
                } else {
                    false
                }
            }
        }
    }

    /**
     * Matches the Contact group item by its name.
     *
     * @param name contact group name
     */
    fun withContactGroupName(name: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            RecyclerView.ViewHolder>(RecyclerView.ViewHolder::class.java) {

            val contactsList = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("With contact group name: \"$name\"")
                contactsList.forEach { description.appendText(" - \"$it\"\n") }
            }

            override fun matchesSafely(item: RecyclerView.ViewHolder): Boolean {
                return if (item.itemView is ConstraintLayout) {
                    val groupItem = item.itemView as ConstraintLayout
                    val groupName = groupItem.text_view_contact_name.text.toString()
                    contactsList.add(groupName)
                    groupName == name
                } else {
                    false
                }
            }
        }
    }

    /**
     * Matches the Contact group item by its name and members count.
     *
     * @param name contact group name
     * @param membersCount group members count
     */
    fun withContactGroupNameAndMembersCount(name: String, membersCount: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            RecyclerView.ViewHolder>(RecyclerView.ViewHolder::class.java) {

            val contactGroupsList = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("With contact group name: \"$name\"")
                description.appendText("Here is the actual list of groups:\n")
                contactGroupsList.forEach { description.appendText(" - \"$it\"\n") }
            }

            override fun matchesSafely(item: RecyclerView.ViewHolder): Boolean {
                val groupName = item.itemView.findViewById<TextView>(R.id.text_view_contact_name).text.toString()
                val groupMembersCount =
                    item.itemView.findViewById<TextView>(R.id.text_view_contact_subtitle).text.toString()
                contactGroupsList.add(groupName)
                return groupName == name && groupMembersCount == membersCount
            }
        }
    }

    /**
     * Matches the Contact Item in Manage Addresses RecyclerView by its email.
     *
     * @param email - contact email
     */
    fun withContactEmailInManageAddressesView(email: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            RecyclerView.ViewHolder>(RecyclerView.ViewHolder::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("Contact item that with email: \"$email\"")
            }

            override fun matchesSafely(item: RecyclerView.ViewHolder): Boolean {
                return item.itemView.findViewById<TextView>(R.id.email).text.toString() == email
            }
        }
    }
}
