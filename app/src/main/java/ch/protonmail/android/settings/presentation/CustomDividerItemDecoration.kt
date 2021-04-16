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

package ch.protonmail.android.settings.presentation

import android.content.Context
import android.graphics.Canvas
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.adapters.VIEW_TYPE_ITEM

class CustomDividerItemDecoration(context: Context?, orientation: Int) : DividerItemDecoration(context, orientation) {

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {

        for (i in 0 until parent.childCount - 1) {
            val view = parent.getChildAt(i)
            val viewHolder = parent.getChildViewHolder(view)
            val viewType = viewHolder.itemViewType

            // Draw divider only for view type 2 (can also put position here to remove for certain positions)
            if (viewType == VIEW_TYPE_ITEM) {
                val params = view?.layoutParams as RecyclerView.LayoutParams
                val top = view.bottom + params.bottomMargin
                val bottom = top + (drawable?.intrinsicHeight ?: 0)
                drawable?.setBounds(0, top, parent.right, bottom)
                drawable?.draw(canvas)
            } else continue
        }
    }
}
