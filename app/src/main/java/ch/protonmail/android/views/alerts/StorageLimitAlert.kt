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
package ch.protonmail.android.views.alerts

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import ch.protonmail.android.R
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import kotlinx.android.synthetic.main.storage_limit_layout_view.view.*

class StorageLimitAlert @JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.storage_limit_layout_view, this, true)
        this.setOnClickListener {
            DialogUtils.showInfoDialogWithTwoButtons(
                context,
                context.getString(R.string.storage_limit_warning_title),
                context.getString(R.string.storage_limit_reached_text),
                context.getString(R.string.learn_more),
                context.getString(R.string.okay),
                { unit ->
                    val browserIntent =
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.limit_reached_learn_more)))
                    context.startActivity(browserIntent)
                },
                {
                },
                true
            )
        }
    }

    fun setText(text: String) {
        textStorageLimit.text = text
    }

    fun setIcon(image: Drawable) {
        imageStorageLimit.setImageDrawable(image)
    }
}
