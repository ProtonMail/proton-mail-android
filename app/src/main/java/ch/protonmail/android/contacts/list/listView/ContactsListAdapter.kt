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
package ch.protonmail.android.contacts.list.listView

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.databinding.ListItemContactsBinding
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.selection.SelectionModeEnum
import ch.protonmail.android.views.messagesList.SenderInitialView

class ContactsListAdapter(
    private val onContactGroupClickListener: (ContactItem) -> Unit,
    private val onContactGroupSelect: (() -> Unit)?,
    private val onSelectionModeChange: ((SelectionModeEnum) -> Unit)?
) : ListAdapter<ContactItem, RecyclerView.ViewHolder>(ContactItemDiffCallback) {

    private var selectedItems: MutableSet<ContactItem>? = null

    val getSelectedItems get() = selectedItems

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemContactsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ContactViewHolder(
            binding.initialsViewContactsList,
            binding.textViewContactName,
            binding.textViewContactSubtitle,
            binding.imageViewEditButton,
            binding.root
        )

        binding.imageViewEditButton.setOnClickListener { view ->
            val emailValue = getItem(viewHolder.adapterPosition).contactEmails
            if (!emailValue.isNullOrEmpty()) {
                val intent = Intent(view.context, ComposeMessageActivity::class.java)
                intent.putExtra(BaseActivity.EXTRA_IN_APP, true)
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_TO_RECIPIENTS, arrayOf(emailValue)
                )
                view.context.startActivity(intent)
            } else {
                view.context.showToast(R.string.email_empty, Toast.LENGTH_SHORT)
            }
        }

        binding.root.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != NO_POSITION) {
                onContactGroupClickListener(getItem(position))
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ContactViewHolder).bind(getItem(position))
    }

//    private fun ViewHolder.bind(
//        contactItem: ContactItem,
//        clickListener: (ContactItem) -> Unit,
//        onContactGroupSelect: (() -> Unit)?,
//        position: Int
//    ) {
//        val rowType = getItemType(position)
//        val result = itemView as? ContactListItemView ?: when (rowType) {
//            ItemType.HEADER -> ContactListItemView.ContactsHeaderView(context)
//            ItemType.CONTACT -> ContactListItemView.ContactView(context)
//        }

//        result.bind(contactItem)
//        if (rowType == ItemType.CONTACT) {
//            result.setOnClickListener {
//                val selectedItems = selectedItems
//                if (selectedItems != null) {
//                    selectDeselectItems(selectedItems, contactItem)
//                    notifyItemChanged(adapterPosition)
//                } else {
//                    if (onSelectionModeChange == null) {
//                        return@setOnClickListener
//                    }
//                    if (this@ContactsListAdapter.selectedItems == null) {
//                        contactItem.isChecked = true
//                        this@ContactsListAdapter.selectedItems = hashSetOf(contactItem)
//                        onSelectionModeChange.invoke(SelectionModeEnum.STARTED)
//                        notifyDataSetChanged()
//                    }
//                }
//                onContactGroupSelect?.invoke()
//            }
//
//            result.setOnLongClickListener {
//                if (onSelectionModeChange == null) {
//                    return@setOnLongClickListener false
//                }
//                if (this@ContactsListAdapter.selectedItems == null) {
//                    contactItem.isChecked = true
//                    this@ContactsListAdapter.selectedItems = hashSetOf(contactItem)
//                    onSelectionModeChange.invoke(SelectionModeEnum.STARTED)
//                    notifyDataSetChanged()
//                }
//                onContactGroupSelect?.invoke()
//                return@setOnLongClickListener true
//            }
//
//            result.setOnClickListener {
//                val selectedItems = selectedItems
//                if (selectedItems != null) {
//                    selectDeselectItems(selectedItems, contactItem)
//                    notifyDataSetChanged()
//                } else {
//                    clickListener(contactItem)
//                }
//                onContactGroupSelect?.invoke()
//            }
//        }
//    }

//    override fun getItemViewType(position: Int): Int {
//        val contactItem = getItem(position)
//        return if (contactItem.contactId == "-1") {
//            R.layout.list_item_contacts_header
//        } else
//            R.layout.list_item_contacts
//    }

    fun endSelectionMode() {
        selectedItems?.forEach {
            if (currentList.contains(it)) {
                currentList.find { contactItem -> (contactItem == it) }?.isChecked =
                    false
            }
        }
        selectedItems = null
        notifyDataSetChanged()
    }

    private fun selectDeselectItems(selectedItems: MutableSet<ContactItem>, contactItem: ContactItem) {
        if (selectedItems.contains(contactItem)) {
            selectedItems.remove(contactItem)
            contactItem.isChecked = false
            if (selectedItems.isEmpty()) {
                this@ContactsListAdapter.selectedItems = null
                onSelectionModeChange?.invoke(SelectionModeEnum.ENDED)
                notifyDataSetChanged()
            }
        } else {
            contactItem.isChecked = true
            selectedItems.add(contactItem)
        }
    }

    private class ContactViewHolder(
        val initialsView: SenderInitialView,
        val contactName: TextView,
        val contactSubtitle: TextView,
        val editButton: ImageView,
        root: ConstraintLayout
    ) : RecyclerView.ViewHolder(root) {

        fun bind(item: ContactItem) {
            contactName.text = item.name

            // special case for header items, where the text is in the string res
            if (item.headerStringRes != null) {
                contactName.text = itemView.resources.getString(item.headerStringRes)
                itemView.isClickable = false
            } else {
                itemView.isClickable = true
            }

            if (item.initials.isNotEmpty()) {
                initialsView.apply {
                    bind(item.initials, item.isChecked)
                    isVisible = true
                }
            } else {
                initialsView.isVisible = false
            }

            if (item.contactEmails == null) {
                contactSubtitle.isVisible = false
                editButton.isVisible = false
            } else {
                contactSubtitle.apply {
                    text = if (item.contactEmails.isNotEmpty())
                        item.contactEmails
                    else
                        itemView.resources.getString(R.string.empty_email_list)
                    isVisible = true
                }
                editButton.isVisible = true
            }
        }
    }

    object ContactItemDiffCallback : DiffUtil.ItemCallback<ContactItem>() {

        override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem.contactId == newItem.contactId &&
                oldItem.name == newItem.name &&
                oldItem.headerStringRes == newItem.headerStringRes

        override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem.contactId == newItem.contactId &&
                oldItem.name == newItem.name &&
                oldItem.contactEmails == newItem.contactEmails &&
                oldItem.isChecked == newItem.isChecked &&
                oldItem.isProtonMailContact == newItem.isProtonMailContact
    }
}
