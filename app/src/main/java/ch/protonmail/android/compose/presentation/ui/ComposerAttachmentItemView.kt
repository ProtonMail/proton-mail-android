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

package ch.protonmail.android.compose.presentation.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.setPadding
import ch.protonmail.android.R
import ch.protonmail.android.compose.presentation.model.ComposerAttachmentUiModel
import ch.protonmail.android.databinding.ViewComposerAttachmentItemBinding
import ch.protonmail.libs.core.utils.onClick

/**
 * View for a single ComposerAttachmentsUiModel
 */
class ComposerAttachmentItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val iconImageView: ImageView
    private val nameTextView: TextView
    private val extensionTextView: TextView
    private val sizeTextView: TextView
    private val removeButton: ImageButton

    private val readyTextColor: Int =
        context.getColor(R.color.text_norm)

    private val notReadyTextColor: Int =
        context.getColor(R.color.text_weak)

    init {
        val binding = ViewComposerAttachmentItemBinding.inflate(
            LayoutInflater.from(context),
            this
        )

        iconImageView = binding.composerAttachmentIconImageView
        nameTextView = binding.composerAttachmentNameTextView
        extensionTextView = binding.composerAttachmentExtensionTextView
        sizeTextView = binding.composerAttachmentSizeTextView
        removeButton = binding.composerAttachmentRemoveButton

        binding.root.apply {
            setPadding(resources.getDimensionPixelSize(R.dimen.padding_m))
            setBackgroundResource(R.drawable.shape_rectangle_xsmall_corners_weak_outline)
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    fun setAttachment(attachment: ComposerAttachmentUiModel, onRemoveClicked: (id: Uri) -> Unit) {
        when (attachment) {
            is ComposerAttachmentUiModel.Data -> setData(attachment)
            is ComposerAttachmentUiModel.Idle -> setLoadingIcon()
            is ComposerAttachmentUiModel.NoFileInfo -> setErrorIcon()
        }
        removeButton.onClick { onRemoveClicked(attachment.id) }
    }

    @SuppressLint("SetTextI18n")
    private fun setData(data: ComposerAttachmentUiModel.Data) {
        nameTextView.text = data.displayName
        extensionTextView.text = ".${data.extension}"
        sizeTextView.text = data.size.formatToMegabytesString(floatingPoints = 1)

        fun setReady() {
            iconImageView.setImageResource(data.icon.drawableId)
            nameTextView.setTextColor(readyTextColor)
            extensionTextView.setTextColor(readyTextColor)
        }

        fun setNotReady() {
            nameTextView.setTextColor(notReadyTextColor)
            extensionTextView.setTextColor(notReadyTextColor)
        }

        when (data.state) {
            ComposerAttachmentUiModel.State.Ready -> {
                setReady()
            }
            ComposerAttachmentUiModel.State.Importing -> {
                setNotReady()
                setLoadingIcon()
            }
            ComposerAttachmentUiModel.State.Error -> {
                setNotReady()
                setErrorIcon()
            }
        }
    }

    private fun setLoadingIcon() {
        // TODO set loading icon
    }

    private fun setErrorIcon() {
        // TODO set error icon
    }
}
