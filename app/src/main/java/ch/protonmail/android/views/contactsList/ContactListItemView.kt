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
package ch.protonmail.android.views.contactsList

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ch.protonmail.android.R
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.contacts.list.listView.ContactItem
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.getColorCompat
import ch.protonmail.android.utils.extensions.showToast
import kotlinx.android.synthetic.main.contacts_v2_list_item.view.*
import kotlinx.android.synthetic.main.contacts_v2_list_item_header.view.*

sealed class ContactListItemView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    abstract fun bind(item: ContactItem)

    class ContactsHeaderView(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : ContactListItemView(context, attrs, defStyleAttr) {

        init {
            inflate(context, R.layout.contacts_v2_list_item_header, this)
        }

        override fun bind(item: ContactItem) {
            val titleId = if (item.isProtonMailContact)
                R.string.protonmail_contacts
            else
                R.string.device_contacts
            header_title.text = context.getString(titleId)
        }
    }

    class ContactView(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : ContactListItemView(context, attrs, defStyleAttr) {

        private val emptyEmailList by lazy { context.getString(R.string.empty_email_list) }

        private val selectedColor by lazy {
            ContextCompat.getColor(context, R.color.white)
        }

        init {
            inflate(context, R.layout.contacts_v2_list_item, this)
            contactIcon.apply {
                isClickable = false
                setImageResource(R.drawable.ic_contacts_checkmark)
                setBackgroundResource(R.drawable.bg_circle)
                drawable.setColorFilter(
                    ContextCompat.getColor(context, R.color.contact_action),
                    PorterDuff.Mode.SRC_IN
                )
            }
        }

        private fun handleSelectionUi(isSelected: Boolean) {
            contactIcon.isVisible = isSelected
            contactIconLetter.isVisible = !isSelected
            rowWrapper.setBackgroundColor(
                if (isSelected) context.getColorCompat(R.color.selectable_color)
                else selectedColor
            )
        }

        override fun bind(item: ContactItem) {
            local_contact_icon.visibility =
                if (item.isProtonMailContact) View.GONE else View.VISIBLE

            contact_name.text = item.getName()
            contactIconLetter.text = UiUtil.extractInitials(item.getName())
            val contactEmails =
                if (item.getEmail().isEmpty()) {
                    emptyEmailList
                } else {
                    val additionalEmailsText = if (item.additionalEmailsCount > 0)
                        ", +" + item.additionalEmailsCount.toString()
                    else
                        ""
                    item.getEmail() + additionalEmailsText
                }
            contact_subtitle.text = contactEmails
            writeButton.setOnClickListener {
                val emailValue = item.getEmail()
                if (!TextUtils.isEmpty(emailValue)) {
                    val intent = AppUtil.decorInAppIntent(Intent(context, ComposeMessageActivity::class.java))
                    intent.putExtra(
                        ComposeMessageActivity.EXTRA_TO_RECIPIENTS, arrayOf(item.getEmail())
                    )
                    context.startActivity(intent)
                } else {
                    context.showToast(R.string.email_empty, Toast.LENGTH_SHORT)
                }
            }
            handleSelectionUi(item.isChecked)
        }
    }
}
