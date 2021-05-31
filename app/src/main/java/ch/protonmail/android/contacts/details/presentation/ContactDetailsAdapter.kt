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

package ch.protonmail.android.contacts.details.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsUiItem
import ch.protonmail.android.databinding.ListItemContactDetailBinding
import timber.log.Timber

class ContactDetailsAdapter(
    private val onEmailClicked: (String) -> Unit,
    private val onPhoneClickListener: (String) -> Unit,
    private val onAddressClicked: (String) -> Unit,
    private val onUrlClicked: (String) -> Unit
) : ListAdapter<ContactDetailsUiItem, RecyclerView.ViewHolder>(ContactDetailsDiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemContactDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val viewHolder = ContactDetailViewHolder(
            binding.textViewContactDetailsItemHeader,
            binding.textViewContactDetailsItem,
            binding.root
        )

        binding.textViewContactDetailsItem.setOnClickListener {
            val position = viewHolder.adapterPosition
            Timber.v("Item clicked $position")
            if (position != RecyclerView.NO_POSITION) {
                val item = getItem(viewHolder.adapterPosition)
                Timber.v("Item clicked $item")
                when (item) {
                    is ContactDetailsUiItem.Email -> onEmailClicked(item.value)
                    is ContactDetailsUiItem.TelephoneNumber -> onPhoneClickListener(item.value)
                    is ContactDetailsUiItem.Address ->
                        onAddressClicked("${item.street}, ${item.locality} ${item.country}")
                    is ContactDetailsUiItem.Url -> onUrlClicked(item.value)
                    else -> {
                        Timber.d("This detail type has no special click handling: $item")
                    }
                }
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ContactDetailViewHolder).bind(getItem(position))
    }

    private class ContactDetailViewHolder(
        val textViewContactDetailsItemHeader: TextView,
        val textViewContactDetailsItem: TextView,
        root: ConstraintLayout
    ) : RecyclerView.ViewHolder(root) {

        fun bind(item: ContactDetailsUiItem) {
            Timber.v("Bind $item")
            when (item) {
                is ContactDetailsUiItem.Email -> {
                    textViewContactDetailsItemHeader.text = if (item.type.isNotEmpty()) {
                        item.type
                    } else {
                        textViewContactDetailsItemHeader.context.getString(R.string.email)
                    }
                    textViewContactDetailsItem.apply {
                        text = item.value
                        setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_envelope_full, 0, 0, 0)
                        isClickable = true
                    }
                }
                is ContactDetailsUiItem.TelephoneNumber -> {
                    textViewContactDetailsItemHeader.text = if (item.type.isNotEmpty()) {
                        item.type
                    } else {
                        textViewContactDetailsItemHeader.context.getString(R.string.phone)
                    }
                    textViewContactDetailsItem.apply {
                        text = item.value
                        setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_contact_phone_dark, 0, 0, 0)
                        isClickable = true
                    }
                }
                is ContactDetailsUiItem.Address -> {
                    textViewContactDetailsItemHeader.text = if (item.type.isNotEmpty()) {
                        item.type
                    } else {
                        textViewContactDetailsItemHeader.context.getString(R.string.default_address)
                    }
                    textViewContactDetailsItem.apply {
                        text = getAddressToDisplay(item)
                        setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_map_marker, 0, 0, 0)
                        isClickable = true
                    }
                }
                is ContactDetailsUiItem.Organization -> {
                    textViewContactDetailsItemHeader.setText(R.string.vcard_other_option_org)
                    textViewContactDetailsItem.apply {
                        text = item.values.toString()
                        isClickable = false
                    }
                }
                is ContactDetailsUiItem.Title -> {
                    textViewContactDetailsItemHeader.setText(R.string.vcard_other_option_title)
                    textViewContactDetailsItem.apply {
                        text = item.value
                        isClickable = false
                    }
                }
                is ContactDetailsUiItem.Nickname -> {
                    textViewContactDetailsItemHeader.text = if (item.type.isNotEmpty()) {
                        item.type
                    } else {
                        textViewContactDetailsItemHeader.context.getString(R.string.vcard_other_option_nickname)
                    }
                    textViewContactDetailsItem.apply {
                        text = item.value
                        isClickable = false
                    }
                }
                is ContactDetailsUiItem.Birthday -> {
                    textViewContactDetailsItemHeader.setText(R.string.vcard_other_option_birthday)
                    textViewContactDetailsItem.apply {
                        text = item.birthdayDate
                        isClickable = false
                    }
                }
                is ContactDetailsUiItem.Anniversary -> {
                    textViewContactDetailsItemHeader.setText(R.string.vcard_other_option_anniversary)
                    textViewContactDetailsItem.apply {
                        text = item.anniversaryDate
                        isClickable = false
                    }
                }
                is ContactDetailsUiItem.Role -> {
                    textViewContactDetailsItemHeader.setText(R.string.vcard_other_option_role)
                    textViewContactDetailsItem.apply {
                        text = item.value
                        isClickable = false
                    }
                }
                is ContactDetailsUiItem.Url -> {
                    textViewContactDetailsItemHeader.setText(R.string.vcard_other_option_url)
                    textViewContactDetailsItem.apply {
                        text = item.value
                        isClickable = true
                    }
                }
                is ContactDetailsUiItem.Gender -> {
                    textViewContactDetailsItemHeader.setText(R.string.vcard_other_option_gender)
                    textViewContactDetailsItem.apply {
                        text = item.value
                        isClickable = false
                    }
                }
            }
        }

        private fun getAddressToDisplay(item: ContactDetailsUiItem.Address): String =
            "${item.street}, ${item.locality} ${item.region} ${item.postalCode} ${item.country}"
    }
}
