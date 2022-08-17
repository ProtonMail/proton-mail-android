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
package ch.protonmail.android.uitests.robots.mailbox.spam

import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatImageView
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface
import me.proton.fusion.Fusion

/**
 * [SpamRobot] class implements [MailboxRobotInterface],
 * contains actions and verifications for Spam mailbox functionality.
 */
open class SpamRobot : MailboxRobotInterface, Fusion {

    override fun swipeLeftMessageAtPosition(position: Int): SpamRobot {
        super.swipeLeftMessageAtPosition(position)
        return this
    }

    override fun longClickMessageOnPosition(position: Int): SpamRobot {
        super.longClickMessageOnPosition(position)
        return this
    }

    fun moreOptions(): SpamRobot {
        view.instanceOf(AppCompatImageView::class.java).hasParent(view.instanceOf(ActionMenuView::class.java)).click()
        return this
    }
}
