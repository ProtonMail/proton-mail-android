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
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import timber.log.Timber
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ParentFolderPickerViewModelTest : CoroutinesTest {

    private val mockTimberTree: Timber.Tree = mockk()

    @BeforeTest
    fun setup() {
        Timber.plant(mockTimberTree)
    }

    @AfterTest
    fun teardown() {
        Timber.uproot(mockTimberTree)
    }

    @Test
    fun `on select action selects the correct item`() = runBlockingTest {
        // given
        val initialState = ParentFolderPickerState.Editing(
            selectedItemId = null,
            items = listOf(
                buildNoneUiModel(isSelected = true),
                buildFolderUiModel(TEST_FOLDER_ID_1, isSelected = false),
                buildFolderUiModel(TEST_FOLDER_ID_2, isSelected = false)
            )
        )
        val viewModel = buildViewModel(initialState)

        val action = ParentFolderPickerAction.SetSelected(TEST_FOLDER_ID_1)

        val expectedState = ParentFolderPickerState.Editing(
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

    @Test
    fun `select action is ignored if is currently saving and closing`() = runBlockingTest {
        // given
        val initialState = ParentFolderPickerState.SavingAndClose(selectedItemId = null)
        val viewModel = buildViewModel(initialState)

        val action = ParentFolderPickerAction.SetSelected(TEST_FOLDER_ID_1)

        // when
        viewModel.process(action)

        // then
        viewModel.state.test {

            assertEquals(initialState, awaitItem())
            verify { mockTimberTree.w("Previous state is 'SavingAndClose', ignoring the current change") }
        }
    }

    @Test
    fun `on save and close action emits correct state with correct selected item`() =
        runBlockingTest {
            // given
            val initialState = ParentFolderPickerState.Editing(
                selectedItemId = TEST_FOLDER_ID_2,
                items = emptyList()
            )
            val viewModel = buildViewModel(initialState)

            val action = ParentFolderPickerAction.SaveAndClose

            val expectedState = ParentFolderPickerState.SavingAndClose(TEST_FOLDER_ID_2)

            // when
            viewModel.process(action)

            // then
            viewModel.state.test {

                assertEquals(expectedState, awaitItem())
            }
        }

    private fun buildViewModel(
        initialState: ParentFolderPickerState = ParentFolderPickerState.Editing(
            selectedItemId = null,
            listOf(buildNoneUiModel())
        )
    ) = ParentFolderPickerViewModel(TestDispatcherProvider, initialState)

    companion object TestData {

        val TEST_FOLDER_ID_1 = LabelId("folder 1")
        val TEST_FOLDER_ID_2 = LabelId("folder 2")

        fun buildNoneUiModel(isSelected: Boolean = true) =
            ParentFolderPickerItemUiModel.None(isSelected = isSelected)

        fun buildFolderUiModel(
            labelId: LabelId,
            isSelected: Boolean = false,
        ) = ParentFolderPickerItemUiModel.Folder(
            id = labelId,
            name = "name",
            icon = ParentFolderPickerItemUiModel.Folder.Icon(0, 0, 0),
            folderLevel = 0,
            isSelected = isSelected
        )
    }
}
