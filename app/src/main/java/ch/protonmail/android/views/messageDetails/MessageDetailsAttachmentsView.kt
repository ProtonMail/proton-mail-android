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
import android.text.format.Formatter
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import ch.protonmail.android.R
import kotlinx.android.synthetic.main.layout_message_details_attachments.view.*
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
            // TODO: To be implemented with MAILAND-1545
        }
    }

    fun bind(attachmentsCount: Int, sizeOfAttachments: Long) {
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
    }
}
