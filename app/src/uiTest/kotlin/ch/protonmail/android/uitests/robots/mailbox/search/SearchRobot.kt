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
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [SearchRobot] class contains actions and verifications for Search functionality.
 */
class SearchRobot {

    fun searchMessageText(messageSubject: String): SearchRobot {
        UIActions.id.insertTextInFieldWithIdAndPressImeAction(R.id.search_src_text, messageSubject)
        return this
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    class Verify {

        fun searchedMessageFound() {
            UIActions.recyclerView.waitForBeingPopulated(R.id.messages_list_view)
            UIActions.recyclerView
                .scrollToRecyclerViewMatchedItem(
                    R.id.messages_list_view,
                    withFirstInstanceMessageSubject(TestData.searchMessageSubject)
                )
        }

        fun noSearchResults() {
            UIActions.wait.untilViewWithIdAndTextAppears(R.id.no_messages, R.string.no_search_results)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
