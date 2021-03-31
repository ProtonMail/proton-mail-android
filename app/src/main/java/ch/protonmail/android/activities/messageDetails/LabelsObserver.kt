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
package ch.protonmail.android.activities.messageDetails

import android.graphics.Color
import android.util.TypedValue
import android.view.View
import androidx.lifecycle.Observer
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.views.messageDetails.FolderIconView
import ch.protonmail.android.views.messagesList.ItemLabelMarginlessSmallView

// TODO change to immutable list after changing all uses to kotlin

/**
 * A class that observes changes in message labels
 */
class LabelsObserver(
    private val adapter: MessageDetailsAdapter,
    private val folderIds: MutableList<String>
) : Observer<List<Label>> {

    override fun onChanged(labels: List<Label>?) {
        if (labels.isNullOrEmpty()) {
            adapter.labelsView.visibility = View.GONE
            return
        } else {
            adapter.labelsView.visibility = View.VISIBLE
        }

        val labelView = adapter.labelsView
        val context = labelView.context
        labelView.removeAllViews()
        val strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 1f,
            labelView.resources.displayMetrics
        ).toInt()
        for (label in labels) {
            if (label.exclusive) {
                val colorString = label.color
                val folderIcon = FolderIconView(context).apply {
                    setColorFilter(Color.parseColor(UiUtil.normalizeColor(colorString)))
                }
                labelView.addView(folderIcon, 0)
                folderIds.add(label.id)
            } else {
                val labelItemView = ItemLabelMarginlessSmallView(context)
                labelView.addView(labelItemView)
                val name = label.name
                val colorString = label.color
                val color = if (colorString.isNotEmpty()) {
                    Color.parseColor(UiUtil.normalizeColor(colorString))
                } else {
                    0
                }
                labelItemView.bind(name, color, strokeWidth)
            }
        }
    }
}
