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

package ch.protonmail.android.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import ch.protonmail.android.R
import ch.protonmail.android.databinding.ViewCheckableButtonBinding
import com.google.android.material.button.MaterialButton

/**
 * A button that can have a Check icon
 * @inherit from [MaterialButton]
 */
class CheckableButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val button: ImageButton
    private val checkView: View

    init {
        val binding = ViewCheckableButtonBinding.inflate(
            LayoutInflater.from(context),
            this
        )
        button = binding.checkableButtonButton
        checkView = binding.checkableButtonCheck

        context.withStyledAttributes(attrs, R.styleable.CheckableButton, defStyleAttr) {
            val iconDrawable = getDrawable(R.styleable.CheckableButton_checkableButtonIcon)
            button.setImageDrawable(iconDrawable)
        }
    }

    fun isChecked(): Boolean =
        checkView.isVisible

    fun setChecked(checked: Boolean) {
        checkView.isVisible = checked
    }

    override fun hasOnClickListeners(): Boolean =
        button.hasOnClickListeners()

    override fun setOnClickListener(l: OnClickListener?) {
        button.setOnClickListener(l)
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        button.setOnLongClickListener(l)
    }
}
