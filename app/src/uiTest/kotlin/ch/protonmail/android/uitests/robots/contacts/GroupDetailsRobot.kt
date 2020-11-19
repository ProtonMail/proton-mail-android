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
import ch.protonmail.android.uitests.testsHelper.click

/**
 * [GroupDetailsRobot] class contains actions and verifications for Contacts functionality.
 */
open class GroupDetailsRobot {

    fun edit(): AddContactGroupRobot {
        UIActions.id.clickViewWithId(R.id.editFab)
        return AddContactGroupRobot()
    }

    fun deleteGroup(): ContactsRobot {
        return delete()
            .confirmDeletion()
    }

    fun navigateUp(): ContactsRobot {
        UIActions.wait.forViewWithId(R.id.editFab)
        UIActions.system.clickHamburgerOrUpButtonInAnimatedToolbar()
        return ContactsRobot()
    }

    private fun delete(): GroupDetailsRobot {
        UIActions.wait.forViewWithId(R.id.contactEmailsRecyclerView)
        UIActions.wait.forViewWithId(R.id.action_delete).click()
        return this
    }

    private fun confirmDeletion(): ContactsRobot {
        UIActions.wait.forViewWithId(android.R.id.button1).click()
        return ContactsRobot()
    }

    /**
     * Contains all the validations that can be performed by [GroupDetailsRobot].
     */
    class Verify

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
