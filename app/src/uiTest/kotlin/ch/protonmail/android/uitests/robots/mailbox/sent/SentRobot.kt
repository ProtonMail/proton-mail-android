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
package ch.protonmail.android.uitests.robots.mailbox.sent

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.MoveToFolderRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.SelectionStateRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [SentRobot] class implements [MailboxRobotInterface],
 * contains actions and verifications for Sent mailbox functionality.
 */
class SentRobot : MailboxRobotInterface {

    override fun swipeLeftMessageAtPosition(position: Int): SentRobot {
        super.swipeLeftMessageAtPosition(position)
        return this
    }

    override fun longClickMessageOnPosition(position: Int): SelectionStateRobot {
        super.longClickMessageOnPosition(position)
        return SelectionStateRobot()
    }

    override fun deleteMessageWithSwipe(position: Int): SentRobot {
        super.deleteMessageWithSwipe(position)
        return this
    }

    fun getMessageSubjectAtPosition(position: Int): String = ""
//        UIActions.get.messageSubjectByPosition(R.id.messages_list_view, position)!!

    class SelectionStateRobot : SelectionStateRobotInterface {

        override fun exitMessageSelectionState(): InboxRobot {
            super.exitMessageSelectionState()
            return InboxRobot()
        }

        override fun selectMessage(position: Int): SelectionStateRobot {
            super.selectMessage(position)
            return this
        }

        override fun addLabel(): SentRobot {
            super.addLabel()
            return SentRobot()
        }

        override fun addFolder(): MoveToFolderRobot {
            super.addFolder()
            return MoveToFolderRobot()
        }

        fun moveToTrash(): SentRobot {
            UIActions.allOf.clickVisibleViewWithId(R.id.move_to_trash)
            return SentRobot()
        }
    }

    class MoveToFolderRobot : MoveToFolderRobotInterface {

        override fun moveToExistingFolder(name: String): InboxRobot {
            super.moveToExistingFolder(name)
            return InboxRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [SentRobot].
     */
    class Verify {

        fun multipleMessagesDeleted() {
            UIActions.wait.untilViewWithIdAppears(R.id.snackbar_text)
            UIActions.check.viewWithIdAndTextIsDisplayed(R.id.snackbar_text, "Messages moved to trash")
            //TODO add check by message text
        }

        fun messageStarred() {
            UIActions.wait.untilViewWithIdAppears(R.id.snackbar_text)
            UIActions.check.viewWithIdAndTextIsDisplayed(R.id.snackbar_text, "Message star updated")
        }

        fun messageWithSubjectExists(subject: String) {
            UIActions.recyclerView.scrollToRecyclerViewMatchedItem(R.id.messages_list_view, withMessageSubject(subject))
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
