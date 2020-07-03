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

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.composer.ComposerRobot
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.UIActions
import java.util.*

/**
 * [ContactsRobot] class contains actions and verifications for Contacts functionality.
 */
open class ContactsRobot : UIActions() {

    fun openOptionsMenu(): ContactsRobot {
        clickOnObjectWithContentDescription("More options")
        return this
    }

    fun refresh(): ContactsRobot {
        clickOnObjectWithIdAndText(R.id.title, "Refresh")
        return this
    }

    fun addFab(): ContactsRobot {
        clickChildInViewGroup(R.id.addFab, 2)
        return this
    }

    fun addContact(): ContactsRobot {
        clickOnObjectWithId(R.id.addContactItem)
        return this
    }

    fun insertContactName(nameNewContact: String?): ContactsRobot {
        waitUntilObjectWithIdAppearsInView(R.id.contact_display_name)
        insertTextIntoFieldWithId(R.id.contact_display_name, nameNewContact)
        return this
    }

    fun saveContact(): ContactsRobot {
        clickOnObjectWithIdAndText(R.id.action_save, R.string.save)
        return this
    }

    fun addContactGroup(): ContactsRobot {
        clickOnObjectWithId(R.id.addContactGroupItem)
        return this
    }

    fun chooseContact(): ContactsRobot {
        clickChildInRecyclerView(R.id.contactsRecyclerView, 1)
        return this
    }

    fun contactDetails(): ContactsRobot {
        waitUntilObjectWithIdAppearsInView(R.id.add_contact_details)
        clickOnObjectWithId(R.id.editContactDetails)
        return this
    }

    fun editDisplayName(displayName: String?): ContactsRobot {
        insertTextIntoFieldWithId(R.id.contact_display_name, displayName)
        return this
    }

    fun editEmailAddress(emailAddress: String?): ContactsRobot {
        insertTextIntoFieldWithHint(R.string.contact_vcard_hint_email, emailAddress)
        return this
    }

    val contactName: String?
        get() = getTextFromObject(R.id.contactTitle)

    fun delete(): ContactsRobot {
        waitWithTimeoutForObjectWithIdIsClickable(R.id.action_delete, 5000)
        clickOnObjectWithId(R.id.action_delete)
        return this
    }

    fun confirmDelete(): ContactsRobot {
        waitWithTimeoutForObjectWithTextToAppear("YES", 5000)
        clickOnObjectWithText("YES")
        return this
    }

    fun chooseContactWithText(emailContainsText: String?): ContactsRobot {
        scrollDownAndClickObjectWithIdAndTextIsFound(R.id.contactsRecyclerView, R.id.contact_email, emailContainsText)
        return this
    }

    fun composeButton(): ContactsRobot {
        waitUntilObjectWithIdAppearsInView(R.id.add_contact_details)
        clickOnObjectWithId(R.id.btnCompose)
        return this
    }

    fun composeMessage(composerData: TestData): ContactsRobot {
        waitUntilObjectWithIdAppearsInView(R.id.message_title)
        clickOnObjectWithId(R.id.message_title)
        typeTextIntoField(R.id.message_title, composerData.messageSubject)
        insertTextIntoFieldWithId(R.id.message_body, composerData.messageBody)
        return this
    }

    internal fun send(): ComposerRobot {
        clickOnObjectWithId(R.id.send_message)
        return ComposerRobot()
    }

    fun editGroupName(newGroupName: String?): ContactsRobot {
        insertTextIntoFieldWithId(R.id.contactGroupName, newGroupName)
        return this
    }

    fun saveGroup(): ContactsRobot {
        clickOnObjectWithIdAndText(R.id.action_save, "DONE")
        return this
    }

    fun groupsView(): ContactsRobot {
        clickOnObjectWithContentDescSubstring("Groups")
        return this
    }

    fun chooseGroup(): ContactsRobot {
        waitWithTimeoutForObjectWithIdToAppear(R.id.contact_data_parent, 10000)
        clickChildInRecyclerView(R.id.contactGroupsRecyclerView, 0)
        return this
    }

    fun editFab(): ContactsRobot {
        clickOnObjectWithId(R.id.editFab)
        return this
    }

    fun manageAddresses(): ContactsRobot {
        val randomPosition = Random().nextInt(3)
        clickOnObjectWithIdAndText(R.id.manageAddresses, R.string.contact_groups_manage_addresses)
        waitUntilObjectWithIdAppearsInView(R.id.contactEmailsRecyclerView)
        clickChildInRecyclerView(R.id.contactEmailsRecyclerView, randomPosition)
        clickChildInRecyclerView(R.id.contactEmailsRecyclerView, randomPosition + 1)
        return this
    }

    fun composeToGroup(): ContactsRobot {
        waitWithTimeoutForObjectWithIdToAppear(R.id.contact_data_parent, 10000)
        clickComposeToGroupButtonAtPosition(R.id.contactGroupsRecyclerView, R.id.writeButton, 0)
        return this
    }

    val groupName: String?
        get() = getTextFromObjectInRecyclerViewAtPosition(R.id.contactGroupsRecyclerView, R.id.contact_name, 0)


    /**
     * Contains all the validations that can be performed by [ContactsRobot].
     */
    class Verify : ContactsRobot() {

        fun contactSaved(): ContactsRobot {
            checkIfToastMessageIsDisplayed(R.string.contact_saved)
            return ContactsRobot()
        }

        fun groupSaved(): ContactsRobot {
            checkIfToastMessageIsDisplayed(R.string.contact_group_saved)
            return ContactsRobot()
        }

        fun contactsRefreshed(): ContactsRobot {
            checkIfToastMessageIsDisplayed(R.string.fetching_contacts_success)
            return ContactsRobot()
        }

        fun contactDeleted(deletedContact: String?): ContactsRobot {
            waitUntilObjectWithIdAppearsInView(R.id.contactsRecyclerView)
            checkIfObjectWithIdAndTextIsNotDisplayed(R.id.contact_name, deletedContact)
            return ContactsRobot()
        }

        fun groupDeleted(deletedGroup: String?): ContactsRobot {
            waitUntilObjectWithIdAppearsInView(R.id.contactGroupsRecyclerView)
            checkIfToastMessageIsDisplayed("Group Deleted")
            checkIfObjectWithIdAndTextIsNotDisplayed(R.id.contact_name, deletedGroup)
            return ContactsRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block) as ContactsRobot
}