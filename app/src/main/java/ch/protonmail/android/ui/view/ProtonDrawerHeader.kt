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

package ch.protonmail.android.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.StyleRes
import ch.protonmail.android.R
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.utils.extensions.inflate
import kotlinx.android.synthetic.main.layout_drawer_header.view.*

internal class ProtonDrawerHeader @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        inflate(R.layout.layout_drawer_header, attachToRoot = true)
    }

    fun setUser(initials: Pair<Char, Char>, name: Name, email: EmailAddress) {
        val stringInitials = "${initials.first}${initials.second}"
        drawer_header_initials.text = stringInitials
        drawer_header_name.text = name.s
        drawer_header_email.text = email.s
    }
}
