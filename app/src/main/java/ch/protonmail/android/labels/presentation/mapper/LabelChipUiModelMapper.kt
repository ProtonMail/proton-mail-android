/*
 * Copyright (c) 2022 Proton Technologies AG
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
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.utils.UiUtil
import me.proton.core.domain.arch.Mapper
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

class LabelChipUiModelMapper @Inject constructor() : Mapper<Label, LabelChipUiModel> {

    fun toUiModel(label: Label): LabelChipUiModel {
        val labelColor = label.color.takeIfNotBlank()
            ?.let { Color.parseColor(UiUtil.normalizeColor(it)) }

        return LabelChipUiModel(label.id, Name(label.name), labelColor)
    }

    fun toUiModels(labels: Collection<Label>): List<LabelChipUiModel> =
        labels.map(::toUiModel)
}
