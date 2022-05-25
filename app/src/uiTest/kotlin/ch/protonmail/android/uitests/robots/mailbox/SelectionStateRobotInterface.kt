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
package ch.protonmail.android.uitests.robots.mailbox

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.saveMessageSubjectAtPosition
import me.proton.core.test.android.instrumented.Robot

interface SelectionStateRobotInterface : Robot {

    fun exitMessageSelectionState(): Any {
        view.withId(R.id.mailboxRecyclerView).pressBack()
        return Any()
    }

    fun markAsReadUnread(): Any {
        view.withId(R.id.firstActionImageButton).click()
        return Any()
    }

    fun moveToTrash(): Any {
        view.withId(R.id.secondActionImageButton).click()
        return Any()
    }

    fun openMoreOptions(): Any {
        view.withId(R.id.moreActionImageButton).click()
        return Any()
    }

    fun addLabel(): Any {
        view.withId(R.id.fourthActionImageButton).click()
        return Any()
    }

    fun addFolder(): Any {
        view.withId(R.id.thirdActionImageButton).click()
        return Any()
    }

    fun selectMessageAtPosition(position: Int): Any {
        saveMessageSubjectAtPosition(R.id.mailboxRecyclerView, position, (MailboxRobotInterface::SetSelectMessage)())
        recyclerView
            .withId(R.id.mailboxRecyclerView)
//            .waitUntilPopulated()
            .onItemAtPosition(position)
            .click()
        return Any()
    }
}
