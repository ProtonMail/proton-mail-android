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
import android.text.format.Formatter
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import ch.protonmail.android.R
import kotlinx.android.synthetic.main.layout_message_details_attachments_details.view.*

class AttachmentDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    init {
        inflate(context, R.layout.layout_message_details_attachments_details, this)
    }

    fun bind(
        fileName: CharSequence?,
        fileSize: Long,
        attachmentSpecificIcon: Int,
        showWarningIcon: Boolean,
        downloading: Boolean
    ) {
        attachment_name_text_view.text = fileName
        val formattedSize = Formatter.formatShortFileSize(context, fileSize)
        attachment_size_text_view.text = context.getString(R.string.attachment_size, formattedSize)
        attachment_name_text_view.setCompoundDrawablesRelativeWithIntrinsicBounds(attachmentSpecificIcon, 0, 0, 0)
        attachment_modifier_text_view.isVisible = showWarningIcon
        attachment_download_progress.isVisible = downloading
        attachment_download_image_view.visibility = if (downloading) View.INVISIBLE else View.VISIBLE
    }
}
