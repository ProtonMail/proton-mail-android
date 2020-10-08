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

            val messagesList = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("Message item with subject: \"$subject\"\n")
                description.appendText("Here is the actual list of messages:\n")
                messagesList.forEach { description.appendText(" - \"$it\"\n") }
            }

            override fun matchesSafely(item: MessagesListViewHolder.MessageViewHolder): Boolean {
                val messageSubjectView = item.itemView.findViewById<TextView>(R.id.messageTitleTextView)
                val actualSubject = messageSubjectView.text.toString()
                return if (messageSubjectView != null) {
                    subject == actualSubject
                } else {
                    messagesList.add(actualSubject)
                    false
                }
            }
        }
    }

    /**
     * Matches the Mailbox message represented by [MessagesListItemView] by message subject and sender.
     * Subject must be unique in a list in order to use this matcher.
     *
     * @param subject - message subject
     * @param to - message sender email
     */
    fun withMessageSubjectAndRecipient(subject: String, to: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            MessagesListViewHolder.MessageViewHolder>(MessagesListViewHolder.MessageViewHolder::class.java) {

            val messagesList = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("Message item with subject and recipient: \"$subject\"\n")
                description.appendText("Here is the actual list of messages:\n")
                messagesList.forEach { description.appendText(" - \"$it\"\n") }
            }

            override fun matchesSafely(item: MessagesListViewHolder.MessageViewHolder): Boolean {
                val messageSubjectView = item.itemView.findViewById<TextView>(R.id.messageTitleTextView)
                val messageToTextView = item.itemView.findViewById<TextView>(R.id.messageSenderTextView)
                val actualSubject = messageSubjectView.text.toString()
                val actualTo = messageToTextView.text.toString()
                return if (messageSubjectView != null && messageToTextView != null) {
                    subject == actualSubject && to == actualTo
                } else {
                    messagesList.add("Subject: $actualSubject, to: $actualTo")
                    false
                }
            }
        }
    }

    /**
     * Matches the Mailbox message represented by [MessagesListItemView] by message subject part.
     * Subject must be unique in a list in order to use this matcher.
     *
     * @param subjectPart - message subject part
     */
    fun withMessageSubjectContaining(subjectPart: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder,
            MessagesListViewHolder.MessageViewHolder>(MessagesListViewHolder.MessageViewHolder::class.java) {

            val messagesList = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("Message item that contains pattern: \"$subjectPart\"\n")
                description.appendText("Here is the actual list of messages:\n")
                messagesList.forEach { description.appendText(" - \"$it\"\n") }
            }

            override fun matchesSafely(item: MessagesListViewHolder.MessageViewHolder): Boolean {
                val messageSubjectView = item.itemView.findViewById<TextView>(R.id.messageTitleTextView)
                val actualSubject = messageSubjectView.text.toString()
                return if (actualSubject.contains(subjectPart)) {
                    true
                } else {
                    messagesList.add(actualSubject)
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
            val messagesList = ArrayList<String>()

            override fun describeTo(description: Description) {
                description.appendText("Message item that contains subject text: $text.\nIterated subjects:\n")
                description.appendText("Here is the actual list of messages:\n")
                messagesList.forEach { description.appendText(" - \"$it\"\n") }
            }

            override fun matchesSafely(item: MessagesListViewHolder.MessageViewHolder): Boolean {
                val messageSubjectView = item.itemView.findViewById<TextView>(R.id.messageTitleTextView)
                val actualSubject = messageSubjectView.text.toString()
                if (messageSubjectView != null) {
                    /** since we need only the first match [alreadyMatched] var acts as a guard for other matches **/
                    val matched = !alreadyMatched && messageSubjectView.text.toString().contains(text)
                    if (matched) alreadyMatched = true
                    messagesList.add(actualSubject)
                    return matched
                }
                return false
            }
        }
    }
}
