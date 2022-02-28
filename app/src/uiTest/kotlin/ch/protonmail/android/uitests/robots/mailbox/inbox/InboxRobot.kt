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
package ch.protonmail.android.uitests.robots.mailbox.inbox

import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.MoveToFolderRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.SelectionStateRobotInterface

/**
 * [InboxRobot] class implements [MailboxRobotInterface],
 * contains actions and verifications for Inbox functionality.
 */
class InboxRobot : MailboxRobotInterface {

    override fun swipeLeftMessageAtPosition(position: Int): InboxRobot {
        super.swipeLeftMessageAtPosition(position)
        return this
    }

    override fun longClickMessageOnPosition(position: Int): SelectionStateRobot {
        super.longClickMessageOnPosition(position)
        return SelectionStateRobot()
    }

    override fun deleteMessageWithSwipe(position: Int): InboxRobot {
        super.deleteMessageWithSwipe(position)
        return this
    }

    override fun refreshMessageList(): InboxRobot {
        super.refreshMessageList()
        return InboxRobot()
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    class Verify : MailboxRobotInterface.verify()

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    class SelectionStateRobot : SelectionStateRobotInterface {

        override fun exitMessageSelectionState(): InboxRobot {
            super.exitMessageSelectionState()
            return InboxRobot()
        }

        override fun selectMessageAtPosition(position: Int): SelectionStateRobot {
            super.selectMessageAtPosition(position)
            return this
        }

        override fun addLabel(): InboxRobot {
            super.addLabel()
            return InboxRobot()
        }

        override fun addFolder(): MoveToFolderRobot {
            super.addFolder()
            return MoveToFolderRobot()
        }

        override fun moveToTrash() = InboxRobot()
    }

    class MoveToFolderRobot : MoveToFolderRobotInterface {

        override fun moveToExistingFolder(name: String): InboxRobot {
            super.moveToExistingFolder(name)
            return InboxRobot()
        }
    }
}
