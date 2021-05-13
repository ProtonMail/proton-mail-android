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
import android.view.View
import androidx.lifecycle.Observer
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.ui.view.LabelChipUiModel
import ch.protonmail.android.utils.UiUtil

// TODO change to immutable list after changing all uses to kotlin

/**
 * A class that observes changes in message labels
 */
class LabelsObserver(
    private val adapter: MessageDetailsAdapter,
    private val folderIds: MutableList<String>
) : Observer<List<Label>> {

    override fun onChanged(labels: List<Label>?) {
        adapter.labelsList = labels
        val nonExclusiveLabels = labels?.filterNot { it.exclusive }

        if (nonExclusiveLabels.isNullOrEmpty()) {
            adapter.labelsView.visibility = View.GONE
            return
        }
        adapter.labelsView.visibility = View.VISIBLE
        adapter.labelsView.setLabels(nonExclusiveLabels.toLabelChipUiModels())
    }

    private fun List<Label>.toLabelChipUiModels(): List<LabelChipUiModel> =
        map { label ->
            val color =
                if (label.color.isNotBlank()) Color.parseColor(UiUtil.normalizeColor(label.color))
                else Color.BLACK

            LabelChipUiModel(Id(label.id), Name(label.name), color)
        }
}
