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

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.contacts.groups.list.ViewHolder
import ch.protonmail.android.utils.ui.selection.SelectionModeEnum
import ch.protonmail.android.views.contactsList.ContactListItemView
import kotlinx.android.synthetic.main.contacts_v2_list_item.view.*

class ContactsListAdapter(
    val context: Context,
    var items: List<ContactItem>,
    private val onContactGroupClickListener: (ContactItem) -> Unit,
    private val onContactGroupSelect: (() -> Unit)?,
    val onSelectionModeChange: ((SelectionModeEnum) -> Unit)?
) : RecyclerView.Adapter<ViewHolder>() {

    private enum class ItemType {
        CONTACT, HEADER
    }

    private var selectedItems: MutableSet<ContactItem>? = null

    val getSelectedItems get() = selectedItems

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder).bind(
            items[position],
            onContactGroupClickListener,
            onContactGroupSelect,
            position
        )
    }

    private fun ViewHolder.bind(
        contactItem: ContactItem,
        clickListener: (ContactItem) -> Unit,
        onContactGroupSelect: (() -> Unit)?,
        position: Int
    ) {
        val rowType = getItemType(position)
        val result = itemView as? ContactListItemView ?: when (rowType) {
            ItemType.HEADER -> ContactListItemView.ContactsHeaderView(context)
            ItemType.CONTACT -> ContactListItemView.ContactView(context)
        }

        result.bind(contactItem)
        if (rowType == ItemType.CONTACT) {
            result.contactIconLetter.setOnClickListener {
                val selectedItems = selectedItems
                if (selectedItems != null) {
                    selectDeselectItems(selectedItems, contactItem)
                    notifyItemChanged(adapterPosition)
                } else {
                    if (onSelectionModeChange == null) {
                        return@setOnClickListener
                    }
                    if (this@ContactsListAdapter.selectedItems == null) {
                        contactItem.isChecked = true
                        this@ContactsListAdapter.selectedItems = hashSetOf(contactItem)
                        onSelectionModeChange.invoke(SelectionModeEnum.STARTED)
                        notifyDataSetChanged()
                    }
                }
                onContactGroupSelect?.invoke()
            }

            result.setOnLongClickListener {
                if (onSelectionModeChange == null) {
                    return@setOnLongClickListener false
                }
                if (this@ContactsListAdapter.selectedItems == null) {
                    contactItem.isChecked = true
                    this@ContactsListAdapter.selectedItems = hashSetOf(contactItem)
                    onSelectionModeChange.invoke(SelectionModeEnum.STARTED)
                    notifyDataSetChanged()
                }
                onContactGroupSelect?.invoke()
                return@setOnLongClickListener true
            }

            result.setOnClickListener {
                val selectedItems = selectedItems
                if (selectedItems != null) {
                    selectDeselectItems(selectedItems, contactItem)
                    notifyDataSetChanged()
                } else {
                    clickListener(contactItem)
                }
                onContactGroupSelect?.invoke()
            }
        }
    }

    private fun getItemType(position: Int): ItemType {
        return if (position == 0) {
            ItemType.HEADER
        } else {
            val previousContactItem = items[position - 1]
            val contactItem = items[position]
            if (previousContactItem.isProtonMailContact && !contactItem.isProtonMailContact) {
                ItemType.HEADER
            } else ItemType.CONTACT
        }
    }

    override fun getItemViewType(position: Int) = getItemType(position).ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (ItemType.values()[viewType]) {
            ItemType.HEADER -> ViewHolder(ContactListItemView.ContactsHeaderView(context))
            ItemType.CONTACT -> ViewHolder(ContactListItemView.ContactView(context))
        }
    }

    fun setChecked(position:Int,checked:Boolean) {
        items[position].isChecked=checked
        notifyDataSetChanged()
    }

    fun setData(items: List<ContactItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun endSelectionMode() {
        selectedItems?.forEach {
            if (items.contains(it)) {
                items.find { contactItem -> (contactItem == it) }?.isChecked =
                        false
            }
        }
        selectedItems = null
        notifyDataSetChanged()
    }

    private fun selectDeselectItems(selectedItems : MutableSet<ContactItem>, contactItem : ContactItem) {
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
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
