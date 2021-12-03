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

package ch.protonmail.android.labels.presentation.model

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import ch.protonmail.android.R
import ch.protonmail.android.labels.domain.model.LabelId

sealed class ParentFolderPickerItemUiModel {

    open val id: LabelId? = null
    abstract val isSelected: Boolean

    data class None(override val isSelected: Boolean) : ParentFolderPickerItemUiModel()

    data class Folder(
        override val id: LabelId,
        val name: String,
        val icon: Icon,
        val folderLevel: Int,
        override val isSelected: Boolean
    ) : ParentFolderPickerItemUiModel() {

        data class Icon(
            @DrawableRes val drawableRes: Int,
            @ColorInt val colorInt: Int,
            @StringRes val contentDescriptionRes: Int
        ) {

            companion object {

                const val WITH_CHILDREN_COLORED_ICON_RES = R.drawable.ic_folder_multiple_filled
                const val WITHOUT_CHILDREN_COLORED_ICON_RES = R.drawable.ic_folder_filled
                const val WITH_CHILDREN_BW_ICON_RES = R.drawable.ic_folder_multiple
                const val WITHOUT_CHILDREN_BW_ICON_RES = R.drawable.ic_folder
                const val WITH_CHILDREN_CONTENT_DESCRIPTION_RES = R.string.parent_picker_parent_folder_icon_description
                const val WITHOUT_CHILDREN_CONTENT_DESCRIPTION_RES = R.string.parent_picker_folder_icon_description
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ParentFolderPickerItemUiModel>() {

        override fun areItemsTheSame(
            oldItem: ParentFolderPickerItemUiModel,
            newItem: ParentFolderPickerItemUiModel
        ) = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ParentFolderPickerItemUiModel,
            newItem: ParentFolderPickerItemUiModel
        ) = oldItem == newItem
    }
}
