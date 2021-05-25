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

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.compose.presentation.model.ComposerAttachmentUiModel
import ch.protonmail.android.utils.extensions.getDrawableOrThrow
import me.proton.core.presentation.ui.adapter.ProtonAdapter

/**
 * Displays a list of attachments for the Composer
 * @see ComposerAttachmentUiModel
 */
class ComposerAttachmentsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private lateinit var onRemoveClicked: (Uri) -> Unit
    private val attachmentsAdapter by lazy {
        ProtonAdapter(
            getView = { _, _ -> ComposerAttachmentItemView(context) },
            onBind = { setAttachment(it, onRemoveClicked) },
            diffCallback = ComposerAttachmentUiModel.DiffCallback
        )
    }

    private val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        .apply {
            val drawable = context.getDrawableOrThrow(R.drawable.spacer_m)
            setDrawable(drawable)
        }

    private val recyclerView = RecyclerView(context).apply {
        id = R.id.composer_attachments_recycler_view
        layoutManager = LinearLayoutManager(context)
        adapter = attachmentsAdapter
        addItemDecoration(dividerItemDecoration)

        isNestedScrollingEnabled = false
    }

    init {
        addView(recyclerView)
    }

    fun setAttachments(attachments: List<ComposerAttachmentUiModel>, onRemoveClicked: (Uri) -> Unit) {
        this.onRemoveClicked = onRemoveClicked
        attachmentsAdapter.submitList(attachments)

        if (recyclerView.adapter == null)
            recyclerView.adapter = attachmentsAdapter
    }
}
