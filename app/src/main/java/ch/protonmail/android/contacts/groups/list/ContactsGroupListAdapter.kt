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
import ch.protonmail.android.views.messagesList.SenderInitialView

class ContactsGroupsListAdapter(
    private val onContactGroupClickListener: (ContactGroupListItem) -> Unit,
    private val onWriteToGroupClickListener: (ContactGroupListItem) -> Unit,
    private val onContactGroupSelect: (ContactGroupListItem) -> Unit
) : ListAdapter<ContactGroupListItem, RecyclerView.ViewHolder>(ContactGroupItemDiffCallback()) {

//    private var selectedItems: MutableSet<ContactLabel>? = null
//
//    val getSelectedItems get() = selectedItems

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemContactsBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val viewHolder = ContactGroupsViewHolder(
            binding.initialsViewContactsList,
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

        binding.initialsViewContactsList.setOnClickListener {
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

//    fun endSelectionMode() {
//        selectedItems?.forEach {
//            if (items.contains(it)) {
//                items.find { contactLabel -> (contactLabel == it) }?.isSelected =
//                    ContactEmailGroupSelectionState.DEFAULT
//            }
//        }
//        selectedItems = null
//        notifyDataSetChanged()
//    }
//
//    private fun bind(
//        contactLabel: ContactLabel,
//        clickListener: (ContactLabel) -> Unit,
//        writeToGroupListener: (ContactLabel) -> Unit,
//        onContactGroupSelect: (() -> Unit)?
//    ) {
//        itemView.contactIcon.isVisible = true
//        itemView.contactIconLetter.isVisible = false

//        itemView.text_view_contact_name.text = contactLabel.name
//        val members = contactLabel.contactEmailsCount
//        itemView.text_view_contact_subtitle.text = itemView.context.resources.getQuantityString(
//            R.plurals.contact_group_members,
//            members,
//            members
//        )
//        var colorString = contactLabel.color
//        colorString = UiUtil.normalizeColor(colorString)
//        itemView.initials_view_contacts_list.background.setColorFilter(
//            Color.parseColor(colorString),
//            PorterDuff.Mode.SRC_IN
//        )
//
//        updateSelectedUI(contactLabel, itemView)
//        itemView.initials_view_contacts_list.setOnClickListener {
//            val selectedItems = selectedItems
//            if (selectedItems != null) {
//                if (selectedItems.contains(contactLabel)) {
//                    contactLabel.isSelected = ContactEmailGroupSelectionState.DEFAULT
//                    selectedItems.remove(contactLabel)
//                    if (selectedItems.isEmpty()) {
//                        this@ContactsGroupsListAdapter.selectedItems = null
//                        onSelectionModeChange?.invoke(SelectionModeEnum.ENDED)
//                        notifyDataSetChanged()
//                    }
//                } else {
//                    contactLabel.isSelected = ContactEmailGroupSelectionState.SELECTED
//                    selectedItems.add(contactLabel)
//                }
//                notifyItemChanged(adapterPosition)
//            } else {
//                if (onSelectionModeChange == null) {
//                    return@setOnClickListener
//                }
//                if (this@ContactsGroupsListAdapter.selectedItems == null) {
//                    contactLabel.isSelected = ContactEmailGroupSelectionState.SELECTED
//                    this@ContactsGroupsListAdapter.selectedItems = hashSetOf(contactLabel)
//                    onSelectionModeChange.invoke(SelectionModeEnum.STARTED)
//                    notifyDataSetChanged()
//                }
//            }
//            onContactGroupSelect?.invoke()
//        }
//
//        itemView.setOnLongClickListener {
//            if (onSelectionModeChange == null) {
//                return@setOnLongClickListener false
//            }
//            if (this@ContactsGroupsListAdapter.selectedItems == null) {
//                contactLabel.isSelected = ContactEmailGroupSelectionState.SELECTED
//                this@ContactsGroupsListAdapter.selectedItems = hashSetOf(contactLabel)
//                onSelectionModeChange.invoke(SelectionModeEnum.STARTED)
//                notifyDataSetChanged()
//            }
//            onContactGroupSelect?.invoke()
//            return@setOnLongClickListener true
//        }
//
//        itemView.setOnClickListener {
//            val selectedItems = selectedItems
//            if (selectedItems != null) {
//                if (selectedItems.contains(contactLabel)) {
//                    contactLabel.isSelected = ContactEmailGroupSelectionState.DEFAULT
//                    selectedItems.remove(contactLabel)
//                    if (selectedItems.isEmpty()) {
//                        this@ContactsGroupsListAdapter.selectedItems = null
//                        onSelectionModeChange?.invoke(SelectionModeEnum.ENDED)
//                        notifyDataSetChanged()
//                    }
//                } else {
//                    contactLabel.isSelected = ContactEmailGroupSelectionState.SELECTED
//                    selectedItems.add(contactLabel)
//                }
//                notifyItemChanged(adapterPosition)
//            } else {
//                clickListener(contactLabel)
//            }
//        }
//
//    }


    private class ContactGroupsViewHolder(
        val initialsView: SenderInitialView,
        val contactName: TextView,
        val contactSubtitle: TextView,
        val editButton: ImageView,
        root: ConstraintLayout
    ) : RecyclerView.ViewHolder(root) {

        fun bind(item: ContactGroupListItem) {
            contactName.text = item.name

            itemView.isActivated = item.isSelected

            initialsView.apply {
                bind("", item.isSelected, item.color)
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
                oldItem.isSelected == newItem.isSelected
    }

}


