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
package ch.protonmail.android.uitests.robots.mailbox.drafts

import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatImageView
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.menu.MenuRobot
import me.proton.fusion.Fusion

/**
 * [DraftsRobot] implements [MailboxRobotInterface],
 * contains actions and verifications for Drafts composer functionality.
 */
class DraftsRobot : MailboxRobotInterface, Fusion {

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

    override fun refreshMessageList(): DraftsRobot {
        super.refreshMessageList()
        return this
    }

    fun moreOptions(): DraftsRobot {
        view.instanceOf(AppCompatImageView::class.java).hasParent(view.instanceOf(ActionMenuView::class.java))
        return this
    }

    fun emptyFolder(): DraftsRobot {
        view.withId(R.id.title).withText(R.string.empty_folder).click()
        return this
    }

    fun confirm(): DraftsRobot {
        view.withId(android.R.id.button1).click()
        return this
    }

    fun clickDraftBySubject(subject: String): ComposerRobot {
        super.clickMessageBySubject(subject)
        return ComposerRobot()
    }

    fun clickFirstMatchedDraftBySubject(subject: String): ComposerRobot {
        super.clickFirstMatchedMessageBySubject(subject)
        return ComposerRobot()
    }

    fun clickDraftByPosition(position: Int): ComposerRobot {
        super.clickMessageByPosition(position)
        return ComposerRobot()
    }

    /**
     * Contains all the validations that can be performed by [MenuRobot].
     */
    class Verify : MailboxRobotInterface.verify(), Fusion {

        fun folderEmpty() {
            // TODO - remove this workaround with 20 sec waiting time when possible
//            UIActions.wait.forViewWithIdAndText(R.id.no_messages, R.string.no_messages, 20000)
        }

        fun draftMessageSaved(draftSubject: String): DraftsRobot {
            view.withId(R.id.subject_text_view).withText(draftSubject).click()
            return DraftsRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
