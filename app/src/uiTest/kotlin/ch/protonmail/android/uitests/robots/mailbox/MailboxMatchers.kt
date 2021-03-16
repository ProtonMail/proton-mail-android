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

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedDiagnosingMatcher
import androidx.test.espresso.matcher.BoundedMatcher
import ch.protonmail.android.R
import ch.protonmail.android.adapters.FoldersAdapter
import ch.protonmail.android.adapters.LabelsAdapter
import ch.protonmail.android.adapters.messages.MessagesListViewHolder
import ch.protonmail.android.views.messagesList.MessagesListItemView
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

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
                    messagesList.add(actualSubject)
                    subject == actualSubject
                } else {
                    false
                }
            }
        }
    }

    /**
     * Matches the Mailbox message represented by [MessagesListItemView] by message subject and Reply, Reply all or
     * Forward flag. Subject must be unique in a list in order to use this matcher.
     *
     * @param subject - message subject
     * @param id - the view id of Reply, Reply all or Forward [TextView].
     */
    fun withMessageSubjectAndFlag(subject: String, @IdRes id: Int): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedDiagnosingMatcher<RecyclerView.ViewHolder,
            MessagesListViewHolder.MessageViewHolder>(MessagesListViewHolder.MessageViewHolder::class.java) {

            val messagesList = ArrayList<String>()

            override fun matchesSafely(item: MessagesListViewHolder.MessageViewHolder?, mismatchDescription: Description?): Boolean {
                val messageSubjectView = item!!.itemView.findViewById<TextView>(R.id.messageTitleTextView)
                val actualSubject = messageSubjectView.text.toString()
                val flagView = item.itemView.findViewById<LinearLayout>(R.id.messageTitleContainerLinearLayout)
                    .findViewById<LinearLayout>(R.id.flow_indicators_container)
                    .findViewById<TextView>(id)
                return if (messageSubjectView != null) {
                    messagesList.add("$actualSubject, flag visibility: ${flagView.visibility}")
                    subject == actualSubject && flagView.visibility == View.VISIBLE
                } else {
                    false
                }
            }

            override fun describeMoreTo(description: Description?) {
                description?.apply {
                    appendText("Message item with subject: \"$subject\"\n")
                    appendText("Here is the actual list of messages:\n")
                }
                messagesList.forEach { description?.appendText(" - \"$it\"\n") }
            }
        }
    }

    /**
     * Matches the Mailbox message represented by [MessagesListItemView] by message subject and Reply, Reply all or
     * Forward flag. Subject must be unique in a list in order to use this matcher.
     *
     * @param subject - message subject
     * @param id - the view id of Reply, Reply all or Forward [TextView].
     */
    fun withMessageSubjectAndLocation(subject: String, locationText: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedDiagnosingMatcher<RecyclerView.ViewHolder,
            MessagesListViewHolder.MessageViewHolder>(MessagesListViewHolder.MessageViewHolder::class.java) {

            val messagesList = ArrayList<String>()

            override fun matchesSafely(item: MessagesListViewHolder.MessageViewHolder?, mismatchDescription: Description?): Boolean {
                val messageSubjectView = item!!.itemView.findViewById<TextView>(R.id.messageTitleTextView)
                val actualSubject = messageSubjectView.text.toString()
                val locationView = item.itemView.findViewById<LinearLayout>(R.id.messageTitleContainerLinearLayout)
                    .findViewById<LinearLayout>(R.id.flow_indicators_container)
                    .findViewById<TextView>(R.id.messageLocationTextView)
                return if (messageSubjectView != null) {
                    messagesList.add("$actualSubject, location: ${locationView.text}")
                    subject == actualSubject && locationView.text == locationText
                } else {
                    false
                }
            }

            override fun describeMoreTo(description: Description?) {
                description?.apply {
                    appendText("Message item with subject: \"$subject\" and location text: $locationText\n")
                    appendText("Here is the actual list of messages:\n")
                }
                messagesList.forEach { description?.appendText(" - \"$it\"\n") }
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
                    messagesList.add("Subject: $actualSubject, to: $actualTo")
                    subject == actualSubject && to == actualTo
                } else {
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

    fun withFolderName(name: String): TypeSafeMatcher<FoldersAdapter.FolderItem> =
        withFolderName(equalTo(name))

    fun withFolderName(nameMatcher: Matcher<out Any?>): TypeSafeMatcher<FoldersAdapter.FolderItem> {
        return object : TypeSafeMatcher<FoldersAdapter.FolderItem>(FoldersAdapter.FolderItem::class.java) {
            override fun matchesSafely(item: FoldersAdapter.FolderItem): Boolean {
                return nameMatcher.matches(item.name)
            }

            override fun describeTo(description: Description) {
                description.appendText("with item content: ")
            }
        }
    }

    fun withLabelName(name: String): TypeSafeMatcher<LabelsAdapter.LabelItem> =
        withLabelName(equalTo(name))

    fun withLabelName(nameMatcher: Matcher<out Any?>): TypeSafeMatcher<LabelsAdapter.LabelItem> {
        return object : TypeSafeMatcher<LabelsAdapter.LabelItem>(LabelsAdapter.LabelItem::class.java) {
            override fun matchesSafely(item: LabelsAdapter.LabelItem): Boolean {
                return nameMatcher.matches(item.name)
            }

            override fun describeTo(description: Description) {
                description.appendText("with item content: ")
            }
        }
    }
}
