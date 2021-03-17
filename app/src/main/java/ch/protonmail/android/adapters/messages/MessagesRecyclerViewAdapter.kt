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
package ch.protonmail.android.adapters.messages

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.*
import ch.protonmail.android.utils.ui.selection.SelectionModeEnum
import ch.protonmail.android.views.messagesList.MessagesListFooterView
import ch.protonmail.android.views.messagesList.MessagesListItemView

class MessagesRecyclerViewAdapter(
    private val context: Context,
    private val onSelectionModeChange: ((SelectionModeEnum) -> Unit)?
) : RecyclerView.Adapter<MessagesListViewHolder>() {


    private var mMailboxLocation = Constants.MessageLocationType.INVALID

    private var labels = mapOf<String, Label>()
    private val messages = mutableListOf<Message>()
    private val selectedMessageIds: MutableSet<String> = mutableSetOf()

    private var pendingUploadList: List<PendingUpload>? = null
    private var pendingSendList: List<PendingSend>? = null
    private var contactsList: List<ContactEmail>? = null

    private var onItemClick: ((Message) -> Unit)? = null

    var includeFooter: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (value) {
                notifyItemInserted(messages.size)
            } else {
                notifyItemRemoved(messages.size)
            }

        }

    val checkedMessages get() = selectedMessageIds.mapNotNull { messages.find { message -> message.messageId == it } }

    fun getItem(position: Int) = messages[position]

    fun addAll(messages: List<Message>) {
        this.messages.addAll(
            messages.filter {
                !it.deleted
            }
        )
        notifyDataSetChanged()
    }

    fun clear() {
        messages.clear()
        notifyDataSetChanged()
    }

    fun setItemClick(onItemClick: ((Message) -> Unit)?) {
        this.onItemClick = onItemClick
    }

    private enum class ElementType {
        MESSAGE, FOOTER
    }

    override fun getItemViewType(position: Int): Int {
        val itemViewType = if (position == messages.size) ElementType.FOOTER else ElementType.MESSAGE
        return itemViewType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessagesListViewHolder {
        return when (ElementType.values()[viewType]) {
            ElementType.MESSAGE -> MessagesListViewHolder.MessageViewHolder(MessagesListItemView(context))
            ElementType.FOOTER -> MessagesListViewHolder.FooterViewHolder(MessagesListFooterView(context))
        }
    }

    override fun getItemCount(): Int = messages.size + if (includeFooter) 1 else 0

    override fun onBindViewHolder(holder: MessagesListViewHolder, position: Int) {
        when (ElementType.values()[getItemViewType(position)]) {
            ElementType.MESSAGE -> {
                (holder as MessagesListViewHolder.MessageViewHolder).bindMessage(position)
            }
            ElementType.FOOTER -> {
                // NOOP
            }
        }
    }

    private fun MessagesListViewHolder.MessageViewHolder.bindMessage(position: Int) {
        val message = messages[position]
        val messageLabels = message.allLabelIDs.mapNotNull { labels[it] }

        message.senderDisplayName = contactsList?.find { message.senderEmail == it.email }?.name ?: message.senderName

        this.view.bind(message, messageLabels, mMailboxLocation)

        val isSelected = selectedMessageIds.contains(message.messageId)
        this.view.isActivated = isSelected
        this.view.tag = message.messageId

        this.view.setOnClickListener {
            val messageId = it.tag as String
            if (selectedMessageIds.isNotEmpty()) {
                if (selectedMessageIds.contains(messageId)) {
                    selectedMessageIds.remove(messageId)
                    if (selectedMessageIds.isEmpty()) {
                        onSelectionModeChange?.invoke(SelectionModeEnum.ENDED)
                        notifyDataSetChanged()
                    }
                } else {
                    selectedMessageIds.add(messageId)
                }

                notifyItemChanged(position)
            } else {
                onItemClick?.invoke(message)
            }

        }

        // TODO: This will be changed with MAILAND-1501.
        //  I'm just disabling long click with commenting this code for now.
//        this.view.setOnLongClickListener {
//            val messageId = it.tag as String
//            if (onSelectionModeChange == null) {
//                return@setOnLongClickListener false
//            }
//
//            if (selectedMessageIds.isEmpty()) {
//                selectedMessageIds.add(messageId)
//                onSelectionModeChange.invoke(SelectionModeEnum.STARTED)
//                notifyDataSetChanged()
//            }
//
//            return@setOnLongClickListener true
//        }
    }

    fun endSelectionMode() {
        selectedMessageIds.clear()
        notifyDataSetChanged()
    }

    fun setLabels(labels: List<Label>) {
        this.labels = labels.map { it.id to it }.toMap()
        notifyDataSetChanged()
    }

    fun setPendingUploadsList(pendingUploadList: List<PendingUpload>) {
        this.pendingUploadList = pendingUploadList
        notifyDataSetChanged() // TODO
    }

    fun setPendingForSendingList(pendingSendList: List<PendingSend>) {
        this.pendingSendList = pendingSendList
        notifyDataSetChanged() // TODO
    }

    fun setContactsList(contactsList: List<ContactEmail>?) {
        this.contactsList = contactsList
        notifyDataSetChanged() // TODO
    }

    fun setNewLocation(mailboxLocation: Constants.MessageLocationType) {
        mMailboxLocation = mailboxLocation
    }
}
