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
import ch.protonmail.android.uitests.testsHelper.UIActions
import ch.protonmail.android.uitests.testsHelper.insert

/**
 * [AddContactGroupRobot] class contains actions and verifications for Add/Edit Contact Groups.
 */
class AddContactGroupRobot {

    fun editNameAndSave(name: String): GroupDetailsRobot {
        groupName(name)
            .done()
        return GroupDetailsRobot()
    }

    fun groupName(name: String): AddContactGroupRobot {
        UIActions.wait.forViewWithId(R.id.contactGroupName).insert(name)
        return this
    }

    fun manageAddresses(): ManageAddressesRobot {
        UIActions.id.clickViewWithId(R.id.manageAddresses)
        return ManageAddressesRobot()
    }

    fun done(): ContactsRobot {
        UIActions.id.clickViewWithId(R.id.action_save)
        return ContactsRobot()
    }
}
