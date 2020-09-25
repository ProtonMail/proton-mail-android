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
package ch.protonmail.android.uitests.robots.mailbox.search

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withFirstInstanceMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withMessageSubjectContaining
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [SearchRobot] class contains actions and verifications for Search functionality.
 */
class SearchRobot {

    fun searchMessageText(subject: String): SearchRobot {
        UIActions.id.typeTextIntoFieldWithIdAndPressImeAction(R.id.search_src_text, subject)
        return this
    }

    fun clickSearchedMessageBySubject(subject: String): MessageRobot {
        UIActions.recyclerView.waitForBeingPopulated(messagesRecyclerViewId)
        UIActions.recyclerView
            .clickOnRecyclerViewMatchedItem(messagesRecyclerViewId, withMessageSubject(subject))
        return MessageRobot()
    }

    fun clickSearchedDraftBySubject(subject: String): ComposerRobot {
        UIActions.recyclerView.waitForBeingPopulated(messagesRecyclerViewId)
        UIActions.recyclerView
            .clickOnRecyclerViewMatchedItem(messagesRecyclerViewId, withMessageSubject(subject))
        return ComposerRobot()
    }

    fun navigateUpToInbox(): InboxRobot {
        UIActions.system.clickHamburgerOrUpButton()
        return InboxRobot()
    }

    fun clickSearchedMessageBySubjectPart(subject: String): MessageRobot {
        UIActions.recyclerView.waitForBeingPopulated(messagesRecyclerViewId)
        UIActions.recyclerView
            .clickOnRecyclerViewMatchedItem(messagesRecyclerViewId, withMessageSubjectContaining(subject))
        return MessageRobot()
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    class Verify {

        fun searchedMessageFound() {
            UIActions.recyclerView.waitForBeingPopulated(messagesRecyclerViewId)
            UIActions.recyclerView
                .scrollToRecyclerViewMatchedItem(
                    R.id.messages_list_view,
                    withFirstInstanceMessageSubject(TestData.searchMessageSubject)
                )
        }

        fun noSearchResults() {
            UIActions.wait.forViewWithIdAndText(R.id.no_messages, R.string.no_search_results)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {
        private const val messagesRecyclerViewId = R.id.messages_list_view
    }
}
