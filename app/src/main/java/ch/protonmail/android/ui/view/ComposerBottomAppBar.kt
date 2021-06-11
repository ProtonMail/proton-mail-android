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
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import ch.protonmail.android.databinding.LayoutComposerBottomAppBarBinding
import ch.protonmail.libs.core.utils.onClick

class ComposerBottomAppBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val passwordButton: CheckableButton
    private val expirationButton: CheckableButton
    private val attachmentsButton: ImageButton
    private val attachmentsCountTextView: TextView

    init {
        val binding = LayoutComposerBottomAppBarBinding.inflate(
            LayoutInflater.from(context),
            this
        )

        passwordButton = binding.composerPasswordButton
        expirationButton = binding.composerExpirationButton
        attachmentsButton = binding.composerAttachmentsButton
        attachmentsCountTextView = binding.composerAttachmentsCountTextView
    }

    fun hasPassword(): Boolean =
        passwordButton.isChecked()

    fun setHasPassword(hasPassword: Boolean) {
        passwordButton.setChecked(hasPassword)
    }

    fun hasExpiration(): Boolean =
        expirationButton.isChecked()

    fun setHasExpiration(hasExpiration: Boolean) {
        expirationButton.setChecked(hasExpiration)
    }

    fun hasAttachments(): Boolean =
        attachmentsCountTextView.isVisible

    fun setAttachmentsCount(count: Int) {
        attachmentsCountTextView.text = "$count"
        attachmentsCountTextView.isVisible = count > 0
    }

    fun onPasswordClick(block: () -> Unit) {
        passwordButton.onClick(block)
    }

    fun onExpirationClick(block: () -> Unit) {
        expirationButton.onClick(block)
    }

    fun onAttachmentsClick(block: () -> Unit) {
        attachmentsButton.onClick(block)
    }
}
