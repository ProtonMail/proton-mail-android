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
import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import me.proton.core.test.android.instrumented.CoreRobot

/**
 * [AddContactRobot] class contains actions and verifications for Add/Edit Contacts.
 */
class AddContactRobot : CoreRobot {

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
        view.withId(R.id.contact_display_name).replaceText(name)
        return this
    }

    private fun email(email: String): AddContactRobot {
        view
            .withId(R.id.option)
            .withVisibility(ViewMatchers.Visibility.VISIBLE)
            .isDescendantOf(view.withId(R.id.emailAddressesContainer))
            .replaceText(email)
        return this
    }

    private fun save(): ContactsRobot {
        view.withId(R.id.action_save).click()
        UIActions.wait.forToastWithText(R.string.contact_saved)
        return ContactsRobot()
    }
}
