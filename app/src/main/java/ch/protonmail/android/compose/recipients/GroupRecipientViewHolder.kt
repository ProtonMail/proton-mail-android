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
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.utils.extensions.showToast
import kotlinx.android.synthetic.main.group_recipient_list_item.view.*

/**
 * Created by kadrikj on 9/19/18. */
class GroupRecipientViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(recipient: MessageRecipient, clickListener: () -> Unit) {
        val name = recipient.name
        val address = recipient.address

        itemView.pgpIcon.typeface =
                Typeface.createFromAsset(this.itemView.context.assets, "pgp-icons-android.ttf")

            if (recipient.icon != 0) {
                itemView.pgpIcon.visibility = View.VISIBLE
                itemView.pgpIcon.text = this.itemView.context.getString(recipient.icon)
            }
            if (recipient.iconColor != 0) {
                itemView.pgpIcon.setTextColor(
                    ContextCompat.getColor(
                        this.itemView.context,
                        recipient.iconColor
                    )
                )
            }

            itemView.pgpIcon.setOnClickListener {
                if (recipient.description != 0) run {
                    itemView.context.showToast(recipient.description, Toast.LENGTH_SHORT)
                }
            }

        val spannableText = SpannableString("$name \n<$address>")
        spannableText.setSpan(
            StyleSpan(Typeface.BOLD), 0, name.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        itemView.recipient.text = spannableText
        itemView.check.isChecked = recipient.isSelected
        itemView.check.setOnCheckedChangeListener { _, checked ->
            recipient.isSelected = checked
            clickListener()
        }
        itemView.tag = recipient
    }

    fun toggle() {
        if (itemView.check.isChecked) uncheck() else check()
    }

    private fun check() {
        itemView.check.isChecked = true
        (itemView.tag as MessageRecipient).isSelected = true
    }

    private fun uncheck() {
        itemView.check.isChecked = false
        (itemView.tag as MessageRecipient).isSelected = false
    }
}
