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
import androidx.test.espresso.matcher.RootMatchers
import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.StringUtils.quantityStringFromResource
import me.proton.core.test.android.instrumented.Robot
import me.proton.core.test.android.instrumented.utils.ActivityProvider
import org.hamcrest.CoreMatchers

/**
 * [GroupDetailsRobot] class contains actions and verifications for Contacts functionality.
 */
open class GroupDetailsRobot : Robot {

    fun edit(): AddContactGroupRobot {
        view.withId(R.id.action_contact_details_edit).click()
        return AddContactGroupRobot()
    }

    fun deleteGroup(): ContactsRobot.ContactsGroupView {
        return delete()
            .confirmDeletion()
    }

    fun navigateUp(): ContactsRobot.ContactsGroupView {
        view.instanceOf(AppCompatImageButton::class.java).isDescendantOf(view.withId(R.id.animToolbar)).click()
        return ContactsRobot.ContactsGroupView()
    }

    private fun delete(): GroupDetailsRobot {
        view.withId(R.id.action_delete).click()
        return this
    }

    private fun confirmDeletion(): ContactsRobot.ContactsGroupView {
        view.withId(android.R.id.button1).isEnabled().isCompletelyDisplayed().click()
        view.withText(quantityStringFromResource(R.plurals.group_deleted, 1))
            .withRootMatcher(
                RootMatchers.withDecorView(CoreMatchers.not(ActivityProvider.currentActivity!!.window.decorView))
            )
            .checkDisplayed()
        return ContactsRobot.ContactsGroupView()
    }

    /**
     * Contains all the validations that can be performed by [GroupDetailsRobot].
     */
    class Verify

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
