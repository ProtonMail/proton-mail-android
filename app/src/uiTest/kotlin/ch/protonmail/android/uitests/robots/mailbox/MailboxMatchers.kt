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
package ch.protonmail.android.uitests.robots.mailbox

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import ch.protonmail.android.R
import ch.protonmail.android.adapters.messages.MessagesListViewHolder
import ch.protonmail.android.views.messagesList.MessagesListItemView
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Matchers that are used by Mailbox features like Inbox, Sent, Drafts, Trash, etc.
 */
object MailboxMatchers {

    /**
     * Matches the Mailbox message represented by [MessagesListItemView] by message subject.
     * Subject must be unique in a list in order to use this matcher.
     *
     * @param subject - message subject
     */
    fun withMessageSubject(subject: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            MessagesListViewHolder.MessageViewHolder>(MessagesListViewHolder.MessageViewHolder::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("Message item that contains subject: $subject")
            }

            override fun matchesSafely(item: MessagesListViewHolder.MessageViewHolder): Boolean {
                val messageSubjectView = item.itemView.findViewById<TextView>(R.id.messageTitleTextView)
                return if (messageSubjectView != null) {
                    subject == messageSubjectView.text.toString()
                } else {
                    false
                }
            }
        }
    }

    /**
     * Matches the first instance of Mailbox message represented by [MessagesListItemView] that contains provided text.
     * Can be used in cases when multiple messages have similar subject.
     *
     * @param text - that supposed to be the part of the message subject
     */
    fun withFirstInstanceMessageSubject(text: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            MessagesListViewHolder.MessageViewHolder>(MessagesListViewHolder.MessageViewHolder::class.java) {

            private var alreadyMatched = false
            private var iteratedSubjects = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("Message item that contains subject text: $text.\nIterated subjects:\n")
                iteratedSubjects.forEach {
                    description.appendText("- $it\n")
                }
            }

            override fun matchesSafely(item: MessagesListViewHolder.MessageViewHolder): Boolean {
                val messageSubjectView = item.itemView.findViewById<TextView>(R.id.messageTitleTextView)
                if (messageSubjectView != null) {
                    val subject = messageSubjectView.text.toString()
                    iteratedSubjects.add(subject)
                    /** since we need only the first match [alreadyMatched] var acts as a guard for other matches **/
                    val matched = !alreadyMatched && (messageSubjectView.text.toString().contains(text))
                    if (matched) alreadyMatched = true
                    return matched
                }
                return false
            }
        }
    }
}