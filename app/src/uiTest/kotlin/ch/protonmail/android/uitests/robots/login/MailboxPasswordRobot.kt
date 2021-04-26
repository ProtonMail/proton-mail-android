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

package ch.protonmail.android.uitests.robots.login

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import me.proton.core.test.android.instrumented.CoreRobot

class MailboxPasswordRobot : CoreRobot {

    fun decryptMailbox(password: String): InboxRobot {
        return mailboxPassword(password)
            .decrypt()
    }

    private fun mailboxPassword(password: String): MailboxPasswordRobot {
        view.withId(R.id.mailbox_password).replaceText(password)
        return this
    }

    private fun decrypt(): InboxRobot {
        view.withId(R.id.sign_in).withText(R.string.decrypt).click()
        return InboxRobot()
    }
}
