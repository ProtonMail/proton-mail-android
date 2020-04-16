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
import android.graphics.Typeface
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import android.text.format.Formatter
import android.util.AttributeSet
import android.view.View
import ch.protonmail.android.R
import kotlinx.android.synthetic.main.view_attachment_detail.view.*

/**
 * Created by Kamil Rajtar on 10.08.18.  */
class AttachmentDetailView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
	: ConstraintLayout(context, attrs, defStyleAttr) {
	init {
		inflate(context, R.layout.view_attachment_detail, this)
		val typefacePgp = Typeface.createFromAsset(context.assets, "pgp-icons-android.ttf")
		attachment_modifier.typeface = typefacePgp
		attachment_modifier.setText(R.string.pgp_lock_open)
		attachment_modifier.setTextColor(ContextCompat.getColor(context, R.color.icon_warning))
	}

	fun bind(fileName: CharSequence?,
			 fileSize: Long,
			 attachmentSpecificIcon: Int,
			 showWarningIcon: Boolean,
			 downloading: Boolean) {
		attachment_name.text = fileName
		val formattedSize = Formatter.formatShortFileSize(context, fileSize)
		attachment_size.text = context.getString(R.string.attachment_size, formattedSize)
		attachment_specific_icon.setImageResource(attachmentSpecificIcon)
		attachment_modifier.visibility = if (showWarningIcon) View.VISIBLE else View.GONE
		attachment_download_progress.visibility = if (downloading) View.VISIBLE else View.GONE
	}
}
