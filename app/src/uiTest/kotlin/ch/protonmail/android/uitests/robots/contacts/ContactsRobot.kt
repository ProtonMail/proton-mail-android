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

import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactEmail
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactGroupName
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactGroupNameAndMembersCount
import ch.protonmail.android.uitests.robots.contacts.ContactsMatchers.withContactNameAndEmail
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import me.proton.fusion.Fusion
import me.proton.fusion.utils.StringUtils.stringFromResource

/**
 * [ContactsRobot] class contains actions and verifications for Contacts functionality.
 */
class ContactsRobot : Fusion {

    private fun openAddContactsMenu(): ContactsRobot {
        view.withId(R.id.fab_contacts_add_menu).click()
        return this
    }

    fun addContact(): AddContactRobot {
        openAddContactsMenu()
        view.withId(R.id.fab_contacts_add_contact).click()
        return AddContactRobot()
    }

    fun addGroup(): AddContactGroupRobot {
        openAddContactsMenu()
        view.withId(R.id.fab_contacts_add_contact_group).click()
        return AddContactGroupRobot()
    }

    fun openOptionsMenu(): ContactsMoreOptions {
        view.instanceOf(AppCompatImageView::class.java).hasParent(view.instanceOf(ActionMenuView::class.java)).click()
        return ContactsMoreOptions()
    }

    fun groupsView(): ContactsGroupView {
        view.withContentDescContains(stringFromResource(R.string.groups)).click()
        return ContactsGroupView()
    }

    fun contactsView(): ContactsView {
        view.withContentDescContains(stringFromResource(R.string.contacts)).click()
        return ContactsView()
    }

    fun navigateUpToInbox(): InboxRobot {
        view.withId(contactsRecyclerView).waitForDisplayed().checkIsDisplayed()
        view
            .instanceOf(AppCompatImageButton::class.java)
            .isDescendantOf(view.withId(R.id.toolbar_contacts))
            .click()
        return InboxRobot()
    }

    fun clickContactByEmail(email: String): ContactDetailsRobot {
        recyclerView
            .withId(contactsRecyclerView)
            .onHolderItem(withContactEmail(email))
            .click()
        return ContactDetailsRobot()
    }

    inner class ContactsView {

        fun clickSendMessageToContact(contactEmail: String): ComposerRobot {
            recyclerView
                .withId(contactsRecyclerView)
                .onHolderItem(withContactEmail(contactEmail))
                .onItemChildView(view.withId(R.id.image_view_contact_item_send_button))
                .click()
            return ComposerRobot()
        }
    }

    class ContactsGroupView : Fusion {

        fun navigateUpToInbox(): InboxRobot {
            view
                .instanceOf(AppCompatImageButton::class.java)
                .isDescendantOf(view.withId(R.id.toolbar_contacts))
                .click()
            return InboxRobot()
        }

        fun clickGroup(withName: String): GroupDetailsRobot {
            view.withText(withName).waitForDisplayed()
            recyclerView
                .withId(contactGroupsRecyclerView)
                .onHolderItem(withContactGroupName(withName))
                .click()
            return GroupDetailsRobot()
        }

        fun clickGroupWithMembersCount(name: String, membersCount: String): GroupDetailsRobot {
            recyclerView
                .withId(contactGroupsRecyclerView)
                .onHolderItem(withContactGroupNameAndMembersCount(name, membersCount))
                .click()
            return GroupDetailsRobot()
        }

        fun clickSendMessageToGroup(groupName: String): ComposerRobot {
            recyclerView
                .withId(contactGroupsRecyclerView)
                .onHolderItem(withContactGroupName(groupName))
                .onItemChildView(view.withId(R.id.image_view_contact_item_send_button))
                .click()
            return ComposerRobot()
        }

        fun openOptionsMenu(): ContactsGroupView {
            view.instanceOf(AppCompatImageView::class.java).hasParent(view.instanceOf(ActionMenuView::class.java))
                .click()
            return ContactsGroupView()
        }

        fun refreshGroups(): ContactsGroupView {
            view.withId(R.id.title).withText(R.string.refresh_contacts).click()
            return ContactsGroupView()
        }

        class Verify : Fusion {

            fun groupWithMembersCountExists(name: String, membersCount: String) {
                recyclerView
                    .withId(contactGroupsRecyclerView)
//                    .waitUntilPopulated()
                    .scrollToHolder(withContactGroupNameAndMembersCount(name, membersCount))
            }

            fun groupDoesNotExists(groupName: String, groupMembersCount: String) {
                view.withId(R.id.text_view_contact_name).withText(groupName).waitUntilGone().checkDoesNotExist()
            }
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class ContactsMoreOptions : Fusion {

        fun refreshContacts(): ContactsRobot {
            view.withId(R.id.title).withText(R.string.refresh_contacts).click()
            return ContactsRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [ContactsRobot].
     */
    class Verify : Fusion {

        fun contactsOpened() {
            view.withId(contactsRecyclerView).checkIsDisplayed()
        }

        fun contactExists(name: String, email: String) {
            recyclerView
                .withId(contactsRecyclerView)

                .scrollToHolder(withContactNameAndEmail(name, email))
        }

        fun contactDoesNotExists(name: String, email: String) {
            view.withId(R.id.text_view_contact_name).withText(name).waitUntilGone().checkDoesNotExist()
            view.withId(R.id.text_view_contact_subtitle).withText(email).checkDoesNotExist()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {

        const val contactsRecyclerView = R.id.contactsRecyclerView
        const val contactGroupsRecyclerView = R.id.contactGroupsRecyclerView
    }
}
