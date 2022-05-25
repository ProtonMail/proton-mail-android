/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.compose.recipients

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.databinding.GroupRecipientListItemBinding

class GroupRecipientsAdapter(
    private var items: List<MessageRecipient>,
    private val clickListener: () -> Unit
) : RecyclerView.Adapter<GroupRecipientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupRecipientViewHolder {
        val binding = GroupRecipientListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        val viewHolder = GroupRecipientViewHolder(binding)

        binding.root.setOnClickListener {
            viewHolder.toggle()
            clickListener()
        }
        return viewHolder
    }

    override fun getItemCount(): Int = items.size

    fun getData(): List<MessageRecipient> = this.items

    override fun onBindViewHolder(holder: GroupRecipientViewHolder, position: Int) {
        holder.bind(items[position], clickListener)
    }

    fun setData(items: List<MessageRecipient>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun getSelected(): ArrayList<MessageRecipient> {
        return ArrayList(
            items.filter {
                it.isSelected
            }
        )
    }
}
