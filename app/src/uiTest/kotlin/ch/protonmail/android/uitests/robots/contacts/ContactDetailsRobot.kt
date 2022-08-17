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

import androidx.appcompat.widget.AppCompatImageButton
import ch.protonmail.android.R
import me.proton.fusion.Fusion
import okhttp3.internal.wait

/**
 * [ContactDetailsRobot] class contains actions and verifications for Contacts functionality.
 */
class ContactDetailsRobot : Fusion {

    init {
        view.withId(R.id.text_view_contact_details_item).waitForDisplayed()
    }

    fun deleteContact(): ContactsRobot {
        delete()
            .confirmDeletion()
        return ContactsRobot()
    }

    fun editContact(): AddContactRobot {
        view.withId(R.id.action_contact_details_edit).waitForEnabled().click()
        return AddContactRobot()
    }

    fun navigateUp(): ContactsRobot {
        view.withId(R.id.action_contact_details_edit).waitForDisplayed()
        view
            .instanceOf(AppCompatImageButton::class.java)
            .hasParent(view.withId(R.id.toolbar))
            .waitForEnabled()
            .click()
        return ContactsRobot()
    }

    private fun delete(): ContactDetailsRobot {
        view.withId(R.id.action_contact_details_delete).click()
        return this
    }

    private fun confirmDeletion() {
        view.withId(android.R.id.button1).isEnabled().isCompletelyDisplayed().click()
    }

    /**
     * Contains all the validations that can be performed by [ContactDetailsRobot].
     */
    class Verify {}

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
