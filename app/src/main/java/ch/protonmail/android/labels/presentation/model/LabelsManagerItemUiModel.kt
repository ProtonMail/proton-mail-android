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

package ch.protonmail.android.labels.presentation.model

import androidx.recyclerview.widget.DiffUtil
import ch.protonmail.android.labels.domain.model.LabelId

sealed class LabelsManagerItemUiModel {

    abstract val id: LabelId
    abstract val name: String
    abstract val icon: LabelIcon
    abstract val isChecked: Boolean

    data class Label(
        override val id: LabelId,
        override val name: String,
        override val icon: LabelIcon.Label,
        override val isChecked: Boolean
    ) : LabelsManagerItemUiModel()

    data class Folder(
        override val id: LabelId,
        override val name: String,
        override val icon: LabelIcon.Folder,
        val parentId: LabelId?,
        val folderLevel: Int,
        override val isChecked: Boolean
    ) : LabelsManagerItemUiModel()

    object DiffCallback : DiffUtil.ItemCallback<LabelsManagerItemUiModel>() {

        override fun areItemsTheSame(
            oldItem: LabelsManagerItemUiModel,
            newItem: LabelsManagerItemUiModel
        ) = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: LabelsManagerItemUiModel,
            newItem: LabelsManagerItemUiModel
        ) = oldItem == newItem
    }
}
