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
import ch.protonmail.android.uitests.actions.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitests.testsHelper.TestUser
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * Class represents Password management view.
 */
open class PasswordManagementRobot : UIActions() {

    fun changePassword(user: TestUser): AccountSettingsRobot {
        return PasswordRobot()
            .currentPassword(user.password)
            .newPassword(user.password)
            .confirmNewPassword(user.password)
            .savePassword()
    }

    fun changeMailboxPassword(user: TestUser): AccountSettingsRobot {
        return MailboxPasswordRobot()
            .currentMailboxPassword(user.password)
            .newMailboxPassword(user.password)
            .confirmNewMailboxPassword(user.password)
            .saveMailboxPassword()
    }

    class PasswordRobot : PasswordManagementRobot() {

        internal fun currentPassword(password: String): PasswordRobot {
            insertTextIntoFieldWithId(R.id.currentPasswordEditText, password)
            return this
        }

        internal fun newPassword(password: String): PasswordRobot {
            insertTextIntoFieldWithId(R.id.newPassword, password)
            return this
        }

        internal fun confirmNewPassword(password: String): PasswordRobot {
            insertTextIntoFieldWithId(R.id.newPasswordConfirm, password)
            return this
        }

        internal fun savePassword(): AccountSettingsRobot {
            clickOnObjectWithId(R.id.save)
            return AccountSettingsRobot()
        }
    }

    class MailboxPasswordRobot : PasswordManagementRobot() {

        internal fun currentMailboxPassword(password: String): MailboxPasswordRobot {
            val id = R.id.mailboxLoginPassword
            scrollDownElementInScrollView(id)
            insertTextIntoFieldWithId(id, password)
            return this
        }

        internal fun newMailboxPassword(password: String): MailboxPasswordRobot {
            insertTextIntoFieldWithId(R.id.mailboxNewPassword, password)
            return this
        }

        internal fun confirmNewMailboxPassword(password: String): MailboxPasswordRobot {
            insertTextIntoFieldWithId(R.id.mailboxNewPassword, password)
            return this
        }

        internal fun saveMailboxPassword(): AccountSettingsRobot {
            clickOnObjectWithId(R.id.mailbox_save)
            return AccountSettingsRobot()
        }
    }
}