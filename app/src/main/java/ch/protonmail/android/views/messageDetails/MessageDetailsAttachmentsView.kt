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
package ch.protonmail.android.views.messageDetails

import android.content.Context
import android.database.DataSetObserver
import android.text.format.Formatter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Adapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import ch.protonmail.android.R
import ch.protonmail.android.databinding.LayoutMessageDetailsAttachmentsBinding

/**
 * A view for attachments banner in message details
 */
class MessageDetailsAttachmentsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val attachmentsTextView: TextView
    private val chevronImageView: ImageView
    private val attachmentsListLinearLayout: LinearLayout

    init {
        val binding = LayoutMessageDetailsAttachmentsBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
        attachmentsTextView = binding.attachmentsTextView
        chevronImageView = binding.chevronImageView
        attachmentsListLinearLayout = binding.attachmentsListLinearLayout

        setOnClickListener {
            expanded = !expanded
        }
    }

    private var expanded = false
        set(value) {
            attachmentsListLinearLayout.isVisible = value
            chevronImageView.setImageResource(if (value) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down)
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
                attachmentsListLinearLayout.removeAllViews()
                (0 until count).forEach {
                    attachmentsListLinearLayout.addView(getView(it, null, attachmentsListLinearLayout))
                }
            }
        }

        override fun onInvalidated() {
            attachmentsListLinearLayout.removeAllViews()
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
        attachmentsTextView.text = "$attachmentsText $sizeText"
        this.attachmentsAdapter = attachmentsAdapter
    }
}
