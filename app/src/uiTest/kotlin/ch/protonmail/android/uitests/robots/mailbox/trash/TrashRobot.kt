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
package ch.protonmail.android.uitests.robots.mailbox.trash

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [TrashRobot] class implements [MailboxRobotInterface],
 * contains actions and verifications for Trash mailbox functionality.
 */
class TrashRobot : MailboxRobotInterface {

    override fun swipeLeftMessageAtPosition(messagePosition: Int): TrashRobot {
        super.swipeLeftMessageAtPosition(messagePosition)
        return this
    }

    override fun longClickMessageOnPosition(position: Int): TrashRobot {
        super.longClickMessageOnPosition(position)
        return this
    }

    fun moreOptions(): TrashRobot {
        UIActions.system.clickMoreOptionsButton()
        return this
    }

    fun emptyFolder(): TrashRobot {
        UIActions.allOf.clickViewWithIdAndText(R.id.title, R.string.empty_folder)
        return this
    }

    fun confirm(): TrashRobot {
        UIActions.system.clickPositiveDialogButton()
        return this
    }

    /**
     * Contains all the validations that can be performed by [TrashRobot].
     */
    class Verify {

        fun folderEmpty() {
            //TODO - remove this workaround with 20 sec waiting time when possible
            UIActions.wait.forViewWithIdAndText(R.id.no_messages, R.string.no_messages, 20000)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
