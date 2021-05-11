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
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import ch.protonmail.android.R
import kotlinx.android.synthetic.main.view_load_content_button.view.*

class LoadContentButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    init {
        inflate(context, R.layout.view_load_content_button, this)
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LoadContentButton,
            0, 0
        ).let {

            try {
                loadContentButton.text = it.getString(R.styleable.LoadContentButton_text)
                loadContentButton.setOnClickListener {
                    this.callOnClick()
                }
            } finally {
                it.recycle()
            }
        }
    }
}
