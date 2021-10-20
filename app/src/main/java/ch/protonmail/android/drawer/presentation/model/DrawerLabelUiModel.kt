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
package ch.protonmail.android.drawer.presentation.model

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import ch.protonmail.android.labels.domain.model.LabelType

/**
 * An UiModel representing a Label item
 *
 * @author Davide Farella
 */
data class DrawerLabelUiModel(
    val labelId: String,
    val name: String,
    val icon: Icon,
    val type: LabelType
) {

    data class Icon(
        @DrawableRes val drawableRes: Int,
        @ColorInt val colorInt: Int
    )
}
