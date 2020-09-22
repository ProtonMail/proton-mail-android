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

import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.UIActions
import com.github.clans.fab.FloatingActionButton
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf

/**
 * [ContactsRobot] class contains actions and verifications for Contacts functionality.
 */
open class ContactsRobot {

    fun addFab(): ContactsRobot {
        UIActions.allOf.clickMatchedView(allOf(
            instanceOf(FloatingActionButton::class.java),
            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        ))
        return this
    }

    fun openOptionsMenu(): ContactsMoreOptions {
        UIActions.system.clickMoreOptionsButton()
        return ContactsMoreOptions()
    }

    fun groupsView(): ContactsGroupView {
        UIActions.contentDescription.clickViewWithContentDescSubstring("Groups")
        return ContactsGroupView()
    }

    fun contactsView(): ContactsView {
        UIActions.contentDescription.clickViewWithContentDescSubstring("Contacts")
        return ContactsView()
    }

    fun navigateUpToInbox(): InboxRobot {
        UIActions.wait.forViewWithId(R.id.contactsRecyclerView)
        UIActions.system.clickHamburgerOrUpButton()
        return InboxRobot()
    }

    inner class ContactsView {

        fun clickContact(withEmail: String): ContactDetailsRobot {
            UIActions.recyclerView.clickContactItem(R.id.contactsRecyclerView, withEmail)
            return ContactDetailsRobot()
        }

        fun clickSendMessageToContact(contactName: String): ComposerRobot {
            UIActions.recyclerView.waitForBeingPopulated(R.id.contactsRecyclerView)
            UIActions.recyclerView.clickContactItemView(
                R.id.contactsRecyclerView,
                contactName,
                R.id.writeButton
            )
            return ComposerRobot()
        }
    }

    inner class ContactsGroupView {

        fun clickGroup(withName: String): GroupDetailsRobot {
            UIActions.recyclerView.clickContactsGroupItem(R.id.contactGroupsRecyclerView, withName)
            return GroupDetailsRobot()
        }

        fun clickSendMessageToGroup(groupName: String): ComposerRobot {
            UIActions.recyclerView.clickContactsGroupItemView(
                R.id.contactGroupsRecyclerView,
                groupName,
                R.id.writeButton)
            return ComposerRobot()
        }
    }

    inner class ContactsMoreOptions {

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
            UIActions.check.viewWithIdIsDisplayed(R.id.contactsRecyclerView)
        }

        fun contactsRefreshed() {
            UIActions.wait.forViewWithText(R.string.fetching_contacts)
            UIActions.wait.untilViewWithIdIsGone(R.id.progress_bar)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
