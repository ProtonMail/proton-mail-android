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

package ch.protonmail.android.compose.presentation.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.core.view.isVisible
import ch.protonmail.android.R
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.contacts.domain.usecase.ExtractInitials
import ch.protonmail.android.databinding.LayoutRecipientDropdownItemBinding
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Name
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.containsNoCase
import timber.log.Timber
import kotlin.collections.filter as kFilter

/**
 * Array adapter for [MessageRecipient]
 * This will filter our contacts with email address that doesn't match [EmailAddress.VALIDATION_REGEX]
 */
class MessageRecipientArrayAdapter(context: Context) :
    ArrayAdapter<MessageRecipient>(context, R.layout.layout_recipient_dropdown_item) {

    private var data = emptyList<MessageRecipient>()

    fun setData(recipients: List<MessageRecipient>) {
        data = recipients
        clear()
        addAll(recipients)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflateView(parent)
        val viewHolder = view.tag as ViewHolder

        getItem(position)?.let(viewHolder::bind)

        return view
    }

    private fun inflateView(parent: ViewGroup): View {
        val binding = LayoutRecipientDropdownItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return binding.root.apply {
            tag = ViewHolder(binding)
        }
    }

    override fun getFilter() = object : Filter() {

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return if (constraint.isNullOrBlank()) {
                // No filter implemented we return all the recipients with valid emails
                data.filterValidEmails().toFilterResults()
            } else {
                data.filterValidEmails().kFilter { messageRecipient ->
                    messageRecipient.name containsNoCase constraint ||
                        messageRecipient.emailAddress containsNoCase constraint
                }.toFilterResults()
            }
        }

        private fun List<MessageRecipient>.filterValidEmails() = kFilter {
            val isGroup = context.getString(R.string.members) in it.name
            isGroup || EmailAddress.VALIDATION_REGEX.matches(it.emailAddress)
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            clear()
            if (results.count > 0) {
                @Suppress("UNCHECKED_CAST")
                addAll(results.values as List<MessageRecipient>)
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }

        private fun List<MessageRecipient>.toFilterResults(): FilterResults =
            FilterResults().apply {
                values = this@toFilterResults
                count = size
            }
    }

    private class ViewHolder(private val binding: LayoutRecipientDropdownItemBinding) {

        fun bind(recipient: MessageRecipient) {
            val isGroup = binding.root.context.getString(R.string.members) in recipient.name
            val isContact = isGroup.not()

            with(binding) {
                contactInitialsTextView.isVisible = isContact
                contactEmailTextView.isVisible = isContact
                contactGroupIconImageView.isVisible = isGroup

                binding.contactNameTextView.text = recipient.name
                if (isContact) {

                    contactInitialsTextView.text = recipient.initials
                    contactEmailTextView.text = recipient.emailAddress
                } else if (isGroup) {
                    contactGroupIconImageView.backgroundTintList =
                        ColorStateList.valueOf(recipient.groupColor)
                    contactGroupIconImageView.imageTintList =
                        ColorStateList.valueOf(root.context.getColor(R.color.icon_inverted))
                }
            }
        }

        private val MessageRecipient.initials: String get() = try {
            val extractInitials = ExtractInitials()
            extractInitials(Name(name), EmailAddress(emailAddress))
        } catch (e: IllegalArgumentException) {
            EMPTY_STRING
        }
    }
}
