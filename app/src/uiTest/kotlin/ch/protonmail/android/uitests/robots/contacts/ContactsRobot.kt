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
import ch.protonmail.android.uitests.testsHelper.UIActions
import ch.protonmail.android.uitests.testsHelper.insert
import com.github.clans.fab.FloatingActionButton
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf

/**
 * [ContactsRobot] class contains actions and verifications for Contacts functionality.
 */
open class ContactsRobot {

    fun openOptionsMenu(): ContactsRobot {
        UIActions.contentDescription.clickViewWithContentDescription("More options")
        return this
    }

    fun refresh(): ContactsRobot {
        UIActions.allOf.clickViewWithIdAndText(R.id.title, "Refresh")
        return this
    }

    fun addFab(): ContactsRobot {
        UIActions.allOf.clickMatchedView(allOf(
            instanceOf(FloatingActionButton::class.java),
            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        ))
        return this
    }

    fun addContact(): ContactsRobot {
        UIActions.id.clickViewWithId(R.id.addContactItem)
        return this
    }

    fun insertContactName(nameNewContact: String): ContactsRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.contact_display_name).insert(nameNewContact)
        return this
    }

    fun saveContact(): ContactsRobot {
        UIActions.allOf.clickViewWithIdAndText(R.id.action_save, R.string.save)
        return this
    }

    fun addContactGroup(): ContactsRobot {
        UIActions.id.clickViewWithId(R.id.addContactGroupItem)
        return this
    }

    fun chooseContact(withName: String): ContactsRobot {
        UIActions.recyclerView.clickOnContactItem(R.id.contactsRecyclerView, withName)
        return this
    }

    fun contactDetails(): ContactsRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.add_contact_details)
        UIActions.id.clickViewWithId(R.id.editContactDetails)
        return this
    }

    fun editDisplayName(displayName: String): ContactsRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.contact_display_name, displayName)
        return this
    }

    fun editEmailAddress(emailAddress: String): ContactsRobot {
        UIActions.hint.insertTextIntoFieldWithHint(R.string.contact_vcard_hint_email, emailAddress)
        return this
    }

    fun delete(): ContactsRobot {
        UIActions.id.clickViewWithId(R.id.action_delete)
        return this
    }

    fun confirmDelete(): ContactsRobot {
        UIActions.wait.untilViewWithTextAppears("YES")
        UIActions.text.clickViewWithText("YES")
        return this
    }

    fun chooseContactWithText(emailContainsText: String?): ContactsRobot {
        //TODO implement recyclerview matcher
        //scrollDownAndClickObjectWithIdAndTextIsFound(R.id.contactsRecyclerView, R.id.contact_email, emailContainsText)
        return this
    }

    fun composeButton(): ContactsRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.add_contact_details)
        UIActions.id.clickViewWithId(R.id.btnCompose)
        return this
    }

    fun composeMessage(messageSubject: String, messageBody: String): ContactsRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.message_title)
        UIActions.id.clickViewWithId(R.id.message_title)
        UIActions.id.typeTextIntoFieldWithId(R.id.message_title, messageSubject)
        UIActions.id.insertTextIntoFieldWithId(R.id.message_body, messageBody)
        return this
    }

    internal fun send(): ComposerRobot {
        UIActions.id.clickViewWithId(R.id.send_message)
        return ComposerRobot()
    }

    fun editGroupName(newGroupName: String): ContactsRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.contactGroupName, newGroupName)
        return this
    }

    fun saveGroup(): ContactsRobot {
        UIActions.allOf.clickViewWithIdAndText(R.id.action_save, "DONE")
        return this
    }

    fun groupsView(): ContactsRobot {
        UIActions.contentDescription.clickViewWithContentDescSubstring("Groups")
        return this
    }

    fun chooseGroup(withName: String): ContactsRobot {
        UIActions.recyclerView.clickOnGroupItem(R.id.contactGroupsRecyclerView, withName)
        return this
    }

    fun editFab(): ContactsRobot {
        UIActions.id.clickViewWithId(R.id.editFab)
        return this
    }

    fun manageAddresses(): ContactsRobot {
        UIActions.allOf.clickViewWithIdAndText(R.id.manageAddresses, R.string.contact_groups_manage_addresses)
        UIActions.wait.untilViewWithIdAppears(R.id.contactEmailsRecyclerView)
        return this
    }

    fun composeToGroup(): ContactsRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.contact_data_parent)
        //TODO
//        clickComposeToGroupButtonAtPosition(R.id.contactGroupsRecyclerView, R.id.writeButton, 0)
        return this
    }

    /**
     * Contains all the validations that can be performed by [ContactsRobot].
     */
    class Verify {

        fun contactSaved(): ContactsRobot {
            UIActions.check.toastMessageIsDisplayed(R.string.contact_saved)
            return ContactsRobot()
        }

        fun groupSaved(): ContactsRobot {
            UIActions.check.toastMessageIsDisplayed(R.string.contact_group_saved)
            return ContactsRobot()
        }

        fun contactsRefreshed(): ContactsRobot {
            UIActions.check.toastMessageIsDisplayed(R.string.fetching_contacts_success)
            return ContactsRobot()
        }

        fun contactDeleted(deletedContact: String): ContactsRobot {
            UIActions.wait.untilViewWithIdAppears(R.id.contactsRecyclerView)
            UIActions.check.viewWithIdAndTextIsNotDisplayed(R.id.contact_name, deletedContact)
            return ContactsRobot()
        }

        fun groupDeleted(deletedGroup: String): ContactsRobot {
            UIActions.wait.untilViewWithIdAppears(R.id.contactGroupsRecyclerView)
            UIActions.check.toastMessageIsDisplayed("Group Deleted")
            UIActions.check.viewWithIdAndTextDoesNotExist(R.id.contact_name, deletedGroup)
            return ContactsRobot()
        }

        fun contactsOpened() {
            UIActions.check.viewWithIdIsDisplayed(R.id.contactsRecyclerView)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
