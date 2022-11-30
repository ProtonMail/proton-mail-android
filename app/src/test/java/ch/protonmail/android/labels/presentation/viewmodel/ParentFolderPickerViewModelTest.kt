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

package ch.protonmail.android.labels.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.usecase.ObserveFoldersEligibleAsParent
import ch.protonmail.android.labels.presentation.mapper.ParentFolderPickerItemUiModelMapper
import ch.protonmail.android.labels.presentation.model.LabelIcon
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerAction
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerItemUiModel
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerState
import ch.protonmail.android.labels.utils.buildFolders
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import timber.log.Timber
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ParentFolderPickerViewModelTest :
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private val savedStateHandle: SavedStateHandle = mockk {
        every { get<String>(any()) } returns null
    }
    private val accountManager: AccountManager = mockk {
        every { getPrimaryUserId() } returns flowOf(USER_ID)
    }
    private val observeFoldersEligibleAsParent: ObserveFoldersEligibleAsParent = mockk()
    private val mapper: ParentFolderPickerItemUiModelMapper = mockk()
    private val mockTimberTree: Timber.Tree = mockk()

    private val viewModel by lazy {
        ParentFolderPickerViewModel(
            savedStateHandle = savedStateHandle,
            dispatchers = dispatchers,
            accountManager = accountManager,
            observeFoldersEligibleAsParent = observeFoldersEligibleAsParent,
            mapper = mapper
        )
    }

    @BeforeTest
    fun setup() {
        Timber.plant(mockTimberTree)
    }

    @AfterTest
    fun teardown() {
        Timber.uproot(mockTimberTree)
    }

    @Test
    fun `on select action selects the correct item while loading`() = runBlockingTest {
        // given
        every { observeFoldersEligibleAsParent(USER_ID) } returns flowOf()
        val action = ParentFolderPickerAction.SetSelected(FOLDER_1_ID)
        val expectedState = ParentFolderPickerState.Loading(selectedItemId = FOLDER_1_ID)

        // when
        viewModel.process(action)

        // then
        viewModel.state.test {

            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `on select action selects the correct item while picking`() = runBlockingTest {
        // given
        every { observeFoldersEligibleAsParent(USER_ID) } returns flowOf(buildTwoFoldersList())
        every { mapper.toUiModels(buildTwoFoldersList(), any(), any(), any()) } returns buildTwoFoldersUiModelList()

        val action = ParentFolderPickerAction.SetSelected(FOLDER_1_ID)

        val expectedState = ParentFolderPickerState.Editing(
            selectedItemId = FOLDER_1_ID,
            items = buildTwoFoldersUiModelList(FOLDER_1_ID)
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
        val saveAndCloseAction = ParentFolderPickerAction.SaveAndClose
        val setSelectedAction = ParentFolderPickerAction.SetSelected(FOLDER_1_ID)

        val expectedState = ParentFolderPickerState.SavingAndClose(selectedItemId = null)

        // when
        viewModel.process(saveAndCloseAction)
        viewModel.process(setSelectedAction)

        // then
        viewModel.state.test {

            assertEquals(expectedState, awaitItem())
            verify { mockTimberTree.d("Previous state is 'SavingAndClose', ignoring the current change") }
        }
    }

    @Test
    fun `on save and close action emits correct state with correct selected item`() =
        runBlockingTest {
            // given
            val setSelectedAction = ParentFolderPickerAction.SetSelected(FOLDER_2_ID)
            val saveAndCloseAction = ParentFolderPickerAction.SaveAndClose

            val expectedState = ParentFolderPickerState.SavingAndClose(FOLDER_2_ID)

            // when
            viewModel.process(setSelectedAction)
            viewModel.process(saveAndCloseAction)

            // then
            viewModel.state.test {

                assertEquals(expectedState, awaitItem())
            }
        }

    @Test
    fun `emits updated data from source`() = runBlockingTest {
        // given
        val dataSourceFlow = MutableSharedFlow<List<LabelOrFolderWithChildren.Folder>>()
        every { observeFoldersEligibleAsParent(USER_ID) } returns dataSourceFlow
        every { mapper.toUiModels(buildOneFolderList(), any(), any(), any()) } returns buildOneFolderUiModelList()
        every { mapper.toUiModels(buildTwoFoldersList(), any(), any(), any()) } returns buildTwoFoldersUiModelList()

        val firstExpectedState = ParentFolderPickerState.Editing(
            selectedItemId = null,
            items = buildOneFolderUiModelList()
        )
        val secondExpectedState = ParentFolderPickerState.Editing(
            selectedItemId = null,
            items = buildTwoFoldersUiModelList()
        )

        // when
        viewModel.state.test {

            assertEquals(ParentFolderPickerState.Loading(selectedItemId = null), awaitItem())

            // then
            dataSourceFlow.emit(buildOneFolderList())
            assertEquals(firstExpectedState, awaitItem())

            dataSourceFlow.emit(buildTwoFoldersList())
            assertEquals(secondExpectedState, awaitItem())
        }
    }

    @Test
    fun `selectedItemId is correctly retrieved from SavedStateHandle`() = runBlockingTest {
        // given
        every { savedStateHandle.get<String>(any()) } returns FOLDER_2_ID.id
        val expectedState = ParentFolderPickerState.Loading(selectedItemId = FOLDER_2_ID)

        // when
        viewModel.state.test {

            assertEquals(expectedState, awaitItem())
        }
    }

    companion object TestData {

        private const val FOLDER_1_NAME = "folder 1"
        private const val FOLDER_2_NAME = "folder 2"

        val USER_ID = UserId("user")
        val FOLDER_1_ID = LabelId(FOLDER_1_NAME)
        val FOLDER_2_ID = LabelId(FOLDER_2_NAME)

        fun buildOneFolderList(): List<LabelOrFolderWithChildren.Folder> = buildFolders {
            folder(FOLDER_1_NAME)
        }

        fun buildTwoFoldersList(): List<LabelOrFolderWithChildren.Folder> = buildFolders {
            folder(FOLDER_1_NAME)
            folder(FOLDER_2_NAME)
        }

        fun buildOneFolderUiModelList(selected: LabelId? = null): List<ParentFolderPickerItemUiModel> = listOf(
            buildNoneUiModel(isSelected = selected == null),
            buildFolderUiModel(FOLDER_1_NAME, isSelected = selected == FOLDER_1_ID)
        )

        fun buildTwoFoldersUiModelList(selected: LabelId? = null): List<ParentFolderPickerItemUiModel> = listOf(
            buildNoneUiModel(isSelected = selected == null),
            buildFolderUiModel(FOLDER_1_NAME, isSelected = selected == FOLDER_1_ID),
            buildFolderUiModel(FOLDER_2_NAME, isSelected = selected == FOLDER_2_ID)
        )

        private fun buildNoneUiModel(isSelected: Boolean = true) =
            ParentFolderPickerItemUiModel.None(isSelected = isSelected)

        private fun buildFolderUiModel(
            name: String,
            isSelected: Boolean = false,
        ) = ParentFolderPickerItemUiModel.Folder(
            id = LabelId(name),
            name = name,
            icon = LabelIcon.Folder.WithChildren.Colored(0),
            folderLevel = 0,
            isSelected = isSelected,
            isEnabled = true
        )
    }
}
