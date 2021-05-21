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
package ch.protonmail.android.contacts.groups.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.databinding.ListItemContactsBinding
import ch.protonmail.android.views.ListItemThumbnail

class ContactsGroupsListAdapter(
    private val onContactGroupClickListener: (ContactGroupListItem) -> Unit,
    private val onWriteToGroupClickListener: (ContactGroupListItem) -> Unit,
    private val onContactGroupSelect: (ContactGroupListItem) -> Unit
) : ListAdapter<ContactGroupListItem, RecyclerView.ViewHolder>(ContactGroupItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemContactsBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val viewHolder = ContactGroupsViewHolder(
            binding.thumbnailViewContactsList,
            binding.textViewContactName,
            binding.textViewContactSubtitle,
            binding.imageViewEditButton,
            binding.root
        )

        binding.imageViewEditButton.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onWriteToGroupClickListener(getItem(position))
            }
        }

        binding.thumbnailViewContactsList.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onContactGroupSelect(getItem(position))
            }
        }

        binding.root.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onContactGroupClickListener(getItem(position))
            }
        }

        binding.root.setOnLongClickListener {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onContactGroupSelect(getItem(position))
                true
            } else {
                false
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ContactGroupsViewHolder).bind(getItem(position))
    }

    private class ContactGroupsViewHolder(
        val itemThumbnail: ListItemThumbnail,
        val contactName: TextView,
        val contactSubtitle: TextView,
        val editButton: ImageView,
        root: ConstraintLayout
    ) : RecyclerView.ViewHolder(root) {

        fun bind(item: ContactGroupListItem) {
            contactName.text = item.name

            itemView.isSelected = item.isSelected

            itemThumbnail.apply {
                bind(
                    isSelectedActive = item.isSelected,
                    isMultiselectActive = item.isMultiselectActive,
                    circleColor = item.color
                )
                isVisible = true
            }

            contactSubtitle.apply {
                text = if (item.contactEmailsCount > 0)
                    context.resources.getQuantityString(
                        R.plurals.contact_group_members,
                        item.contactEmailsCount,
                        item.contactEmailsCount
                    )
                else
                    itemView.resources.getString(R.string.empty_email_list)
                isVisible = true
            }

            editButton.visibility = View.VISIBLE
        }
    }

    class ContactGroupItemDiffCallback : DiffUtil.ItemCallback<ContactGroupListItem>() {

        override fun areItemsTheSame(oldItem: ContactGroupListItem, newItem: ContactGroupListItem): Boolean =
            oldItem.contactId == newItem.contactId &&
                oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: ContactGroupListItem, newItem: ContactGroupListItem): Boolean =
            oldItem.contactId == newItem.contactId &&
                oldItem.name == newItem.name &&
                oldItem.color == newItem.color &&
                oldItem.contactEmailsCount == newItem.contactEmailsCount &&
                oldItem.isMultiselectActive == newItem.isMultiselectActive &&
                oldItem.isSelected == newItem.isSelected
    }

}


