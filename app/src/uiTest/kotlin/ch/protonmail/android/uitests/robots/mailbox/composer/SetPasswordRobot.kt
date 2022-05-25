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

package ch.protonmail.android.uitests.robots.mailbox.composer

import android.widget.EditText
import ch.protonmail.android.R
import me.proton.core.test.android.instrumented.Robot

/**
 * Class represents Message Password dialog.
 */
class SetPasswordRobot : Robot {

    fun definePasswordWithHint(password: String, hint: String): ComposerRobot {
        return definePassword(password)
            .confirmPassword(password)
            .defineHint(hint)
            .applyPassword()
    }

    private fun definePassword(password: String): SetPasswordRobot {
        view
            .withId(R.id.set_msg_password_msg_password_input)
            .instanceOf(EditText::class.java)
            .replaceText(password)
        return this
    }

    private fun confirmPassword(password: String): SetPasswordRobot {
        view
            .withId(R.id.set_msg_password_repeat_password_input)
            .instanceOf(EditText::class.java)
            .replaceText(password)
        return this
    }

    private fun defineHint(hint: String): SetPasswordRobot {
        view.withId(R.id.set_msg_password_hint_input).instanceOf(EditText::class.java).replaceText(hint)
        return this
    }

    private fun applyPassword(): ComposerRobot {
        view.withId(R.id.set_msg_password_apply_button).click()
        return ComposerRobot()
    }
}
