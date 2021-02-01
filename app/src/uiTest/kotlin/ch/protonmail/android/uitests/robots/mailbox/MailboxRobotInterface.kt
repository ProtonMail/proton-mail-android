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

import android.widget.ImageView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withFirstInstanceMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withMessageSubjectAndFlag
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withMessageSubjectAndRecipient
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitests.robots.mailbox.search.SearchRobot
import ch.protonmail.android.uitests.robots.menu.MenuRobot
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import ch.protonmail.android.uitests.testsHelper.uiactions.click
import ch.protonmail.android.uitests.testsHelper.uiactions.swipeViewDown
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf

interface MailboxRobotInterface {

    fun swipeLeftMessageAtPosition(position: Int): Any {
        UIActions.recyclerView
            .common.waitForBeingPopulated(messagesRecyclerViewId)
            .messages.saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetSwipeLeftMessage)())
        UIActions.recyclerView.common.swipeRightToLeftObjectWithIdAtPosition(messagesRecyclerViewId, position)
        return Any()
    }

    fun longClickMessageOnPosition(position: Int): Any {
        UIActions.recyclerView
            .common.waitForBeingPopulated(messagesRecyclerViewId)
            .messages.saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetLongClickMessage)())
        UIActions.recyclerView.common.longClickItemInRecyclerView(messagesRecyclerViewId, position)
        return Any()
    }

    fun deleteMessageWithSwipe(position: Int): Any {
        UIActions.recyclerView
            .common.waitForBeingPopulated(messagesRecyclerViewId)
            .messages.saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetDeleteWithSwipeMessage)())
        UIActions.recyclerView.common.swipeItemLeftToRightOnPosition(messagesRecyclerViewId, position)
        return Any()
    }

    fun searchBar(): SearchRobot {
        UIActions.wait.forViewWithId(R.id.search).click()
        return SearchRobot()
    }

    fun compose(): ComposerRobot {
        UIActions.wait.forViewWithId(R.id.compose).click()
        return ComposerRobot()
    }

    fun menuDrawer(): MenuRobot {
        UIActions.wait.forViewWithId(drawerLayoutId, 15_000L)
        UIActions.id.openMenuDrawerWithId(drawerLayoutId)
        return MenuRobot()
    }

    fun clickMessageByPosition(position: Int): MessageRobot {
        UIActions.wait.forViewWithId(messagesRecyclerViewId)
        UIActions.recyclerView.messages.saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetSelectMessage)())
        UIActions.recyclerView.common.clickOnRecyclerViewItemByPosition(messagesRecyclerViewId, 1)
        return MessageRobot()
    }

    fun clickMessageBySubject(subject: String): MessageRobot {
        UIActions.wait
            .untilViewByViewInteractionIsGone(
                onView(
                    allOf(
                        instanceOf(ImageView::class.java), withParent(withId(R.id.messages_list_view))
                    )
                )
            )
        UIActions.wait.forViewWithId(messagesRecyclerViewId)
        UIActions.recyclerView
            .common.waitForBeingPopulated(messagesRecyclerViewId)
            .common.clickOnRecyclerViewMatchedItem(messagesRecyclerViewId, withMessageSubject(subject))
        return MessageRobot()
    }

    fun refreshMessageList(): Any {
        UIActions.wait.forViewWithId(messagesRecyclerViewId).swipeViewDown()
        return Any()
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    @Suppress("ClassName")
    open class verify {

        fun messageExists(messageSubject: String) {
            UIActions.wait.forViewWithIdAndText(messageTitleTextViewId, messageSubject)
        }

        fun draftWithAttachmentSaved(draftSubject: String) {
            UIActions.wait.forViewWithIdAndText(messageTitleTextViewId, draftSubject)
        }

        fun mailboxLayoutShown() {
            UIActions.wait.forViewWithId(R.id.swipe_refresh_layout)
        }

        fun messageDeleted(subject: String, date: String) {
            UIActions.recyclerView
                .common.waitForBeingPopulated(messagesRecyclerViewId)
                .messages.checkDoesNotContainMessage(messagesRecyclerViewId, subject, date)
        }

        fun messageWithSubjectExists(subject: String) {
            UIActions.recyclerView.common.waitForBeingPopulated(messagesRecyclerViewId)
            UIActions.recyclerView
                .common.scrollToRecyclerViewMatchedItem(messagesRecyclerViewId, withFirstInstanceMessageSubject(subject))
        }

        fun messageWithSubjectHasRepliedFlag(subject: String) {
            UIActions.recyclerView.common.waitForBeingPopulated(messagesRecyclerViewId)
            UIActions.recyclerView
                .common.scrollToRecyclerViewMatchedItem(
                    messagesRecyclerViewId,
                    withMessageSubjectAndFlag(subject, R.id.messageReplyTextView)
                )
        }

        fun messageWithSubjectHasRepliedAllFlag(subject: String) {
            UIActions.recyclerView.common.waitForBeingPopulated(messagesRecyclerViewId)
            UIActions.recyclerView
                .common.scrollToRecyclerViewMatchedItem(
                    messagesRecyclerViewId,
                    withMessageSubjectAndFlag(subject, R.id.messageReplyAllTextView)
                )
        }

        fun messageWithSubjectHasForwardedFlag(subject: String) {
            UIActions.recyclerView.common.waitForBeingPopulated(messagesRecyclerViewId)
            UIActions.recyclerView
                .common.scrollToRecyclerViewMatchedItem(
                    messagesRecyclerViewId,
                    withMessageSubjectAndFlag(subject, R.id.messageForwardTextView)
                )
        }

        fun messageWithSubjectAndRecipientExists(subject: String, to: String) {
            UIActions.recyclerView.common.waitForBeingPopulated(messagesRecyclerViewId)
            UIActions.recyclerView
                .common.scrollToRecyclerViewMatchedItem(messagesRecyclerViewId, withMessageSubjectAndRecipient(subject, to))
        }
    }

    private class SetLongClickMessage : (String, String) -> Unit {
        override fun invoke(subject: String, date: String) {
            longClickedMessageSubject = subject
            longClickedMessageDate = date
        }
    }

    private class SetSwipeLeftMessage : (String, String) -> Unit {
        override fun invoke(text: String, date: String) {
            swipeLeftMessageSubject = text
            swipeLeftMessageDate = date
        }
    }

    private class SetDeleteWithSwipeMessage : (String, String) -> Unit {
        override fun invoke(text: String, date: String) {
            deletedMessageSubject = text
            deletedMessageDate = date
        }
    }

    private class SetSelectMessage : (String, String) -> Unit {
        override fun invoke(subject: String, date: String) {
            selectedMessageSubject = subject
            selectedMessageDate = date
        }
    }

    companion object {
        var longClickedMessageSubject = ""
        var longClickedMessageDate = ""
        var swipeLeftMessageSubject = ""
        var swipeLeftMessageDate = ""
        var selectedMessageSubject = ""
        var selectedMessageDate = ""
        var deletedMessageSubject = ""
        var deletedMessageDate = ""

        private const val messagesRecyclerViewId = R.id.messages_list_view
        private const val messageTitleTextViewId = R.id.messageTitleTextView
        private const val drawerLayoutId = R.id.drawer_layout
    }
}
