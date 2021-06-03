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
import android.text.format.Formatter
import android.util.AttributeSet
import android.widget.Adapter
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import ch.protonmail.android.R
import kotlinx.android.synthetic.main.layout_message_details_attachments.view.*
import kotlinx.android.synthetic.main.layout_message_details_attachments.view.attachments_list_linear_layout
import me.proton.core.presentation.utils.inflate

/**
 * A view for attachments banner in message details
 */
class MessageDetailsAttachmentsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(R.layout.layout_message_details_attachments, true)
        setOnClickListener {
            expanded = !expanded
        }
    }

    private var expanded = false
        set(value) {
            attachments_list_linear_layout.isVisible = value
            chevron_image_view.setImageResource(if (value) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down)
            field = value
        }

    private var attachmentsAdapter: Adapter? = null
        set(value) {
            field?.unregisterDataSetObserver(dataSetObserver)
            value?.registerDataSetObserver(dataSetObserver)
            field = value
            dataSetObserver.onChanged()
        }

    private val dataSetObserver = object : DataSetObserver() {
        override fun onChanged() {
            attachmentsAdapter?.apply {
                attachments_list_linear_layout.removeAllViews()
                (0 until count).forEach {
                    attachments_list_linear_layout.addView(getView(it, null, attachments_list_linear_layout))
                }
            }
        }

        override fun onInvalidated() {
            attachments_list_linear_layout.removeAllViews()
        }
    }

    fun bind(attachmentsCount: Int, sizeOfAttachments: Long, attachmentsAdapter: Adapter) {
        val attachmentsText = resources.getQuantityString(
            R.plurals.attachments_number,
            attachmentsCount,
            attachmentsCount
        )
        val sizeText = context.getString(
            R.string.attachment_size,
            Formatter.formatShortFileSize(context, sizeOfAttachments)
        )
        attachments_text_view.text = "$attachmentsText $sizeText"
        this.attachmentsAdapter = attachmentsAdapter
    }
}
