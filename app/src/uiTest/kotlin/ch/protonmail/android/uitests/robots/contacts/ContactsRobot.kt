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

import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactGroupNameAndMembersCount
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactNameAndEmail
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import ch.protonmail.android.uitests.testsHelper.uiactions.click
import com.github.clans.fab.FloatingActionButton
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf

/**
 * [ContactsRobot] class contains actions and verifications for Contacts functionality.
 */
class ContactsRobot {

    fun addContact(): AddContactRobot {
        UIActions.allOf.clickMatchedView(
            allOf(
                instanceOf(FloatingActionButton::class.java),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
            )
        )
        UIActions.id.clickViewWithId(R.id.addContactItem)
        return AddContactRobot()
    }

    fun addGroup(): AddContactGroupRobot {
        UIActions.allOf.clickMatchedView(
            allOf(
                instanceOf(FloatingActionButton::class.java),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
            )
        )
        UIActions.id.clickViewWithId(R.id.addContactGroupItem)
        return AddContactGroupRobot()
    }


    fun openOptionsMenu(): ContactsMoreOptions {
        UIActions.system.waitForMoreOptionsButton().click()
        return ContactsMoreOptions()
    }

    fun groupsView(): ContactsGroupView {
        UIActions.wait.forViewWithContentDescription(R.string.groups).click()
        return ContactsGroupView()
    }

    fun contactsView(): ContactsView {
        UIActions.wait.forViewWithContentDescription(R.string.contacts).click()
        return ContactsView()
    }

    fun navigateUpToInbox(): InboxRobot {
        UIActions.wait.forViewWithId(contactsRecyclerView)
        UIActions.system.clickHamburgerOrUpButton()
        return InboxRobot()
    }

    fun clickContactByEmail(email: String): ContactDetailsRobot {
        UIActions.wait.forViewWithId(contactsRecyclerView)
        UIActions.recyclerView
            .common.waitForBeingPopulated(contactsRecyclerView)
            .contacts.clickContactItemWithRetry(contactsRecyclerView, email)
        return ContactDetailsRobot()
    }

    inner class ContactsView {

        fun clickContact(withEmail: String): ContactDetailsRobot {
            UIActions.recyclerView.contacts.clickContactItem(contactsRecyclerView, withEmail)
            return ContactDetailsRobot()
        }

        fun navigateUpToInbox(): InboxRobot {
            UIActions.wait.forViewWithId(contactsRecyclerView)
            UIActions.system.clickHamburgerOrUpButton()
            return InboxRobot()
        }

        fun clickSendMessageToContact(contactName: String): ComposerRobot {
            UIActions.recyclerView
                .common.waitForBeingPopulated(contactsRecyclerView)
                .contacts.clickContactItemView(
                    contactsRecyclerView,
                    contactName,
                    R.id.writeButton
                )
            return ComposerRobot()
        }
    }

    class ContactsGroupView {

        fun navigateUpToInbox(): InboxRobot {
            UIActions.wait.forViewWithId(contactGroupsRecyclerView)
            UIActions.system.clickHamburgerOrUpButton()
            return InboxRobot()
        }

        fun clickGroup(withName: String): GroupDetailsRobot {
            UIActions.recyclerView
                .common.waitForBeingPopulated(R.id.contactGroupsRecyclerView)
                .contacts.clickContactsGroupItem(R.id.contactGroupsRecyclerView, withName)
            return GroupDetailsRobot()
        }

        fun clickGroupWithMembersCount(name: String, membersCount: String): GroupDetailsRobot {
            UIActions.wait.forViewWithId(contactGroupsRecyclerView)
            UIActions.recyclerView
                .common.waitForBeingPopulated(contactGroupsRecyclerView)
                .common.scrollToRecyclerViewMatchedItem(
                    contactGroupsRecyclerView,
                    withContactGroupNameAndMembersCount(name, membersCount))
                .perform(click())
            return GroupDetailsRobot()
        }

        fun clickSendMessageToGroup(groupName: String): ComposerRobot {
            UIActions.recyclerView.contacts.clickContactsGroupItemView(
                R.id.contactGroupsRecyclerView,
                groupName,
                R.id.writeButton)
            return ComposerRobot()
        }

        class Verify {
            fun groupWithMembersCountExists(name: String, membersCount: String) {
                UIActions.recyclerView
                    .common.waitForBeingPopulated(contactGroupsRecyclerView)
                    .common.scrollToRecyclerViewMatchedItem(
                        contactGroupsRecyclerView,
                        withContactGroupNameAndMembersCount(name, membersCount)
                    )
            }

            fun groupDoesNotExists(name: String, membersCount: String) {
                UIActions.recyclerView
                    .common.waitForBeingPopulated(contactGroupsRecyclerView)
                    .common.scrollToRecyclerViewMatchedItem(
                        contactGroupsRecyclerView,
                        withContactGroupNameAndMembersCount(name, membersCount)
                    )
            }
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class ContactsMoreOptions {

        fun refresh(): ContactsRobot {
            UIActions.allOf.clickViewWithIdAndText(R.id.title, "Refresh")
            return ContactsRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [ContactsRobot].
     */
    class Verify {

        fun contactsOpened() {
            UIActions.check.viewWithIdIsDisplayed(contactsRecyclerView)
        }

        fun contactExists(name: String, email: String) {
            UIActions.recyclerView
                .common.scrollToRecyclerViewMatchedItem(contactsRecyclerView, withContactNameAndEmail(name, email))
        }

        fun contactDoesNotExists(name: String, email: String) {
            UIActions.wait.forViewWithId(contactsRecyclerView)
            UIActions.recyclerView
                .contacts.checkDoesNotContainContact(contactsRecyclerView, name, email)
        }

        fun contactsRefreshed() {
            UIActions.wait.untilViewWithIdDisabled(R.id.progress_bar)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {
        const val contactsRecyclerView = R.id.contactsRecyclerView
        const val contactGroupsRecyclerView = R.id.contactGroupsRecyclerView
    }
}
