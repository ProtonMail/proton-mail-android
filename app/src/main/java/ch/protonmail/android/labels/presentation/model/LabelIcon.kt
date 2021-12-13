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

sealed class LabelIcon {

    @get:DrawableRes
    abstract val drawableRes: Int

    @get:ColorInt
    abstract val colorInt: Int

    @get:StringRes
    abstract val contentDescriptionRes: Int

    data class Label(
        @ColorInt override val colorInt: Int
    ) : LabelIcon() {

        override val drawableRes = R.drawable.circle_labels_selection
        override val contentDescriptionRes = R.string.x_label_icon_description
    }

    sealed class Folder : LabelIcon() {

        sealed class WithChildren : Folder() {

            override val contentDescriptionRes = R.string.x_parent_folder_icon_description

            object BlackWhite : WithChildren() {

                override val colorInt = R.color.icon_norm
                override val drawableRes = R.drawable.ic_folder_multiple
            }

            data class Colored(
                @ColorInt override val colorInt: Int
            ) : WithChildren() {

                override val drawableRes = R.drawable.ic_folder_multiple_filled
            }
        }

        sealed class WithoutChildren : Folder() {

            override val contentDescriptionRes = R.string.x_folder_icon_description

            object BlackWhite : WithoutChildren() {

                override val colorInt = R.color.icon_norm
                override val drawableRes = R.drawable.ic_folder
            }

            data class Colored(
                @ColorInt override val colorInt: Int
            ) : WithChildren() {

                override val drawableRes = R.drawable.ic_folder_filled
            }
        }
    }
}
