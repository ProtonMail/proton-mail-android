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
import me.proton.core.test.android.instrumented.CoreRobot

/**
 * [GroupDetailsRobot] class contains actions and verifications for Contacts functionality.
 */
open class GroupDetailsRobot : CoreRobot {

    fun edit(): AddContactGroupRobot {
        view.withId(R.id.editFab).click()
        return AddContactGroupRobot()
    }

    fun deleteGroup(): ContactsRobot {
        return delete()
            .confirmDeletion()
    }

    fun navigateUp(): ContactsRobot {
        view.withId(R.id.editFab).wait().checkDisplayed()
        UIActions.system.clickHamburgerOrUpButtonInAnimatedToolbar()
        return ContactsRobot()
    }

    private fun delete(): GroupDetailsRobot {
        view.withId(R.id.action_delete).wait().checkDisplayed().click()
        return this
    }

    private fun confirmDeletion(): ContactsRobot {
        view.withId(android.R.id.button1).wait().checkDisplayed().click()
        return ContactsRobot()
    }

    /**
     * Contains all the validations that can be performed by [GroupDetailsRobot].
     */
    class Verify

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
