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
package ch.protonmail.android.uitests.robots.contacts

import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import ch.protonmail.android.R
import ch.protonmail.android.views.contactsList.ContactListItemView
import kotlinx.android.synthetic.main.contacts_list_item.view.*
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Matchers that are used by Contacts features.
 */
object ContactsMatchers {

    /**
     * Matches the Mailbox message represented by [ContactListItemView.ContactView] by contact name.
     * Subject must be unique in a list in order to use this matcher.
     *
     * @param email - contact name
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
                return if (item.itemView is ContactListItemView.ContactView) {
                    val contactItem = item.itemView as ContactListItemView.ContactView
                    val actualEmail = contactItem.contact_subtitle.text.toString()
                    contactsList.add(actualEmail)
                    actualEmail == email
                } else {
                    false
                }
            }
        }
    }

    /**
     * Matches the Mailbox message represented by [ContactListItemView.ContactView] by contact name.
     * Subject must be unique in a list in order to use this matcher.
     *
     * @param email - contact name
     */
    fun withContactNameAndEmail(name: String, email: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            RecyclerView.ViewHolder>(RecyclerView.ViewHolder::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("Contact item with email: \"$email\"")
            }

            override fun matchesSafely(item: RecyclerView.ViewHolder): Boolean {
                return if (item.itemView is ContactListItemView.ContactView) {
                    val contactItem = item.itemView as ContactListItemView.ContactView
                    contactItem.contact_subtitle.text.toString() == email &&
                        contactItem.contact_name.text.toString() == name
                } else {
                    false
                }
            }
        }
    }

    /**
     * Matches the Contact group Item by its name.
     * Subject must be unique in a list in order to use this matcher.
     *
     * @param name - contact group name
     */
    fun withContactGroupName(name: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            RecyclerView.ViewHolder>(RecyclerView.ViewHolder::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("With contact group name: \"$name\"")
            }

            override fun matchesSafely(item: RecyclerView.ViewHolder): Boolean {
                return item.itemView
                    .findViewById<LinearLayout>(R.id.contact_data_parent)
                    .findViewById<LinearLayout>(R.id.contact_data)
                    .findViewById<TextView>(R.id.contact_name).text.toString() == name
            }
        }
    }

    /**
     * Matches the Contact group Item by its name.
     * Subject must be unique in a list in order to use this matcher.
     *
     * @param name - contact group name
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
                val contactDataParent = item.itemView
                    .findViewById<LinearLayout>(R.id.contact_data_parent)
                val groupName = contactDataParent
                    .findViewById<LinearLayout>(R.id.contact_data)
                    .findViewById<TextView>(R.id.contact_name).text.toString()
                val groupMembersCount = contactDataParent
                    .findViewById<TextView>(R.id.contact_subtitle).text.toString()
                contactGroupsList.add(groupName)
                return groupName == name && groupMembersCount == membersCount
            }
        }
    }

    /**
     * Matches the Contact group Item by its name.
     * Subject must be unique in a list in order to use this matcher.
     *
     * @param name - contact group name
     */
    fun sendActionForGroup(name: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            RecyclerView.ViewHolder>(RecyclerView.ViewHolder::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("Send action for group with name: \"$name\"")
            }

            override fun matchesSafely(item: RecyclerView.ViewHolder): Boolean {
                return item.itemView
                    .findViewById<LinearLayout>(R.id.contact_data_parent)
                    .findViewById<LinearLayout>(R.id.contact_data)
                    .findViewById<TextView>(R.id.contact_name).text.toString() == name
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
