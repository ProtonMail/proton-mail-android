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
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.utils.extensions.isInPreviewMode
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxItemDecoration
import com.google.android.flexbox.FlexboxLayoutManager
import me.proton.core.presentation.ui.adapter.ProtonAdapter

/**
 * Displays, on a multiple lines, all the labels not truncated.
 *
 * This View is supposed to be "static", in a way that it won't change form, for cases where the View can be expanded,
 *  like for message details, an ExpandableLabelChipGroupView ( not implemented yet ) must be used instead.
 */
class MultiLineLabelChipGroupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val labelsAdapter = ProtonAdapter(
        getView = { _, _ -> LabelChipView(context) },
        onBind = { setLabel(it) },
        diffCallback = DiffCallback()
    )

    private val dividerItemDecoration = FlexboxItemDecoration(context)
        .apply {
            val drawable = checkNotNull(ContextCompat.getDrawable(context, R.drawable.spacer_s_m))
            setDrawable(drawable)
        }

    init {
        val recyclerView = RecyclerView(context).apply {
            id = R.id.multi_line_label_recycler_view
            layoutManager = FlexboxLayoutManager(context, FlexDirection.ROW)
            addItemDecoration(dividerItemDecoration)
            adapter = labelsAdapter
        }
        addView(recyclerView)

        if (isInPreviewMode())
            setLabels(buildPreviewItems())
    }

    fun setLabels(labels: List<LabelChipUiModel>) {
        isVisible = labels.isNotEmpty()
        labelsAdapter.submitList(labels)
    }

    private class DiffCallback : DiffUtil.ItemCallback<LabelChipUiModel>() {

        override fun areItemsTheSame(oldItem: LabelChipUiModel, newItem: LabelChipUiModel) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: LabelChipUiModel, newItem: LabelChipUiModel) =
            oldItem == newItem

    }

    private companion object {

        /**
         * @return List of Labels for build a Preview
         */
        fun buildPreviewItems(): List<LabelChipUiModel> = listOf(
            LabelChipUiModel(LabelId("a"), Name("long name for first label"), Color.RED),
            LabelChipUiModel(LabelId("b"), Name("second label"), Color.GREEN),
            LabelChipUiModel(LabelId("c"), Name("third"), Color.BLUE),
            LabelChipUiModel(LabelId("d"), Name("long name for forth label"), Color.CYAN),
            LabelChipUiModel(LabelId("e"), Name("fifth label"), Color.MAGENTA),
            LabelChipUiModel(LabelId("f"), Name("sixth"), Color.GRAY),
            LabelChipUiModel(LabelId("g"), Name("long name for seventh label"), Color.BLACK),
        )
    }

}
