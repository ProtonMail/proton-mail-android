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
import ch.protonmail.android.uitests.robots.shared.SharedRobot
import ch.protonmail.android.uitests.testsHelper.TestUser
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * Class represents Email recovery view.
 */
class RecoveryEmailRobot : UIActions() {

    fun changeRecoveryEmail(user: TestUser): RecoveryEmailRobot {
        return newEmail(user.email)
            .confirmNewEmail(user.email)
            .save()
            .password(user.password)
            .confirmSave()
    }

    private fun newEmail(email: String): RecoveryEmailRobot {
        insertTextIntoFieldWithId(R.id.newRecoveryEmail, email)
        return this
    }

    private fun confirmNewEmail(email: String): RecoveryEmailRobot {
        insertTextIntoFieldWithId(R.id.newRecoveryEmailConfirm, email)
        return this
    }

    private fun password(password: String): RecoveryEmailRobot {
        waitUntilObjectWithIdAppearsInView(R.id.current_password).insertText(password)
        return this
    }

    private fun save(): RecoveryEmailRobot {
        clickOnObjectWithId(R.id.save_new_email)
        return RecoveryEmailRobot()
    }

    private fun confirmSave(): RecoveryEmailRobot {
        SharedRobot.clickPositiveDialogButton()
        return RecoveryEmailRobot()
    }

    /**
     * Contains all the validations that can be performed by [RecoveryEmailRobot].
     */
    class Verify : DisplayNameAndSignatureRobot() {

        fun recoveryEmailChanged() {
            waitWithTimeoutForObjectWithIdAndTextToAppear(
                R.id.newRecoveryEmail, "",
                5000)
            waitWithTimeoutForObjectWithIdAndTextToAppear(
                R.id.newRecoveryEmailConfirm,
                "",
                5000)
        }
    }

    inline fun verify(block: Verify.() -> Unit) =
        Verify().apply(block) as DisplayNameAndSignatureRobot
}