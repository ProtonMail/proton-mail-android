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

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.contacts.details.ContactEmailGroupSelectionState
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.ui.selection.SelectionModeEnum
import kotlinx.android.synthetic.main.contacts_v2_list_item.view.*

class ContactsGroupsListAdapter(
    private var items: List<ContactLabel>,
    private val onContactGroupClickListener: (ContactLabel) -> Unit,
    private val onWriteToGroupClickListener: (ContactLabel) -> Unit,
    private val onContactGroupSelect: (() -> Unit)?,
    private val onSelectionModeChange: ((SelectionModeEnum) -> Unit)?
) : RecyclerView.Adapter<ViewHolder>() {

    private var selectedItems: MutableSet<ContactLabel>? = null

    val getSelectedItems get() = selectedItems

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(
            items[position],
            onContactGroupClickListener,
            onWriteToGroupClickListener,
            onContactGroupSelect
        )
    }

    fun endSelectionMode() {
        selectedItems?.forEach {
            if (items.contains(it)) {
                items.find { contactLabel -> (contactLabel == it) }?.isSelected =
                    ContactEmailGroupSelectionState.DEFAULT
            }
        }
        selectedItems = null
        notifyDataSetChanged()
    }

    private fun ViewHolder.bind(
        contactLabel: ContactLabel,
        clickListener: (ContactLabel) -> Unit,
        writeToGroupListener: (ContactLabel) -> Unit,
        onContactGroupSelect: (() -> Unit)?
    ) {
        itemView.contactIcon.isVisible = true
        itemView.contactIconLetter.isVisible = false

        itemView.contact_name.text = contactLabel.name
        val members = contactLabel.contactEmailsCount
        itemView.contact_subtitle.text = itemView.context.resources.getQuantityString(
            R.plurals.contact_group_members,
            members,
            members
        )
        var colorString = contactLabel.color
        colorString = UiUtil.normalizeColor(colorString)
        itemView.contactIcon.background.setColorFilter(
            Color.parseColor(colorString),
            PorterDuff.Mode.SRC_IN
        )

        updateSelectedUI(contactLabel, itemView)
        itemView.contactIcon.setOnClickListener {
            val selectedItems = selectedItems
            if (selectedItems != null) {
                if (selectedItems.contains(contactLabel)) {
                    contactLabel.isSelected = ContactEmailGroupSelectionState.DEFAULT
                    selectedItems.remove(contactLabel)
                    if (selectedItems.isEmpty()) {
                        this@ContactsGroupsListAdapter.selectedItems = null
                        onSelectionModeChange?.invoke(SelectionModeEnum.ENDED)
                        notifyDataSetChanged()
                    }
                } else {
                    contactLabel.isSelected = ContactEmailGroupSelectionState.SELECTED
                    selectedItems.add(contactLabel)
                }
                notifyItemChanged(adapterPosition)
            } else {
                if (onSelectionModeChange == null) {
                    return@setOnClickListener
                }
                if (this@ContactsGroupsListAdapter.selectedItems == null) {
                    contactLabel.isSelected = ContactEmailGroupSelectionState.SELECTED
                    this@ContactsGroupsListAdapter.selectedItems = hashSetOf(contactLabel)
                    onSelectionModeChange.invoke(SelectionModeEnum.STARTED)
                    notifyDataSetChanged()
                }
            }
            onContactGroupSelect?.invoke()
        }

        itemView.setOnLongClickListener {
            if (onSelectionModeChange == null) {
                return@setOnLongClickListener false
            }
            if (this@ContactsGroupsListAdapter.selectedItems == null) {
                contactLabel.isSelected = ContactEmailGroupSelectionState.SELECTED
                this@ContactsGroupsListAdapter.selectedItems = hashSetOf(contactLabel)
                onSelectionModeChange.invoke(SelectionModeEnum.STARTED)
                notifyDataSetChanged()
            }
            onContactGroupSelect?.invoke()
            return@setOnLongClickListener true
        }

        itemView.setOnClickListener {
            val selectedItems = selectedItems
            if (selectedItems != null) {
                if (selectedItems.contains(contactLabel)) {
                    contactLabel.isSelected = ContactEmailGroupSelectionState.DEFAULT
                    selectedItems.remove(contactLabel)
                    if (selectedItems.isEmpty()) {
                        this@ContactsGroupsListAdapter.selectedItems = null
                        onSelectionModeChange?.invoke(SelectionModeEnum.ENDED)
                        notifyDataSetChanged()
                    }
                } else {
                    contactLabel.isSelected = ContactEmailGroupSelectionState.SELECTED
                    selectedItems.add(contactLabel)
                }
                notifyItemChanged(adapterPosition)
            } else {
                clickListener(contactLabel)
            }
        }

        itemView.writeButton.setOnClickListener {
            writeToGroupListener.invoke(contactLabel)
        }
    }

    private fun updateSelectedUI(contactLabel: ContactLabel, itemView: View) {
        if (contactLabel.isSelected == ContactEmailGroupSelectionState.SELECTED) {
            itemView.contactIcon.setBackgroundResource(0)
            itemView.contactIcon.setImageResource(R.drawable.ic_contacts_checkmark)
            itemView.contactIcon.setBackgroundResource(R.drawable.bg_circle)
            itemView.contactIcon.drawable.setColorFilter(
                ContextCompat.getColor(itemView.context, R.color.contact_action),
                PorterDuff.Mode.SRC_IN
            )
            itemView.rowWrapper.setBackgroundResource(R.color.selectable_color)
        } else {
            updateNotSelectedUI(contactLabel, itemView)
        }
    }

    private fun updateNotSelectedUI(contactLabel: ContactLabel, itemView: View) {
        itemView.contactIcon.setBackgroundResource(0)
        itemView.contactIcon.setImageResource(R.drawable.ic_contact_groups)
        itemView.contactIcon.setBackgroundResource(R.drawable.label_color_circle)
        var colorString = contactLabel.color
        colorString = UiUtil.normalizeColor(colorString)
        itemView.contactIcon.background.setColorFilter(
            Color.parseColor(colorString),
            PorterDuff.Mode.SRC_IN
        )
        itemView.rowWrapper.setBackgroundResource(R.color.white)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.contacts_v2_list_item, parent, false)
        )
    }

    fun setData(items: List<ContactLabel>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
