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

package ch.protonmail.android.details.presentation.view

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import ch.protonmail.android.R
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showInfoDialog
import kotlinx.android.synthetic.main.layout_message_details_scheduled_info.view.*
import me.proton.core.presentation.utils.inflate

class ScheduledInfoBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(R.layout.layout_message_details_scheduled_info, true)

        editButton.setOnClickListener {

            showInfoDialog(
                context,
                "",
                context.getString(R.string.scheduled_message_edit_info)
            ) { }

        }

    }

    fun bind(showInfo: Boolean, dateScheduled: String) {
        isVisible = showInfo
        scheduledInfoTextView.text = String.format(
            context.getString(R.string.scheduled_message_info),
            dateScheduled
        )
    }
}
