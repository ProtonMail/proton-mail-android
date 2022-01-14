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
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import ch.protonmail.android.R
import me.proton.core.test.android.instrumented.Robot
import me.proton.core.test.android.instrumented.utils.ActivityProvider
import org.hamcrest.CoreMatchers.not

/**
 * [AddContactRobot] class contains actions and verifications for Add/Edit Contacts.
 */
class AddContactRobot : Robot {

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
        view.withId(R.id.contact_display_name).instanceOf(EditText::class.java).replaceText(name)
        return this
    }

    private fun email(email: String): AddContactRobot {
        view
            .withId(R.id.option)
            .instanceOf(EditText::class.java)
            .withVisibility(ViewMatchers.Visibility.VISIBLE)
            .isDescendantOf(view.withId(R.id.emailAddressesContainer))
            .replaceText(email)
        return this
    }

    private fun saveNewContact(): ContactsRobot {
        view.withId(R.id.action_save).click()
        view
            .withText(R.string.contact_saved)
            .withRootMatcher(withDecorView(not(ActivityProvider.currentActivity!!.window.decorView)))
            .checkDisabled()
        return ContactsRobot()
    }

    private fun saveEditContact(): ContactDetailsRobot {
        view.withId(R.id.action_save).click()
        return ContactDetailsRobot()
    }
}
