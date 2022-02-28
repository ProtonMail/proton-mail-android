/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.drawer.presentation.mapper

import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import me.proton.core.domain.arch.Mapper
import javax.inject.Inject

/**
 * Map from [LabelOrFolderWithChildren] to [DrawerItemUiModel.Primary.Label]
 * Inherit from [Mapper]
 */
internal class DrawerLabelItemUiModelMapper @Inject constructor(
    private val drawerLabelMapper: DrawerLabelUiModelMapper
) : Mapper<Collection<LabelOrFolderWithChildren>, List<DrawerItemUiModel.Primary.Label>> {

    fun toUiModels(models: Collection<LabelOrFolderWithChildren>): List<DrawerItemUiModel.Primary.Label> =
        drawerLabelMapper.toUiModels(models).map(DrawerItemUiModel.Primary::Label)
}
