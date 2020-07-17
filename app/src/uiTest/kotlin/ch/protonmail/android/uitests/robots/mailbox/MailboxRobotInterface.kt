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
@file:Suppress("UNCHECKED_CAST")

package ch.protonmail.android.uitests.robots.mailbox

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.message.MessageRobot
import ch.protonmail.android.uitests.robots.mailbox.search.SearchRobot
import ch.protonmail.android.uitests.robots.menu.MenuRobot
import ch.protonmail.android.uitests.testsHelper.UIActions

interface MailboxRobotInterface {

    fun swipeLeftMessageAtPosition(position: Int): Any {
        UIActions.recyclerView
            .saveMessageSubjectAtPosition(R.id.messages_list_view, position, (::SetSwipeLeftMessage)())
        UIActions.recyclerView.swipeRightToLeftObjectWithIdAtPosition(R.id.messages_list_view, position)
        return Any()
    }

    fun longClickMessageOnPosition(position: Int): Any {
        UIActions.recyclerView
            .saveMessageSubjectAtPosition(R.id.messages_list_view, position, (::SetLongClickMessage)())
        UIActions.recyclerView.longClickItemInRecyclerView(R.id.messages_list_view, position)
        return Any()
    }

    fun deleteMessageWithSwipe(position: Int): Any {
        UIActions.recyclerView
            .saveMessageSubjectAtPosition(R.id.messages_list_view, position, (::SetDeleteWithSwipeMessage)())
        UIActions.recyclerView.swipeItemLeftToRightOnPosition(R.id.messages_list_view, position)
        return Any()
    }

    fun searchBar(): SearchRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.search)
        UIActions.id.clickViewWithId(R.id.search)
        return SearchRobot()
    }

    fun compose(): ComposerRobot {
        UIActions.id.clickViewWithId(R.id.compose)
        return ComposerRobot()
    }

    fun menuDrawer(): MenuRobot {
        UIActions.id.openMenuDrawerWithId(R.id.drawer_layout)
        return MenuRobot()
    }

    fun selectMessage(position: Int): MessageRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.messages_list_view)
        UIActions.recyclerView.saveMessageSubjectAtPosition(R.id.messages_list_view, position, (::SetSelectMessage)())
        UIActions.recyclerView.clickOnRecyclerViewItemByPosition(R.id.messages_list_view, 1)
        return MessageRobot()
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    @Suppress("ClassName")
    class verify {

        fun messageMoved(messageSubject: String) {
            UIActions.wait.untilViewWithIdAndTextAppears(R.id.messageTitleTextView, messageSubject)
        }

        fun draftWithAttachmentSaved(draftSubject: String) {
            UIActions.wait.untilViewWithIdAndTextAppears(R.id.messageTitleTextView, draftSubject)
        }

        fun mailboxLayoutShown() {
            UIActions.wait.untilViewWithIdAppears(R.id.swipe_refresh_layout)
        }
    }

    fun setSwipedLeftMessage(text: String) {
        swipeLeftMessageText = text
    }

    fun setSelectedMessage(text: String) {
        selectedMessageText = text
    }

    fun setDeletedMessage(text: String) {
        deletedMessageText = text
    }

    private class SetLongClickMessage : (String, String) -> Unit {
        override fun invoke(message: String, date: String) {
            longClickedMessageText = message
            longClickedMessageDate = date
        }
    }

    private class SetSwipeLeftMessage : (String, String) -> Unit {
        override fun invoke(text: String, date: String) {
            swipeLeftMessageText = text
            swipeLeftMessageDate = date
        }
    }

    private class SetDeleteWithSwipeMessage : (String, String) -> Unit {
        override fun invoke(text: String, date: String) {
            deletedMessageText = text
            deletedMessageDate = date
        }
    }

    private class SetSelectMessage : (String, String) -> Unit {
        override fun invoke(text: String, date: String) {
            selectedMessageText = text
            selectedMessageDate = date
        }
    }

    companion object {
        var longClickedMessageText = ""
        var longClickedMessageDate = ""
        var swipeLeftMessageText = ""
        var swipeLeftMessageDate = ""
        var selectedMessageText = ""
        var selectedMessageDate = ""
        var deletedMessageText = ""
        var deletedMessageDate = ""
    }
}
