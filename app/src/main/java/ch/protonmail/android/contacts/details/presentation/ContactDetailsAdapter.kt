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
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
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
                is ContactDetailsUiItem.Group -> setGroupData(item)
                is ContactDetailsUiItem.Email -> setEmailData(item)
                is ContactDetailsUiItem.TelephoneNumber -> setPhoneData(item)
                is ContactDetailsUiItem.Address -> setAddressData(item)
                is ContactDetailsUiItem.Organization -> {
                    setBasicItemData(
                        R.string.vcard_other_option_org,
                        item.values.joinToString(" - "),
                        false
                    )
                }
                is ContactDetailsUiItem.Title -> {
                    setBasicItemData(
                        R.string.vcard_other_option_title,
                        item.value,
                        false
                    )
                }
                is ContactDetailsUiItem.Nickname -> setNickNameData(item)
                is ContactDetailsUiItem.Birthday -> {
                    setBasicItemData(
                        R.string.vcard_other_option_birthday,
                        item.birthdayDate,
                        false
                    )
                }
                is ContactDetailsUiItem.Anniversary -> {
                    setBasicItemData(
                        R.string.vcard_other_option_anniversary,
                        item.anniversaryDate,
                        false
                    )
                }
                is ContactDetailsUiItem.Role -> {
                    setBasicItemData(
                        R.string.vcard_other_option_role,
                        item.value,
                        false
                    )
                }
                is ContactDetailsUiItem.Url -> {
                    setBasicItemData(
                        R.string.vcard_other_option_url,
                        item.value,
                        true
                    )
                }
                is ContactDetailsUiItem.Gender -> {
                    setBasicItemData(
                        R.string.vcard_other_option_gender,
                        item.value,
                        false
                    )
                }
                is ContactDetailsUiItem.Note -> {
                    textViewContactDetailsItemHeader.setText(R.string.contact_vcard_note)
                    textViewContactDetailsItem.apply {
                        text = item.value
                        setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_note, 0, 0, 0)
                        isClickable = false
                    }
                }
            }
        }

        private fun setGroupData(item: ContactDetailsUiItem.Group) {
            textViewContactDetailsItemHeader.setText(R.string.groups)
            textViewContactDetailsItemHeader.isVisible = item.groupIndex == 0

            textViewContactDetailsItem.apply {
                text = item.name
                val iconDrawable =
                    ResourcesCompat.getDrawable(resources, R.drawable.circle_labels_selection, null)
                        ?.mutate()
                iconDrawable?.setTint(item.colorInt)
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    iconDrawable,
                    null,
                    null,
                    null
                )
                isClickable = false
            }
        }

        private fun setEmailData(item: ContactDetailsUiItem.Email) {
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

        private fun setPhoneData(
            item: ContactDetailsUiItem.TelephoneNumber
        ) {
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

        private fun setAddressData(
            item: ContactDetailsUiItem.Address
        ) {
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

        private fun setNickNameData(
            item: ContactDetailsUiItem.Nickname
        ) {
            textViewContactDetailsItemHeader.text = if (item.type.isNullOrBlank().not()) {
                item.type
            } else {
                textViewContactDetailsItemHeader.context.getString(R.string.vcard_other_option_nickname)
            }
            textViewContactDetailsItem.apply {
                text = item.value
                isClickable = false
            }
        }

        private fun setBasicItemData(
            @StringRes titleRes: Int,
            textToDisplay: String?,
            shouldBeClickable: Boolean
        ) {
            textViewContactDetailsItemHeader.setText(titleRes)
            textViewContactDetailsItem.apply {
                text = textToDisplay
                isClickable = shouldBeClickable
            }
        }

        private fun getAddressToDisplay(item: ContactDetailsUiItem.Address): String = with(item) {
            buildString {
                street?.let { append("$it\n") }
                extendedStreet?.let { append("$it\n") }
                postalCode?.let { append("$it\n") }
                locality?.let { append("$it\n") }
                poBox?.let { append("$it\n") }
                region?.let { append("$it\n") }
                country?.let(::append)
            }
        }
    }
}
