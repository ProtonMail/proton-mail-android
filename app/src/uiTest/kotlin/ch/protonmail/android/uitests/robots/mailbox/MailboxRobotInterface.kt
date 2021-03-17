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
import me.proton.core.test.android.instrumented.CoreRobot

interface MailboxRobotInterface : CoreRobot {

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
        view.withId(R.id.search).click()
        return SearchRobot()
    }

    fun compose(): ComposerRobot {
        view.withId(R.id.compose).click()
        return ComposerRobot()
    }

    fun menuDrawer(): MenuRobot {
        view.withId(drawerLayoutId).wait(15_000L).openDrawer()
        return MenuRobot()
    }

    fun clickMessageByPosition(position: Int): MessageRobot {
        recyclerView.withId(messagesRecyclerViewId).waitUntilPopulated()
        // TODO replace below line with core test lib code
        UIActions.recyclerView
            .messages.saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetSelectMessage)())
        view.withId(messagesRecyclerViewId)
        recyclerView.withId(messagesRecyclerViewId).onItemAtPosition(position).click()
        return MessageRobot()
    }

    fun clickMessageBySubject(subject: String): MessageRobot {
        recyclerView
            .withId(messagesRecyclerViewId)
            .waitUntilPopulated()
            .onHolderItem(withMessageSubject(subject))
            .click()
        return MessageRobot()
    }

    fun clickFirstMatchedMessageBySubject(subject: String): MessageRobot {
        view.instanceOf(ImageView::class.java).withParent(view.withId(R.id.mailboxRecyclerView)).waitUntilGone()
        recyclerView
            .withId(messagesRecyclerViewId)
            .waitUntilPopulated()
            .onHolderItem(withFirstInstanceMessageSubject(subject))
            .click()
        return MessageRobot()
    }

    fun refreshMessageList(): Any {
        recyclerView.withId(messagesRecyclerViewId).waitUntilPopulated()
        view.withId(messagesRecyclerViewId).swipeDown()
        // Waits for loading icon to disappear
        view.instanceOf(ImageView::class.java).withParent(view.withId(messagesRecyclerViewId)).waitUntilGone()
        recyclerView.withId(messagesRecyclerViewId).waitUntilPopulated()
        return Any()
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    @Suppress("ClassName")
    open class verify : CoreRobot {

        fun messageExists(messageSubject: String) {
            UIActions.wait.forViewWithIdAndText(messageTitleTextViewId, messageSubject)
        }

        fun draftWithAttachmentSaved(draftSubject: String) {
            UIActions.wait.forViewWithIdAndText(messageTitleTextViewId, draftSubject)
        }

        fun mailboxLayoutShown() {
            view.withId(R.id.mailboxSwipeRefreshLayout).wait()
        }

        fun messageDeleted(subject: String, date: String) {
            UIActions.recyclerView
                .common.waitForBeingPopulated(messagesRecyclerViewId)
                .messages.checkDoesNotContainMessage(messagesRecyclerViewId, subject, date)
        }

        fun messageWithSubjectExists(subject: String) {
            recyclerView
                .withId(messagesRecyclerViewId)
                .waitUntilPopulated()
                .scrollToHolder(withFirstInstanceMessageSubject(subject))
        }

        fun messageWithSubjectHasRepliedFlag(subject: String) {
            UIActions.recyclerView.common.waitForBeingPopulated(messagesRecyclerViewId)
            UIActions.recyclerView
                .common.scrollToRecyclerViewMatchedItem(
                    messagesRecyclerViewId,
                    withMessageSubjectAndFlag(subject, R.id.replyImageView)
                )
        }

        fun messageWithSubjectHasRepliedAllFlag(subject: String) {
            UIActions.recyclerView.common.waitForBeingPopulated(messagesRecyclerViewId)
            UIActions.recyclerView
                .common.scrollToRecyclerViewMatchedItem(
                    messagesRecyclerViewId,
                    withMessageSubjectAndFlag(subject, R.id.replyAllImageView)
                )
        }

        fun messageWithSubjectHasForwardedFlag(subject: String) {
            UIActions.recyclerView.common.waitForBeingPopulated(messagesRecyclerViewId)
            UIActions.recyclerView
                .common.scrollToRecyclerViewMatchedItem(
                    messagesRecyclerViewId,
                    withMessageSubjectAndFlag(subject, R.id.forwardImageView)
                )
        }

        fun messageWithSubjectAndRecipientExists(subject: String, to: String) {
            recyclerView
                .withId(messagesRecyclerViewId)
                .waitUntilPopulated()
                .scrollToHolder(withMessageSubjectAndRecipient(subject, to))
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

        private const val messagesRecyclerViewId = R.id.mailboxRecyclerView
        private const val messageTitleTextViewId = R.id.subjectTextView
        private const val drawerLayoutId = R.id.drawer_layout
    }
}
