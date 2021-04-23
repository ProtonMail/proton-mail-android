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

import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.test.espresso.matcher.ViewMatchers
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactEmail
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactGroupName
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactGroupNameAndMembersCount
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactNameAndEmail
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import com.github.clans.fab.FloatingActionButton
import me.proton.core.test.android.instrumented.CoreRobot
import me.proton.core.test.android.instrumented.utils.StringUtils.stringFromResource
import org.hamcrest.CoreMatchers.containsString

/**
 * [ContactsRobot] class contains actions and verifications for Contacts functionality.
 */
class ContactsRobot : CoreRobot {

    fun addContact(): AddContactRobot {
        view.instanceOf(FloatingActionButton::class.java)
            .withVisibility(ViewMatchers.Visibility.VISIBLE)
            .click()
        view.withId(R.id.addContactItem).click()
        return AddContactRobot()
    }

    fun addGroup(): AddContactGroupRobot {
        view.instanceOf(FloatingActionButton::class.java)
            .withVisibility(ViewMatchers.Visibility.VISIBLE)
            .click()
        view.withId(R.id.addContactGroupItem).click()
        return AddContactGroupRobot()
    }

    fun openOptionsMenu(): ContactsMoreOptions {
        view.instanceOf(AppCompatImageView::class.java).withParent(view.instanceOf(ActionMenuView::class.java)).click()
        return ContactsMoreOptions()
    }

    fun groupsView(): ContactsGroupView {
        view.withContentDesc(containsString(stringFromResource(R.string.groups))).click()
        return ContactsGroupView()
    }

    fun contactsView(): ContactsView {
        view.withContentDesc(containsString(stringFromResource(R.string.contacts))).click()
        return ContactsView()
    }

    fun navigateUpToInbox(): InboxRobot {
        view.withId(contactsRecyclerView).wait().checkDisplayed()
        view
            .instanceOf(AppCompatImageButton::class.java)
            .isDescendantOf(view.withId(R.id.toolbar))
            .click()
        return InboxRobot()
    }

    fun clickContactByEmail(email: String): ContactDetailsRobot {
        recyclerView
            .withId(contactsRecyclerView)
            .waitUntilPopulated()
            .onHolderItem(withContactEmail(email))
            .click()
        return ContactDetailsRobot()
    }

    inner class ContactsView {

        fun clickSendMessageToContact(contactEmail: String): ComposerRobot {
            recyclerView
                .withId(contactsRecyclerView)
                .waitUntilPopulated()
                .onHolderItem(withContactEmail(contactEmail))
                .onItemChildView(view.withId(R.id.writeButton))
                .click()
            return ComposerRobot()
        }
    }

    class ContactsGroupView : CoreRobot {

        fun navigateUpToInbox(): InboxRobot {
            view.withId(contactGroupsRecyclerView).wait().checkDisplayed()
            view
                .instanceOf(AppCompatImageButton::class.java)
                .isDescendantOf(view.withId(R.id.toolbar))
                .click()
            return InboxRobot()
        }

        fun clickGroup(withName: String): GroupDetailsRobot {
            recyclerView
                .withId(contactGroupsRecyclerView)
                .waitUntilPopulated()
                .onHolderItem(withContactGroupName(withName))
                .click()
            return GroupDetailsRobot()
        }

        fun clickGroupWithMembersCount(name: String, membersCount: String): GroupDetailsRobot {
            recyclerView
                .withId(contactGroupsRecyclerView)
                .waitUntilPopulated()
                .onHolderItem(withContactGroupNameAndMembersCount(name, membersCount))
                .click()
            return GroupDetailsRobot()
        }

        fun clickSendMessageToGroup(groupName: String): ComposerRobot {
            recyclerView
                .withId(contactGroupsRecyclerView)
                .waitUntilPopulated()
                .onHolderItem(withContactGroupName(groupName))
                .onItemChildView(view.withId(R.id.writeButton))
                .click()
            return ComposerRobot()
        }

        class Verify : CoreRobot {

            fun groupWithMembersCountExists(name: String, membersCount: String) {
                recyclerView
                    .withId(contactGroupsRecyclerView)
                    .waitUntilPopulated()
                    .scrollToHolder(withContactGroupNameAndMembersCount(name, membersCount))
            }

            fun groupDoesNotExists(groupName: String, groupMembersCount: String) {
//                TODO Create "checkDoesNotExists" method in [recyclerView] and move to Core lib.
                UIActions.recyclerView
                    .common.waitForBeingPopulated(contactGroupsRecyclerView)
                    .contacts.checkDoesNotContainGroup(contactGroupsRecyclerView, groupName, groupMembersCount)
            }
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class ContactsMoreOptions : CoreRobot {
        fun refresh(): ContactsRobot {
            view.withId(R.id.title).withText(R.string.refresh_contacts).click()
            UIActions.wait.forToastWithText(R.string.fetching_contacts_success)
            return ContactsRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [ContactsRobot].
     */
    class Verify : CoreRobot {

        fun contactsOpened() {
            view.withId(contactsRecyclerView).wait().checkDisplayed()
        }

        fun contactExists(name: String, email: String) {
            recyclerView
                .withId(contactsRecyclerView)
                .waitUntilPopulated()
                .scrollToHolder(withContactNameAndEmail(name, email))
        }

        fun contactDoesNotExists(name: String, email: String) {
//            TODO Create "checkDoesNotExists" method in [recyclerView] and move to Core lib.
            UIActions.wait.forViewWithId(contactsRecyclerView)
            UIActions.recyclerView
                .contacts.checkDoesNotContainContact(contactsRecyclerView, name, email)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {
        const val contactsRecyclerView = R.id.contactsRecyclerView
        const val contactGroupsRecyclerView = R.id.contactGroupsRecyclerView
    }
}
