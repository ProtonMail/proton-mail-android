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

package ch.protonmail.android.uitests.robots.login

import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import me.proton.fusion.Fusion
import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.typeInProtonInputField

class MailboxPasswordMailRobot : Fusion {

    fun unlockMailbox(mailboxPassword: String): InboxRobot {
        view
            .withId(R.id.input)
            .isDescendantOf(view.withId(R.id.mailboxPasswordInput))
            .replaceText(mailboxPassword)
        view.withId(R.id.unlockButton).click()
        return InboxRobot()
    }
}
