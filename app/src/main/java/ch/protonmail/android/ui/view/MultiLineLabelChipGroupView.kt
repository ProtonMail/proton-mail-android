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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import me.proton.core.presentation.ui.adapter.ProtonAdapter

/**
 * Displays, on a multiple lines, all the labels not truncated.
 *
 * This View is supposed to be "static", in a way that it won't change form, for cases where the View can be expanded,
 *  like for message details, an ExpandableLabelChipGroupView ( not implemented yet ) must be used instead.
 */
class MultiLineLabelChipGroupView @JvmOverloads constructor (
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

    init {
        val recyclerView = RecyclerView(context).apply {
            id = RECYCLER_VIEW_ID
            layoutManager = FlexboxLayoutManager(context, FlexDirection.ROW)
            adapter = labelsAdapter
        }
        addView(recyclerView)
    }

    fun setLabels(labels: List<LabelChipUiModel>) {
        labelsAdapter.submitList(labels)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    companion object {

        const val RECYCLER_VIEW_ID = 26_894
    }
}
