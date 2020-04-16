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
package ch.protonmail.android.contacts.groups

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import ch.protonmail.android.R
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.contacts.groups.details.ContactGroupEmailViewHolder
import ch.protonmail.android.utils.extensions.listen
import ch.protonmail.android.utils.extensions.listenForDelete

/**
 * Created by kadrikj on 9/3/18. */
class ContactGroupEmailsAdapter(
        private val context: Context,
        private var items: List<ContactEmail>,
        private val contactEmailClick: ((ContactEmail) -> Unit)?,
        private val contactEmailDeleteClick: ((ContactEmail) -> Unit)? = null,
        private val mode: GroupsItemAdapterMode = GroupsItemAdapterMode.NORMAL
): RecyclerView.Adapter<ContactGroupEmailViewHolder>() {

    fun setData(items: List<ContactEmail>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun getData(): HashSet<ContactEmail> = HashSet(items)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactGroupEmailViewHolder {
        val viewHolder = ContactGroupEmailViewHolder(
                LayoutInflater.from(context).inflate(
                        when (mode) {
                            GroupsItemAdapterMode.NORMAL -> R.layout.contact_groups_email_list_item
                            GroupsItemAdapterMode.CHECKBOXES -> R.layout.contact_groups_email_list_item_selectable
                            GroupsItemAdapterMode.DELETE -> R.layout.contact_groups_email_list_item_closeable
                        },
                        parent, false
                ), mode
        )
        viewHolder.listen { position ->
            contactEmailClick?.invoke(items[position])
            viewHolder.toggle()
        }
        if (mode == GroupsItemAdapterMode.DELETE) {
            viewHolder.listenForDelete { position -> contactEmailDeleteClick?.invoke(items[position]) }
        }
        return viewHolder
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ContactGroupEmailViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun getUnSelected(): ArrayList<ContactEmail> {
        return ArrayList(items.filter {
            !it.selected
        })
    }

    fun getSelected(): ArrayList<ContactEmail> {
        return ArrayList(items.filter {
            it.selected
        })
    }
}