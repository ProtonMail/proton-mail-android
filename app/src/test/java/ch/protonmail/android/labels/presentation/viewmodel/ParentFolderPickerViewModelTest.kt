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

package ch.protonmail.android.labels.presentation.viewmodel

import app.cash.turbine.test
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerAction
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerItemUiModel
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerState
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class ParentFolderPickerViewModelTest : CoroutinesTest {

    @Test
    fun `ParentFolderPickerAction_SetSelected select the correct item`() = runBlockingTest {
        // given
        val initialState = ParentFolderPickerState(
            selectedItemId = null,
            items = listOf(
                buildNoneUiModel(isSelected = true),
                buildFolderUiModel(TEST_FOLDER_ID_1, isSelected = false),
                buildFolderUiModel(TEST_FOLDER_ID_2, isSelected = false)
            )
        )
        val viewModel = buildViewModel(initialState)

        val action = ParentFolderPickerAction.SetSelected(TEST_FOLDER_ID_1)

        val expectedState = ParentFolderPickerState(
            selectedItemId = TEST_FOLDER_ID_1,
            items = listOf(
                buildNoneUiModel(isSelected = false),
                buildFolderUiModel(TEST_FOLDER_ID_1, isSelected = true),
                buildFolderUiModel(TEST_FOLDER_ID_2, isSelected = false)
            )
        )

        // when
        viewModel.process(action)

        // then
        viewModel.state.test {

            assertEquals(expectedState, awaitItem())
        }
    }

    private fun buildViewModel(
        initialState: ParentFolderPickerState = ParentFolderPickerState(
            selectedItemId = null,
            listOf(buildNoneUiModel())
        )
    ) = ParentFolderPickerViewModel(TestDispatcherProvider, initialState)

    companion object TestData {

        val TEST_FOLDER_ID_1 = LabelId("folder 1")
        val TEST_FOLDER_ID_2 = LabelId("folder 2")
        val TEST_FOLDER_ID_3 = LabelId("folder 3")
        val TEST_FOLDER_ID_4 = LabelId("folder 4")

        fun buildNoneUiModel(isSelected: Boolean = true) =
            ParentFolderPickerItemUiModel.None(isSelected = isSelected)

        fun buildFolderUiModel(
            labelId: LabelId,
            isSelected: Boolean = false,
        ) = ParentFolderPickerItemUiModel.Folder(
            id = labelId,
            name = "name",
            colorInt = 0,
            icon = ParentFolderPickerItemUiModel.Folder.Icon.WithoutChildren,
            folderLevel = 0,
            isSelected = isSelected
        )
    }
}
