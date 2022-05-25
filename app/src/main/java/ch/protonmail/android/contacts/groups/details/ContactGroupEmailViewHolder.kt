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
package ch.protonmail.android.contacts.groups.details

import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.contacts.groups.GroupsItemAdapterMode
import ch.protonmail.android.data.local.model.ContactEmail
import kotlinx.android.synthetic.main.contact_groups_email_list_item_selectable.view.*
import java.util.Locale

class ContactGroupEmailViewHolder(view: View, private val mode: GroupsItemAdapterMode) : RecyclerView.ViewHolder(view) {

    fun bind(contactEmail: ContactEmail) {
        var name = contactEmail.name
        itemView.name.text = name
        itemView.email.text = contactEmail.email
        itemView.tag = contactEmail
        if (TextUtils.isEmpty(name)) {
            name = contactEmail.email
        }
        if (null != name && !TextUtils.isEmpty(name) && name.length >= 2) {
            itemView.mailAvatar.bind(
                isSelectedActive = false,
                isMultiselectActive = false,
                initials = name.substring(0, 2).toUpperCase(Locale.getDefault())
            )
        }
        itemView.apply {
            if (mode == GroupsItemAdapterMode.CHECKBOXES) {
                check.visibility = View.VISIBLE
                check.isChecked = contactEmail.selected
            } else if (mode == GroupsItemAdapterMode.NORMAL) {
                check.visibility = View.GONE
            }
        }
    }

    fun toggle() {
        if (mode != GroupsItemAdapterMode.CHECKBOXES) {
            return
        }
        if (itemView.check.isChecked) uncheck() else check()
    }

    private fun check() {
        itemView.check.isChecked = true
        (itemView.tag as ContactEmail).selected = true
    }

    private fun uncheck() {
        itemView.check.isChecked = false
        (itemView.tag as ContactEmail).selected = false
    }
}
