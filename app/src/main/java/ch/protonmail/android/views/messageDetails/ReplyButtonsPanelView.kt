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
import ch.protonmail.android.core.Constants
import kotlinx.android.synthetic.main.panel_reply_buttons.view.*

/**
 * Created by Kamil Rajtar on 09.08.18.  */
class ReplyButtonsPanelView @JvmOverloads constructor(context: Context,
                                                      attrs: AttributeSet? = null,
                                                      defStyleAttr: Int = 0) : ConstraintLayout(
        context,
        attrs,
        defStyleAttr) {
    init {
        View.inflate(context, R.layout.panel_reply_buttons, this)
    }

    fun setOnMessageActionListener(messageActionListener: ((Constants.MessageActionType) -> Unit)?) {
        val actions = mapOf(
                reply to Constants.MessageActionType.REPLY,
                reply_all to Constants.MessageActionType.REPLY_ALL,
                forward to Constants.MessageActionType.FORWARD)

        actions.forEach { (view, actionId) ->
            view.setOnClickListener { messageActionListener?.invoke(actionId) }
        }
    }
}
