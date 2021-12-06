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
package ch.protonmail.android.labels.presentation.mapper

import android.graphics.Color
import ch.protonmail.android.R
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.uiModel.LabelUiModel
import ch.protonmail.android.utils.UiUtil
import me.proton.core.domain.arch.Mapper
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import javax.inject.Inject

/**
 * A Mapper of [LabelUiModel]
 *
 * @author Davide Farella
 */
internal class LabelUiModelMapper @Inject constructor() : Mapper<Label, LabelUiModel> {

    /** @return [LabelUiModel] from receiver [LabelEntity] Entity */
    fun toUiModel(label: Label): LabelUiModel {

        val image = when (label.type) {
            LabelType.MESSAGE_LABEL -> R.drawable.shape_ellipse
            LabelType.FOLDER -> R.drawable.ic_folder
            LabelType.CONTACT_GROUP -> throw IllegalArgumentException("Contact groups are not supported!")
        }

        return LabelUiModel(
            labelId = label.id,
            name = label.name,
            image = image,
            color = normalizeColor(label.color),
            parentId = label.parentId.takeIfNotBlank()?.let(::LabelId),
            isChecked = false,
            expanded = 0,
            type = label.type
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
