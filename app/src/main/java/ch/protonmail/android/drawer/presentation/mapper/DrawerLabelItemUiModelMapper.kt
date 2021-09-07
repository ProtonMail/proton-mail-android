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

import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel
import ch.protonmail.android.mapper.UiModelMapper
import me.proton.core.util.kotlin.invoke
import javax.inject.Inject

/**
 * Map from [Label] to [DrawerItemUiModel.Primary.Label]
 * Inherit from [UiModelMapper]
 */
internal class DrawerLabelItemUiModelMapper @Inject constructor(
    private val drawerLabelMapper: DrawerLabelUiModelMapper
) : UiModelMapper<Label, DrawerItemUiModel.Primary.Label> {

    override fun Label.toUiModel(): DrawerItemUiModel.Primary.Label =
        DrawerItemUiModel.Primary.Label(drawerLabelMapper { toUiModel() })
}
