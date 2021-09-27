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
package ch.protonmail.android.uitests.robots.settings.account

import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import ch.protonmail.android.uitests.testsHelper.User
import ch.protonmail.android.uitests.testsHelper.uiactions.insert

/**
 * Class represents Email recovery view.
 */
class RecoveryEmailRobot {

    fun changeRecoveryEmail(user: User): RecoveryEmailRobot {
        return newEmail(user.email)
            .confirmNewEmail(user.email)
            .save()
            .password(user.password)
            .confirmSave()
    }

    private fun newEmail(email: String): RecoveryEmailRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.newRecoveryEmail, email)
        return this
    }

    private fun confirmNewEmail(email: String): RecoveryEmailRobot {
        UIActions.id.insertTextIntoFieldWithId(R.id.newRecoveryEmailConfirm, email)
        return this
    }

    private fun password(password: String): RecoveryEmailRobot {
        UIActions.wait.forViewWithId(R.id.current_password)
            .insert(password)
        return this
    }

    private fun save(): RecoveryEmailRobot {
        UIActions.id.clickViewWithId(R.id.save_new_email)
        return RecoveryEmailRobot()
    }

    private fun confirmSave(): RecoveryEmailRobot {
        UIActions.system.clickPositiveDialogButton()
        return RecoveryEmailRobot()
    }

    /**
     * Contains all the validations that can be performed by [RecoveryEmailRobot].
     */
    class Verify {

        fun recoveryEmailChangedTo(email: String) {
            UIActions.wait.forViewWithIdAndText(R.id.currentRecoveryEmail, email)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
