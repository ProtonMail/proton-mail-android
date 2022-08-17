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
package ch.protonmail.android.uitests.robots.mailbox.trash

import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface
import me.proton.fusion.Fusion

/**
 * [TrashRobot] class implements [MailboxRobotInterface],
 * contains actions and verifications for Trash mailbox functionality.
 */
class TrashRobot : MailboxRobotInterface, Fusion {

    override fun swipeLeftMessageAtPosition(position: Int): TrashRobot {
        super.swipeLeftMessageAtPosition(position)
        return this
    }

    override fun longClickMessageOnPosition(position: Int): TrashRobot {
        super.longClickMessageOnPosition(position)
        return this
    }

    fun moreOptions(): TrashRobot {
        view.instanceOf(AppCompatImageView::class.java).hasParent(view.instanceOf(ActionMenuView::class.java)).click()
        return this
    }

    fun emptyFolder(): TrashRobot {
        view.withId(R.id.title).withText(R.string.empty_folder).click()
        return this
    }

    fun confirm(): TrashRobot {
        view.withId(android.R.id.button1).click()
        return this
    }

    fun navigateUpToTrash(): TrashRobot {
        view.instanceOf(AppCompatImageButton::class.java).hasParent(view.withId(R.id.toolbar)).click()
        return TrashRobot()
    }

    /**
     * Contains all the validations that can be performed by [TrashRobot].
     */
    class Verify : Fusion {

        fun folderEmpty() {
            view.withId(R.id.no_messages).checkIsDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
