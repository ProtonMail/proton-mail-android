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

import androidx.appcompat.widget.AppCompatImageButton
import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import me.proton.core.test.android.instrumented.CoreRobot

/**
 * [ContactDetailsRobot] class contains actions and verifications for Contacts functionality.
 */
open class ContactDetailsRobot : CoreRobot {

    fun deleteContact(): ContactsRobot {
        delete()
            .confirmDeletion()
        return ContactsRobot()
    }

    fun editContact(): AddContactRobot {
        view.withId(R.id.editContactDetails).wait().click()
        return AddContactRobot()
    }

    fun navigateUp(): ContactsRobot {
        view
            .instanceOf(AppCompatImageButton::class.java)
            .isDescendantOf(view.withId(R.id.animToolbar))
            .wait().click()
        return ContactsRobot()
    }

    private fun delete(): ContactDetailsRobot {
        view.withId(R.id.action_delete).wait().checkDisplayed().click()
        return this
    }

    private fun confirmDeletion() {
        UIActions.system.clickPositiveDialogButton()
    }

    /**
     * Contains all the validations that can be performed by [ContactDetailsRobot].
     */
    class Verify {}

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
