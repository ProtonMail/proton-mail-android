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
package ch.protonmail.android.drawer.presentation.mapper

import android.graphics.Color
import ch.protonmail.android.R
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.drawer.presentation.model.DrawerLabelUiModel
import ch.protonmail.android.mapper.UiModelMapper
import ch.protonmail.android.utils.UiUtil
import timber.log.Timber
import javax.inject.Inject

/**
 * A Mapper of [DrawerLabelUiModel]
 * Inherit from [UiModelMapper]
 *
 * @author Davide Farella
 */
internal class DrawerLabelUiModelMapper @Inject constructor() : UiModelMapper<Label, DrawerLabelUiModel> {

    override fun Label.toUiModel(): DrawerLabelUiModel {

        val type = if (exclusive) {
            DrawerLabelUiModel.Type.FOLDERS
        } else DrawerLabelUiModel.Type.LABELS

        val image = when (type) {
            DrawerLabelUiModel.Type.LABELS -> R.drawable.shape_ellipse
            DrawerLabelUiModel.Type.FOLDERS -> R.drawable.ic_folder
        }

        return DrawerLabelUiModel(
            labelId = id,
            name = name,
            icon = DrawerLabelUiModel.Icon(image, normalizeColor(color)),
            type = type
        )
    }

    private fun normalizeColor(color: String): Int {

        val v4FixedColor = when (color) {
            "#5ec7b7" -> "#79C4B7"
            "#97c9c1" -> "#A1C8C1"
            else -> color
        }

        return try {
            Color.parseColor(UiUtil.normalizeColor(v4FixedColor))
        } catch (exception: Exception) {
            Timber.w(exception, "Cannot parse color: $v4FixedColor")
            Color.WHITE
        }
    }
}
