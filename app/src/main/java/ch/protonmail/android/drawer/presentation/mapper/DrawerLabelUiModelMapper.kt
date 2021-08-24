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

import android.content.Context
import android.graphics.Color
import androidx.annotation.VisibleForTesting
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.LabelEntity
import ch.protonmail.android.drawer.presentation.model.DrawerLabelUiModel
import ch.protonmail.android.mapper.UiModelMapper
import ch.protonmail.android.utils.UiUtil
import timber.log.Timber
import javax.inject.Inject

/**
 * A Mapper of [DrawerLabelUiModel]
 * Inherit from [UiModelMapper]
 *
 * @property useFolderColor whether the user enabled the settings for use Colors for Folders.
 *  TODO to be implemented in MAILAND-1818, ideally inject its use case. Currently defaults to `true`
 */
internal class DrawerLabelUiModelMapper @Inject constructor(
    private val context: Context
) : UiModelMapper<LabelEntity, DrawerLabelUiModel> {

    private val useFolderColor: Boolean = true

    override fun LabelEntity.toUiModel(): DrawerLabelUiModel {

        val type =
            if (type == Constants.LABEL_TYPE_MESSAGE_FOLDERS) DrawerLabelUiModel.Type.FOLDERS
            else DrawerLabelUiModel.Type.LABELS

        return DrawerLabelUiModel(
            labelId = id.id,
            name = name,
            icon = buildIcon(type, color),
            type = type
        )
    }

    private fun buildIcon(type: DrawerLabelUiModel.Type, color: String): DrawerLabelUiModel.Icon {

        val drawableRes = when (type) {
            DrawerLabelUiModel.Type.LABELS -> R.drawable.shape_ellipse
            DrawerLabelUiModel.Type.FOLDERS -> if (useFolderColor) R.drawable.ic_folder_filled else R.drawable.ic_folder
        }

        val colorInt =
            if (useFolderColor) toColorInt(color)
            else context.getColor(R.color.icon_inverted)

        return DrawerLabelUiModel.Icon(drawableRes, colorInt)
    }

    private fun toColorInt(color: String): Int {
        return when (color) {
            AQUA_BASE_V3_COLOR -> context.getColor(R.color.aqua_base)
            SAGE_BASE_V3_COLOR -> context.getColor(R.color.sage_base)
            else -> parseColor(color)
        }
    }

    private fun parseColor(color: String): Int =
        try {
            Color.parseColor(UiUtil.normalizeColor(color))
        } catch (exception: Exception) {
            Timber.w(exception, "Cannot parse color: $color")
            context.getColor(R.color.icon_inverted)
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    companion object {

        const val AQUA_BASE_V3_COLOR = "#5ec7b7"
        const val SAGE_BASE_V3_COLOR = "#97c9c1"
    }
}
