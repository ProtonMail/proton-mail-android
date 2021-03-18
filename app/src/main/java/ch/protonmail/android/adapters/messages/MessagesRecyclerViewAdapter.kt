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
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.PendingSend
import ch.protonmail.android.data.local.model.PendingUpload
import ch.protonmail.android.utils.ui.selection.SelectionModeEnum
import ch.protonmail.android.views.messagesList.MessagesListFooterView
import ch.protonmail.android.views.messagesList.MessagesListItemView
import kotlinx.android.synthetic.main.layout_sender_initial.view.*
import kotlinx.android.synthetic.main.list_item_mailbox.view.*

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
    private var onItemSelectionChangedListener: (() -> Unit)? = null

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

    fun setOnItemSelectionChangedListener(onItemSelectionChangedListener: () -> Unit) {
        this.onItemSelectionChangedListener = onItemSelectionChangedListener
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

    private fun selectMessage(messageId: String, position: Int) {
        if (selectedMessageIds.isEmpty()) {
            onSelectionModeChange?.invoke(SelectionModeEnum.STARTED)
            notifyDataSetChanged()
        }
        selectedMessageIds.add(messageId)
        onItemSelectionChangedListener?.invoke()
        notifyItemChanged(position)
    }

    private fun deselectMessage(messageId: String, position: Int) {
        selectedMessageIds.remove(messageId)
        if (selectedMessageIds.isEmpty()) {
            onSelectionModeChange?.invoke(SelectionModeEnum.ENDED)
            notifyDataSetChanged()
        } else {
            onItemSelectionChangedListener?.invoke()
            notifyItemChanged(position)
        }
    }

    private fun selectOrDeselectMessage(messageId: String, position: Int): Boolean {
        if (onSelectionModeChange == null || onItemSelectionChangedListener == null) {
            return false
        }

        if (selectedMessageIds.contains(messageId)) {
            deselectMessage(messageId, position)
        } else {
            selectMessage(messageId, position)
        }
        return true
    }

    private fun MessagesListViewHolder.MessageViewHolder.bindMessage(position: Int) {
        val message = messages[position]
        val messageLabels = message.allLabelIDs.mapNotNull { labels[it] }

        val pendingSend = pendingSendList?.find { it.messageId == message.messageId }
        // under these conditions the message is in sending process
        message.isBeingSent = pendingSend != null && pendingSend.sent == null
        message.isAttachmentsBeingUploaded = pendingUploadList?.find { it.messageId == message.messageId } != null
        message.senderDisplayName = contactsList?.find { message.senderEmail == it.email }?.name
            ?: message.senderName

        this.view.bind(message, messageLabels, selectedMessageIds.isNotEmpty(), mMailboxLocation)

        val isSelected = selectedMessageIds.contains(message.messageId)
        this.view.checkImageView.isActivated = isSelected

        this.view.tag = message.messageId
        this.view.senderInitialView.tag = message.messageId

        this.view.senderInitialView.setOnClickListener {
            val messageId = it.tag as String
            selectOrDeselectMessage(messageId, position)
        }

        this.view.setOnClickListener {
            if (selectedMessageIds.isNotEmpty()) {
                val messageId = it.tag as String
                selectOrDeselectMessage(messageId, position)
            } else {
                onItemClick?.invoke(message)
            }
        }
        this.view.setOnLongClickListener {
            if (selectedMessageIds.isEmpty()) {
                val messageId = it.tag as String
                return@setOnLongClickListener selectOrDeselectMessage(messageId, position)
            }
            return@setOnLongClickListener true
        }
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
        notifyDataSetChanged()
    }

    fun setPendingForSendingList(pendingSendList: List<PendingSend>) {
        this.pendingSendList = pendingSendList
        notifyDataSetChanged()
    }

    fun setContactsList(contactsList: List<ContactEmail>?) {
        this.contactsList = contactsList
        notifyDataSetChanged()
    }

    fun setNewLocation(mailboxLocation: Constants.MessageLocationType) {
        mMailboxLocation = mailboxLocation
    }
}
