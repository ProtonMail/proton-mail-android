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
package ch.protonmail.android.views.contactsList

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import ch.protonmail.android.R
import kotlinx.android.synthetic.main.contact_group_email_avatar.view.*

/**
 * Created by kadrikj on 9/10/18. */
class ContactGroupEmailAvatarView(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    constructor(
            context: Context,
            attrs: AttributeSet? = null
    ): this (context, attrs, 0)

    constructor(context: Context): this(context, null, 0)

    init {
        inflate(context, R.layout.contact_group_email_avatar, this)
    }

    //todo: add support for images
    fun setDrawable() {
        emailAvatar.visibility = View.VISIBLE
        emailLetters.visibility = View.GONE
    }

    fun setLetters(letters: String) {
        if (TextUtils.isEmpty(letters) || letters.length > 2) {
            return
        }
        emailAvatar.visibility = View.GONE
        emailLetters.visibility = View.VISIBLE
        emailLetters.text = letters.toUpperCase()
        emailLetters.background.setColorFilter(
                resources.getColor(R.color.chateau_gray),
                PorterDuff.Mode.SRC_IN
        )
    }

    fun setColorFilter(colorString: String) {
        if (TextUtils.isEmpty(colorString)) {
            return
        }
        emailLetters.background.setColorFilter(
                Color.parseColor(colorString),
                PorterDuff.Mode.SRC_IN
        )
    }
}