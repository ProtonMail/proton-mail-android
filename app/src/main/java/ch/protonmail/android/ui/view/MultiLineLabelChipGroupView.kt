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
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.domain.entity.Name
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
        diffCallback = LabelChipUiModel.DiffCallback
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

    private companion object {

        /**
         * @return List of Labels for build a Preview
         */
        fun buildPreviewItems(): List<LabelChipUiModel> = listOf(
            LabelChipUiModel(UserId("a"), Name("long name for first label"), Color.RED),
            LabelChipUiModel(UserId("b"), Name("second label"), Color.GREEN),
            LabelChipUiModel(UserId("c"), Name("third"), Color.BLUE),
            LabelChipUiModel(UserId("d"), Name("long name for forth label"), Color.CYAN),
            LabelChipUiModel(UserId("e"), Name("fifth label"), Color.MAGENTA),
            LabelChipUiModel(UserId("f"), Name("sixth"), Color.GRAY),
            LabelChipUiModel(UserId("g"), Name("long name for seventh label"), Color.BLACK),
        )
    }
}
