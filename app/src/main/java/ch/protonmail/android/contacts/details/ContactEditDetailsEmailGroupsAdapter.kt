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
package ch.protonmail.android.contacts.details

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.data.local.model.ContactLabel
import ch.protonmail.android.utils.UiUtil
import kotlinx.android.synthetic.main.contacts_groups_dropdown_item.view.*


class ContactEditDetailsEmailGroupsAdapter(
    val context: Context,
    var items: List<ContactLabel>
) : RecyclerView.Adapter<ContactEditDetailsEmailGroupsAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder).bind(context, items[position])
    }

    fun setData(items: List<ContactLabel>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                LayoutInflater.from(context).inflate(
                        R.layout.contacts_groups_dropdown_item,
                        parent,
                        false
                )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(
            context: Context,
            contactLabel: ContactLabel
        ) {
            this.setIsRecyclable(false)

            itemView.checkBox.isChecked = contactLabel.isSelected ==
                    ContactEmailGroupSelectionState.SELECTED

            itemView.checkBox.setOnCheckedChangeListener { view, isChecked ->
                contactLabel.isSelected = if (isChecked) {
                    ContactEmailGroupSelectionState.SELECTED
                } else {
                    ContactEmailGroupSelectionState.UNSELECTED
                }
            }

            val colorString = UiUtil.normalizeColor(contactLabel.color)
            val groupDrawable = itemView.groupColor.drawable
            groupDrawable.mutate()
            groupDrawable.setColorFilter(
                Color.parseColor(colorString),
                PorterDuff.Mode.SRC_IN
            )

            itemView.groupName.text = contactLabel.name
            val membersText = String.format(
                context.getString(R.string.contact_group_toolbar_title),
                "",
                contactLabel.contactEmailsCount
            )
            itemView.groupMembersNum.text = membersText

            itemView.setOnClickListener {
                itemView.checkBox.isChecked = !itemView.checkBox.isChecked

            }

        }
    }
}
