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
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions

/**
 * [AddContactRobot] class contains actions and verifications for Add/Edit Contacts.
 */
class AddContactRobot {

    fun setNameEmailAndSave(name: String, email: String): ContactsRobot {
        return displayName(name)
            .email(email)
            .save()
    }

    fun editNameEmailAndSave(name: String, email: String): ContactDetailsRobot {
        displayName(name)
            .email(email)
            .save()
        return ContactDetailsRobot()
    }

    private fun displayName(name: String): AddContactRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.contact_display_name, name)
        return this
    }

    private fun email(email: String): AddContactRobot {
        UIActions.allOf.setTextIntoFieldByIdAndParent(R.id.option, R.id.emailAddressesContainer, email)
        return this
    }

    private fun save(): ContactsRobot {
        UIActions.id.clickViewWithId(R.id.action_save)
        return ContactsRobot()
    }
}