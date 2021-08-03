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

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.databinding.GroupRecipientListItemBinding

class GroupRecipientViewHolder(binding: GroupRecipientListItemBinding):
    RecyclerView.ViewHolder(binding.root) {

    private val checkbox = binding.groupRecipientListItemCheckbox

    fun bind(recipient: MessageRecipient, clickListener: () -> Unit) {
        val name = recipient.name
        val address = recipient.emailAddress

        val spannableText = SpannableString("$name \n<$address>")
        spannableText.setSpan(
            StyleSpan(Typeface.BOLD), 0, name.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        checkbox.text = spannableText
        checkbox.isChecked = recipient.isSelected
        checkbox.setOnCheckedChangeListener { _, checked ->
            recipient.isSelected = checked
            clickListener()
        }
        itemView.tag = recipient
    }

    fun toggle() {
        if (checkbox.isChecked) uncheck() else check()
    }

    private fun check() {
        checkbox.isChecked = true
        (itemView.tag as MessageRecipient).isSelected = true
    }

    private fun uncheck() {
        checkbox.isChecked = false
        (itemView.tag as MessageRecipient).isSelected = false
    }
}
