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

import androidx.appcompat.widget.AppCompatImageButton
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.ApplyLabelRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.MoveToFolderRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.SelectionStateRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import me.proton.core.test.android.instrumented.Robot

/**
 * [SentRobot] class implements [MailboxRobotInterface],
 * contains actions and verifications for Sent mailbox functionality.
 */
class SentRobot : MailboxRobotInterface, Robot {

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

    override fun refreshMessageList(): SentRobot {
        super.refreshMessageList()
        return SentRobot()
    }

    fun navigateUpToSent(): SentRobot {
        view.instanceOf(AppCompatImageButton::class.java).withParent(view.withId(R.id.toolbar)).click()
        return SentRobot()
    }

    /**
     * Handles Mailbox selection state actions and verifications after user long click one of the messages.
     */
    class SelectionStateRobot : SelectionStateRobotInterface, Robot {

        override fun exitMessageSelectionState(): InboxRobot {
            super.exitMessageSelectionState()
            return InboxRobot()
        }

        override fun selectMessageAtPosition(position: Int): SelectionStateRobot {
            super.selectMessageAtPosition(position)
            return this
        }

        override fun addLabel(): ApplyLabelRobot {
            super.addLabel()
            return ApplyLabelRobot()
        }

        override fun addFolder(): MoveToFolderRobot {
            super.addFolder()
            return MoveToFolderRobot()
        }

        fun deleteSelection(): ConfirmDeletionDialogRobot {
            view.withId(R.id.secondActionImageButton).click()
            return ConfirmDeletionDialogRobot()
        }
    }

    class ConfirmDeletionDialogRobot: Robot {

        fun confirmDeletion(): SentRobot {
            view.withId(android.R.id.button1).click()
            return SentRobot()
        }
    }

    /**
     * Handles Move to folder dialog actions.
     */
    class MoveToFolderRobot : MoveToFolderRobotInterface {

        override fun moveToExistingFolder(name: String): InboxRobot {
            super.moveToExistingFolder(name)
            return InboxRobot()
        }
    }

    /**
     * Handles Move to folder dialog actions.
     */
    class ApplyLabelRobot : ApplyLabelRobotInterface {

        override fun selectLabelByName(name: String): ApplyLabelRobot {
            super.selectLabelByName(name)
            return ApplyLabelRobot()
        }

        override fun apply(): SentRobot {
            super.apply()
            return SentRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [SentRobot].
     */
    class Verify : MailboxRobotInterface.verify() {

        fun messageStarred(subject: String) {
            view.withId(R.id.snackbar_text).withText(R.string.swipe_action_star).checkDisplayed()
            view.withId(R.id.subject_text_view).withText(subject)
                .hasSibling(view.withId(R.id.star_image_view)).checkDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
