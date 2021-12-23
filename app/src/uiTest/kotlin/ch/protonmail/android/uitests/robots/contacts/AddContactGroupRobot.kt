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

import android.widget.EditText
import ch.protonmail.android.R
import me.proton.core.test.android.instrumented.Robot

/**
 * [AddContactGroupRobot] class contains actions and verifications for Add/Edit Contact Groups.
 */
class AddContactGroupRobot : Robot {

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
        view.withId(R.id.contactGroupName).instanceOf(EditText::class.java).replaceText(name)
        return this
    }

    fun done(): ContactsRobot {
        view.withId(R.id.action_save).click()
        view.withText(R.string.contact_group_saved).checkDisabled()
        return ContactsRobot()
    }
}
