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
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.uiModel.LabelUiModel
import ch.protonmail.android.utils.UiUtil

/**
 * A Mapper of [LabelUiModel]
 * Inherit from [UiModelMapper]
 *
 * @param isLabelEditable whether the created Label is editable.
 * A different [LabelUiModel.image] will be used if [isLabelEditable]
 *
 * @author Davide Farella
 */
internal class LabelUiModelMapper(private val isLabelEditable: Boolean) : UiModelMapper<Label, LabelUiModel> {

    /** @return [LabelUiModel] from receiver [Label] Entity */
    override fun Label.toUiModel(): LabelUiModel {

        val type = if (exclusive)
            LabelUiModel.Type.FOLDERS else LabelUiModel.Type.LABELS

        val image = when (type) {
            LabelUiModel.Type.LABELS ->
                if (isLabelEditable) R.drawable.label_edit_active else R.drawable.ic_menu_label
            LabelUiModel.Type.FOLDERS ->
                if (isLabelEditable) R.drawable.folder_edit_active else R.drawable.ic_menu_folder
        }

        val normalizedColor =
            try {
                Color.parseColor(UiUtil.normalizeColor(color))
            } catch (t: Throwable) {
                Color.WHITE
            }

        return LabelUiModel(
            labelId = id,
            name = name,
            image = image,
            color = normalizedColor,
            isChecked = false,
            display = display,
            type = type
        )
    }
}
