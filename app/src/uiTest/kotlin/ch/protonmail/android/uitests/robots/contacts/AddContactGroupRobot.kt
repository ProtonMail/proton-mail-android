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

import android.widget.EditText
import ch.protonmail.android.R
import me.proton.fusion.Fusion

/**
 * [AddContactGroupRobot] class contains actions and verifications for Add/Edit Contact Groups.
 */
class AddContactGroupRobot : Fusion {

    fun editNameAndSave(name: String): GroupDetailsRobot {
        groupName(name)
            .done()
        return GroupDetailsRobot()
    }

    fun manageAddresses(): ManageAddressesRobot {
        view.withId(R.id.manageMembers).click()
        return ManageAddressesRobot()
    }

    fun groupName(name: String): AddContactGroupRobot {
        view
            .withId(R.id.input)
            .isDescendantOf(
                view.withId(R.id.contactGroupName)
            )
            .replaceText(name)
        return this
    }

    fun done(): ContactsRobot {
        view.withId(R.id.action_save).click()
        return ContactsRobot()
    }
}
