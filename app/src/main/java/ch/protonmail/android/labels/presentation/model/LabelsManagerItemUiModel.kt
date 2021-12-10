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
import ch.protonmail.android.R
import ch.protonmail.android.labels.domain.model.LabelId

sealed class LabelsManagerItemUiModel {

    abstract val id: LabelId
    abstract val name: String
    abstract val icon: Icon
    abstract val isChecked: Boolean

    data class Label(
        override val id: LabelId,
        override val name: String,
        @ColorInt val colorInt: Int,
        override val isChecked: Boolean
    ) : LabelsManagerItemUiModel() {

        override val icon = Icon(
            drawableRes = R.drawable.circle_labels_selection,
            colorInt = colorInt,
            contentDescriptionRes = R.string.x_label_icon_description
        )
    }

    data class Folder(
        override val id: LabelId,
        override val name: String,
        override val icon: Icon,
        val folderLevel: Int,
        override val isChecked: Boolean
    ) : LabelsManagerItemUiModel()


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
            const val WITH_CHILDREN_CONTENT_DESCRIPTION_RES = R.string.x_parent_folder_icon_description
            const val WITHOUT_CHILDREN_CONTENT_DESCRIPTION_RES = R.string.x_folder_icon_description
        }
    }

}
