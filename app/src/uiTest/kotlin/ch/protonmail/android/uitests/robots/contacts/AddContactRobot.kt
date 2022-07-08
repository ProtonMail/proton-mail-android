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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import ch.protonmail.android.R
import me.proton.fusion.Fusion
import me.proton.fusion.utils.ActivityProvider
import me.proton.fusion.utils.StringUtils.stringFromResource
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.not

/**
 * [AddContactRobot] class contains actions and verifications for Add/Edit Contacts.
 */
class AddContactRobot : Fusion {

    fun setNameEmailAndSave(name: String, email: String): ContactsRobot {
        return displayName(name)
            .email(email)
            .saveNewContact()
    }

    fun editNameEmailAndSave(name: String, email: String): ContactDetailsRobot {
        displayName(name)
            .email(email)
            .saveEditContact()
        return ContactDetailsRobot()
    }

    private fun displayName(name: String): AddContactRobot {
        view
            .withId(R.id.input)
            .isDescendantOf(
                view.withId(R.id.contact_display_name)
            )
            .replaceText(name)
        return this
    }

    private fun email(email: String): AddContactRobot {
        view
            .withId(R.id.input)
            .isDescendantOf(
                view.withId(R.id.emailAddressesContainer)
            )
            .isCompletelyDisplayed()
            .replaceText(email)
        return this
    }

    private fun saveNewContact(): ContactsRobot {
        view.withId(R.id.action_save).click()
        return ContactsRobot()
    }

    private fun saveEditContact(): ContactDetailsRobot {
        view.withId(R.id.action_save).click()
        return ContactDetailsRobot()
    }
}
