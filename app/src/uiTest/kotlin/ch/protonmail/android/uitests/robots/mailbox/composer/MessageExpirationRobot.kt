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

import ch.protonmail.android.R
import me.proton.core.test.android.instrumented.Robot

/**
 * Class represents Message Expiration dialog.
 */
class MessageExpirationRobot : Robot {

    fun setExpirationInDays(days: Int): ComposerRobot =
        expirationDays(days)
            .confirmSetMessageExpiration()

    private fun expirationDays(days: Int): MessageExpirationRobot {
        view.withId(R.id.set_msg_expiration_3_days_text_view).click()
        return this
    }

    private fun confirmSetMessageExpiration(): ComposerRobot {
        view.withId(R.id.set_msg_expiration_set).isEnabled().isCompletelyDisplayed().click()
        return ComposerRobot()
    }
}