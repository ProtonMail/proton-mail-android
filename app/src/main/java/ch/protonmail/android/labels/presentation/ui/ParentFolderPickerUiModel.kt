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

package ch.protonmail.android.labels.presentation.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import ch.protonmail.android.R
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.presentation.ui.ParentFolderPickerUiModel.Icon.WithChildren
import ch.protonmail.android.labels.presentation.ui.ParentFolderPickerUiModel.Icon.WithoutChildren

sealed class ParentFolderPickerUiModel(
    val id: LabelId,
    val name: String,
    val colorInt: Int,
    val icon: Icon,
    val folderLevel: Int
) {

    /**
     * @see WithChildren The target folder has DISPLAYED children
     * @see WithoutChildren The target folder has not DISPLAYED children
     *
     * DISPLAYED children defer from the Folder's children because, having a limited level of nesting, the max
     *  level won't be displayed
     */
    sealed class Icon(
        @DrawableRes val drawableRes: Int,
        @StringRes val contentDescriptionRes: Int
    ) {

        object WithChildren : Icon(
            R.drawable.ic_folder_multiple_filled,
            R.string.parent_picker_parent_folder_icon_description
        )

        object WithoutChildren : Icon(
            R.drawable.ic_folder_filled,
            R.string.parent_picker_folder_icon_description
        )
    }

    object DiffCallback : DiffUtil.ItemCallback<ParentFolderPickerUiModel>() {

        override fun areItemsTheSame(oldItem: ParentFolderPickerUiModel, newItem: ParentFolderPickerUiModel) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ParentFolderPickerUiModel, newItem: ParentFolderPickerUiModel) =
            oldItem == newItem
    }
}
