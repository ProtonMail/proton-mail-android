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
package ch.protonmail.android.uitests.robots.mailbox.drafts

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface
import ch.protonmail.android.uitests.robots.menu.MenuRobot
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [DraftsRobot] implements [MailboxRobotInterface],
 * contains actions and verifications for Drafts composer functionality.
 */
class DraftsRobot : MailboxRobotInterface {

    override fun swipeLeftMessageAtPosition(position: Int): DraftsRobot {
        super.swipeLeftMessageAtPosition(position)
        return this
    }

    override fun longClickMessageOnPosition(position: Int): DraftsRobot {
        super.longClickMessageOnPosition(position)
        return this
    }

    override fun deleteMessageWithSwipe(position: Int): DraftsRobot {
        super.deleteMessageWithSwipe(position)
        return this
    }

    fun moreOptions(): DraftsRobot {
        UIActions.system.clickMoreOptionsButton()
        return this
    }

    fun emptyFolder(): DraftsRobot {
        UIActions.allOf.clickViewWithIdAndText(R.id.title, R.string.empty_folder)
        return this
    }

    fun confirm(): DraftsRobot {
        UIActions.system.clickPositiveDialogButton()
        return this
    }

    /**
     * Contains all the validations that can be performed by [MenuRobot].
     */
    class Verify {

        fun folderEmpty() {
            //TODO - remove this workaround with 20 sec waiting time when possible
            UIActions.wait.untilViewWithIdAndTextAppears(R.id.no_messages, R.string.no_messages, 20000)
        }

        fun draftMessageSaved(draftSubject: String?): DraftsRobot {
            UIActions.wait.untilViewWithIdAndTextAppears(R.id.messageTitleTextView, draftSubject!!)
            return DraftsRobot()
        }

        fun draftWithAttachmentSaved(draftSubject: String) {
            UIActions.wait.untilViewWithIdAndTextAppears(R.id.messageTitleTextView, draftSubject)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
