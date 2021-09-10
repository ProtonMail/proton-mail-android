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

import ch.protonmail.android.R
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.drawer.presentation.model.DrawerFoldersAndLabelsSectionUiModel
import ch.protonmail.android.drawer.presentation.model.DrawerLabelUiModel
import ch.protonmail.android.mapper.UiModelMapper
import me.proton.core.domain.arch.map
import javax.inject.Inject

/**
 * Map from [List] of [Label] to [DrawerFoldersAndLabelsSectionUiModel]
 * Inherit from [UiModelMapper]
 */
internal class DrawerFoldersAndLabelsSectionUiModelMapper @Inject constructor(
    private val drawerLabelItemUiModelMapper: DrawerLabelItemUiModelMapper
) : UiModelMapper<List<Label>, DrawerFoldersAndLabelsSectionUiModel> {

    override fun List<Label>.toUiModel(): DrawerFoldersAndLabelsSectionUiModel {
        val (labelsItems, foldersItems) = map(drawerLabelItemUiModelMapper) { it.toUiModel() }
            .partition { it.uiModel.type == DrawerLabelUiModel.Type.LABELS }

        return DrawerFoldersAndLabelsSectionUiModel(
            foldersSectionNameRes = R.string.folders,
            folders = foldersItems,
            labelsSectionNameRes = R.string.labels,
            labels = labelsItems
        )
    }
}
