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
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withFirstInstanceMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withMessageSubjectAndFlag
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withMessageSubjectAndRecipient
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitests.robots.mailbox.search.SearchRobot
import ch.protonmail.android.uitests.robots.menu.MenuRobot
import ch.protonmail.android.uitests.testsHelper.StringUtils
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.TIMEOUT_30S
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.TIMEOUT_60S
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions.saveMessageSubject
import me.proton.core.test.android.instrumented.Robot

interface MailboxRobotInterface : Robot {

    fun swipeLeftMessageAtPosition(position: Int): Any {
        saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetSwipeLeftMessage)())
        recyclerView
            .withId(messagesRecyclerViewId)
            .onItemAtPosition(position)
            .swipeLeft()
        return Any()
    }

    fun longClickMessageOnPosition(position: Int): Any {
        saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetLongClickMessage)())
        recyclerView
            .withId(messagesRecyclerViewId)
            .onItemAtPosition(position)
            .longClick()
        return Any()
    }

    fun deleteMessageWithSwipe(position: Int): Any {
        saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetDeleteWithSwipeMessage)())
        recyclerView
            .withId(messagesRecyclerViewId)
            .onItemAtPosition(position)
            .swipeRight()
        return Any()
    }

    fun searchBar(): SearchRobot {
        view.withId(R.id.searchImageButton).click()
        return SearchRobot()
    }

    fun compose(): ComposerRobot {
        view.withId(R.id.composeImageButton).waitForCondition({ view.isCompletelyDisplayed().viewMatcher() }, watchTimeout = TIMEOUT_60S)
        view.withId(R.id.composeImageButton).click()
        return ComposerRobot()
    }

    fun menuDrawer(): MenuRobot {
        view.waitForCondition({ view.withId(drawerLayoutId).isCompletelyDisplayed().viewMatcher() }, TIMEOUT_60S)
        view.withId(drawerLayoutId).openDrawer()
        return MenuRobot()
    }

    fun clickMessageByPosition(position: Int): MessageRobot {
        saveMessageSubjectAtPosition(messagesRecyclerViewId, position, (::SetSelectMessage)())
        recyclerView
            .withId(messagesRecyclerViewId)
//            .waitUntilPopulated()
            .onItemAtPosition(position)
            .click()
        return MessageRobot()
    }

    fun clickMessageBySubject(subject: String): MessageRobot {
        view.withId(R.id.subject_text_view).withText(subject).click()
        return MessageRobot()
    }

    fun clickFirstMatchedMessageBySubject(subject: String): MessageRobot {
        view.instanceOf(ImageView::class.java).withParent(view.withId(R.id.mailboxRecyclerView)).checkDoesNotExist()
        recyclerView
            .withId(messagesRecyclerViewId)
//            .waitUntilPopulated()
            .onHolderItem(withFirstInstanceMessageSubject(subject))
            .click()
        return MessageRobot()
    }

    fun refreshMessageList(): Any {
//        recyclerView.withId(messagesRecyclerViewId).waitUntilPopulated()
        view.withId(messagesRecyclerViewId).swipeDown()
        // Waits for loading icon to disappear
        view.instanceOf(ImageView::class.java).withParent(view.withId(messagesRecyclerViewId)).checkDoesNotExist()
//        recyclerView.withId(messagesRecyclerViewId).waitUntilPopulated()
        return Any()
    }

    fun mailboxLayoutShown() {
//        recyclerView.withId(messagesRecyclerViewId).waitUntilPopulated()
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    @Suppress("ClassName")
    open class verify : Robot {

        fun messageExists(messageSubject: String) {
            view.withId(messageTitleTextViewId).withText(messageSubject).checkDisplayed()
        }

        fun draftWithAttachmentSaved(draftSubject: String) {
            view.withId(messageTitleTextViewId).withText(draftSubject).checkDisplayed()
        }

        fun messageMovedToTrash(subject: String, date: String) {
            val messageMovedToTrash = StringUtils.quantityStringFromResource(R.plurals.action_move_to_trash, 1)
            view.withText(messageMovedToTrash).checkDisplayed()
            view.withId(R.id.subject_text_view).withText(subject).checkDoesNotExist()
        }

        fun messageDeleted(subject: String) {
            view.withId(R.id.subject_text_view).withText(subject).checkDoesNotExist()
        }

        fun multipleMessagesMovedToTrash(subjectMessageOne: String, subjectMessageTwo: String) {
            val messagesMovedToTrash = StringUtils.quantityStringFromResource(R.plurals.action_move_to_trash, 2)
            view.withText(messagesMovedToTrash).checkDisplayed()
            view.withId(R.id.subject_text_view).withText(subjectMessageOne).checkDoesNotExist()
            view.withId(R.id.subject_text_view).withText(subjectMessageTwo).checkDoesNotExist()
        }

        fun multipleMessagesDeleted(subjectMessageOne: String, subjectMessageTwo: String) {
            view.withId(R.id.subject_text_view).withText(subjectMessageOne).checkDoesNotExist()
            view.withId(R.id.subject_text_view).withText(subjectMessageTwo).checkDoesNotExist()
        }

        fun messageWithSubjectExists(subject: String) {
            view.withText(subject).checkDisplayed()
            recyclerView
                .withId(messagesRecyclerViewId)
                .scrollToHolder(withFirstInstanceMessageSubject(subject))
        }

        fun messageWithSubjectHasRepliedFlag(subject: String) {
            recyclerView
                .withId(messagesRecyclerViewId)
                .scrollToHolder(withMessageSubjectAndFlag(subject, R.id.reply_image_view))
        }

        fun messageWithSubjectHasRepliedAllFlag(subject: String) {
            recyclerView
                .withId(messagesRecyclerViewId)
                .scrollToHolder(withMessageSubjectAndFlag(subject, R.id.reply_all_image_view))
        }

        fun messageWithSubjectHasForwardedFlag(subject: String) {
            recyclerView
                .withId(messagesRecyclerViewId)
                .scrollToHolder(withMessageSubjectAndFlag(subject, R.id.forward_image_view))
        }

        fun messageWithSubjectAndRecipientExists(subject: String, to: String) {
            recyclerView
                .withId(messagesRecyclerViewId)
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

        override fun invoke(subject: String, date: String) {
            swipeLeftMessageSubject = subject
            swipeLeftMessageDate = date
        }
    }

    private class SetDeleteWithSwipeMessage : (String, String) -> Unit {

        override fun invoke(subject: String, date: String) {
            deletedMessageSubject = subject
            deletedMessageDate = date
        }
    }

    class SetSelectMessage : (String, String) -> Unit {

        override fun invoke(subject: String, date: String) {
            selectedMessageSubject = subject
            selectedMessageDate = date
        }
    }

    companion object {

        //    TODO replace below line with core test lib code
        fun saveMessageSubjectAtPosition(
            @IdRes recyclerViewId: Int,
            position: Int,
            method: (String, String) -> Unit
        ): ViewInteraction = Espresso.onView(ViewMatchers.withId(recyclerViewId))
            .perform(saveMessageSubject(position, method))

        var longClickedMessageSubject = ""
        var longClickedMessageDate = ""
        var swipeLeftMessageSubject = ""
        var swipeLeftMessageDate = ""
        var selectedMessageSubject = ""
        var selectedMessageDate = ""
        var deletedMessageSubject = ""
        var deletedMessageDate = ""

        private const val messagesRecyclerViewId = R.id.mailboxRecyclerView
        private const val messageTitleTextViewId = R.id.subject_text_view
        private const val drawerLayoutId = R.id.drawer_layout
    }
}
