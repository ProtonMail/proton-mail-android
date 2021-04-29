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
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import ch.protonmail.android.R
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.ServerTime
import kotlinx.android.synthetic.main.layout_message_details_expiration_info.view.*
import me.proton.core.presentation.utils.inflate
import java.util.concurrent.TimeUnit

/**
 * A view for expiration info banner in message details
 */
class MessageDetailsExpirationInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(R.layout.layout_message_details_expiration_info, true)
    }

    fun bind(expirationTime: Long) {
        if (expirationTime > 0) {
            val remainingSeconds = expirationTime - TimeUnit.MILLISECONDS.toSeconds(ServerTime.currentTimeMillis())
            expirationInfoTextView.text = String.format(
                resources.getString(R.string.message_expires_in),
                DateUtil.formatDaysAndHours(context, remainingSeconds)
            )
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }

}
