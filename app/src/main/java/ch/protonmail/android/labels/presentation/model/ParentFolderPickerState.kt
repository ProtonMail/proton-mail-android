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

package ch.protonmail.android.labels.presentation.model

import ch.protonmail.android.labels.domain.model.LabelId

sealed class ParentFolderPickerState {

    abstract val selectedItemId: LabelId?

    /**
     * Picker has just been opened, items are being laoded
     */
    data class Loading(
        override val selectedItemId: LabelId?
    ) : ParentFolderPickerState()

    /**
     * Picker is open and the user can change the selected item
     */
    data class Editing(
        override val selectedItemId: LabelId?,
        val items: List<ParentFolderPickerItemUiModel>
    ) : ParentFolderPickerState()

    /**
     * The user requested to save the current selection, the Picker is about to be closed
     */
    data class SavingAndClose(
        override val selectedItemId: LabelId?
    ) : ParentFolderPickerState()
}
