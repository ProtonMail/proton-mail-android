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
package ch.protonmail.android.views.messageDetails

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Typeface
import android.text.format.Formatter
import android.util.AttributeSet
import android.view.View
import android.widget.Adapter
import androidx.constraintlayout.widget.ConstraintLayout
import ch.protonmail.android.R
import kotlinx.android.synthetic.main.view_attachments_message_details.view.*

class AttachmentsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_attachments_message_details, this)
        val typeface = Typeface.createFromAsset(context.assets, "protonmail-mobile-icons.ttf")
        attachment_title_icon.typeface = typeface
        setOnClickListener {
            expanded = !expanded
        }
    }

    var expanded = false
        set(value) {
            attachment_detail_list.visibility = if (value) View.VISIBLE else View.GONE
            attachments_toggle.setImageResource(if (value) R.drawable.triangle_up else R.drawable.triangle_down)
            field = value
        }

    var attachmentsAdapter: Adapter? = null
        set(value) {
            field?.unregisterDataSetObserver(dataSetObserver)
            value?.registerDataSetObserver(dataSetObserver)
            field = value
            dataSetObserver.onChanged()
        }

    private val dataSetObserver = object : DataSetObserver() {
        override fun onChanged() {
            attachmentsAdapter?.apply {
                attachment_detail_list.removeAllViews()
                (0 until count).forEach {
                    attachment_detail_list.addView(getView(it, null, attachment_detail_list))
                }

            }
        }

        override fun onInvalidated() {
            attachment_detail_list.removeAllViews()
        }
    }

    fun setTitle(attachmentsCount: Int, sizeOfAttachments: Long) {
        val attachmentsText = resources.getQuantityString(
            R.plurals.attachments_non_descriptive,
            attachmentsCount,
            attachmentsCount
        )
        val sizeText = context.getString(
            R.string.attachment_size,
            Formatter.formatShortFileSize(context, sizeOfAttachments)
        )
        attachment_title.text = "$attachmentsText $sizeText"
    }
}
