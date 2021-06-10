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

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import ch.protonmail.android.R
import ch.protonmail.android.databinding.ListItemContactsBinding
import ch.protonmail.android.views.ListItemThumbnail
import timber.log.Timber

class ContactsListAdapter(
    private val onContactGroupClickListener: (ContactItem) -> Unit,
    private val onContactGroupSelect: (ContactItem) -> Unit,
    private val onWriteToContactClicked: (String) -> Unit
) : ListAdapter<ContactItem, RecyclerView.ViewHolder>(ContactItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemContactsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = ContactViewHolder(
            binding.thumbnailViewContactsList,
            binding.textViewContactName,
            binding.textViewContactSubtitle,
            binding.imageViewContactItemSendButton,
            binding.root
        )

        binding.imageViewContactItemSendButton.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != NO_POSITION) {
                val emailValue = getItem(viewHolder.adapterPosition).contactEmails
                if (!emailValue.isNullOrEmpty()) {
                    onWriteToContactClicked(emailValue)
                } else {
                    Timber.v("Cannot edit empty emails.")
                }
            }
        }

        binding.thumbnailViewContactsList.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != NO_POSITION) {
                onContactGroupSelect(getItem(position))
            }
        }

        binding.root.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != NO_POSITION) {
                onContactGroupClickListener(getItem(position))
            }
        }

        binding.root.setOnLongClickListener {
            val position = viewHolder.adapterPosition
            if (position != NO_POSITION) {
                onContactGroupSelect(getItem(position))
                true
            } else
                false
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ContactViewHolder).bind(getItem(position))
    }

    private class ContactViewHolder(
        val itemThumbnail: ListItemThumbnail,
        val contactName: TextView,
        val contactSubtitle: TextView,
        val editButton: ImageView,
        root: ConstraintLayout
    ) : RecyclerView.ViewHolder(root) {

        fun bind(item: ContactItem) {
            Timber.v("Bind contact item: $item")
            contactName.text = item.name

            // special case for header items, where the text is in the string res
            if (item.headerStringRes != null) {
                contactName.text = itemView.resources.getString(item.headerStringRes)
                itemView.isClickable = false
            } else {
                itemView.isClickable = true
            }

            itemView.isSelected = item.isSelected

            if (item.initials.isNotEmpty()) {
                itemThumbnail.apply {
                    bind(
                        item.isSelected,
                        item.isMultiselectActive,
                        item.initials
                    )
                    isVisible = true
                }
            } else {
                itemThumbnail.isVisible = false
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

    class ContactItemDiffCallback : DiffUtil.ItemCallback<ContactItem>() {

        override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem.contactId == newItem.contactId &&
                oldItem.headerStringRes == newItem.headerStringRes

        override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem.contactId == newItem.contactId &&
                oldItem.name == newItem.name &&
                oldItem.contactEmails == newItem.contactEmails &&
                oldItem.isSelected == newItem.isSelected &&
                oldItem.isMultiselectActive == newItem.isMultiselectActive &&
                oldItem.isProtonMailContact == newItem.isProtonMailContact
    }
}
