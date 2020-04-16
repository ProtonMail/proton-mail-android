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
package ch.protonmail.android.compose.recipients

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import ch.protonmail.android.R
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.utils.extensions.listen

/**
 * Created by kadrikj on 9/18/18. */
class GroupRecipientsAdapter(
        private val context: Context,
        private var items: List<MessageRecipient>,
        private val clickListener: () -> Unit) : RecyclerView.Adapter<GroupRecipientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupRecipientViewHolder {
        val viewHolder = GroupRecipientViewHolder(LayoutInflater.from(context).inflate(R.layout.group_recipient_list_item, parent, false))
        viewHolder.listen { _ ->
            viewHolder.toggle()
            clickListener()
        }
        return viewHolder
    }

    override fun getItemCount(): Int = items.size

    fun getData() : List<MessageRecipient> = this.items

    override fun onBindViewHolder(holder: GroupRecipientViewHolder, position: Int) {
        holder.bind(items[position], clickListener)
    }

    fun setData(items: List<MessageRecipient>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun getSelected(): ArrayList<MessageRecipient> {
        return ArrayList(items.filter {
            it.isSelected
        })
    }
}