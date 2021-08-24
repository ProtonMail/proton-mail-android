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
package ch.protonmail.android.mapper

import android.graphics.Color
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.LabelEntity
import ch.protonmail.android.uiModel.LabelUiModel
import ch.protonmail.android.utils.UiUtil
import timber.log.Timber

/**
 * A Mapper of [LabelUiModel]
 * Inherit from [UiModelMapper]
 *
 * @author Davide Farella
 */
internal class LabelUiModelMapper : UiModelMapper<LabelEntity, LabelUiModel> {

    /** @return [LabelUiModel] from receiver [LabelEntity] Entity */
    override fun LabelEntity.toUiModel(): LabelUiModel {

        val type = if (type == Constants.LABEL_TYPE_MESSAGE_FOLDERS) {
            LabelUiModel.Type.FOLDERS
        } else LabelUiModel.Type.LABELS

        val image = when (type) {
            LabelUiModel.Type.LABELS -> R.drawable.shape_ellipse
            LabelUiModel.Type.FOLDERS -> R.drawable.ic_folder
        }

        return LabelUiModel(
            labelId = id.id,
            name = name,
            image = image,
            color = normalizeColor(color),
            isChecked = false,
            expanded = expanded,
            type = type
        )
    }

    private fun normalizeColor(color: String): Int {

        return try {
            Color.parseColor(UiUtil.normalizeColor(color))
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception, "Cannot parse color: $color")
            Color.GRAY
        }
    }
}
