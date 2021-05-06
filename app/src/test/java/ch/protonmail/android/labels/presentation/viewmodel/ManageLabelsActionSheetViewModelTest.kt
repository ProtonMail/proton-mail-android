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

import android.graphics.Color
import androidx.lifecycle.SavedStateHandle
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.labels.domain.model.ManageLabelActionResult
import ch.protonmail.android.labels.domain.model.StandardFolderLocation
import ch.protonmail.android.labels.domain.usecase.GetAllLabels
import ch.protonmail.android.labels.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.labels.domain.usecase.UpdateLabels
import ch.protonmail.android.labels.presentation.model.ManageLabelItemUiModel
import ch.protonmail.android.labels.presentation.ui.ManageLabelsActionSheet
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ManageLabelsActionSheetViewModelTest : ArchTest, CoroutinesTest {

    @MockK
    private lateinit var moveMessagesToFolder: MoveMessagesToFolder

    @MockK
    private lateinit var updateLabels: UpdateLabels

    @MockK
    private lateinit var userManager: UserManager

    @MockK
    private lateinit var getAllLabels: GetAllLabels

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: ManageLabelsActionSheetViewModel

    private val checkedLabelId1 = "checkedLabelId1"
    private val messageId1 = "messageId1"
    private val labelId = "labelId1"
    private val labelId2 = "labelId2"
    private val iconRes = 123
    private val title = "title"
    private val titleRes = 321
    private val colorInt = Color.YELLOW
    private val model1label = ManageLabelItemUiModel(
        labelId,
        iconRes,
        title,
        titleRes,
        colorInt,
        true
    )
    private val model2folder = ManageLabelItemUiModel(
        labelId2,
        iconRes,
        title,
        titleRes,
        colorInt,
        false,
        ManageLabelsActionSheet.Type.FOLDER.typeInt
    )

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)

        every { savedStateHandle.get<List<String>>(ManageLabelsActionSheet.EXTRA_ARG_MESSAGES_IDS) } returns listOf(
            messageId1
        )
        every {
            savedStateHandle.get<List<String>>(
                ManageLabelsActionSheet.EXTRA_ARG_MESSAGE_CHECKED_LABELS
            )
        } returns listOf(checkedLabelId1)
        every {
            savedStateHandle.get<ManageLabelsActionSheet.Type>(
                ManageLabelsActionSheet.EXTRA_ARG_ACTION_SHEET_TYPE
            )
        } returns ManageLabelsActionSheet.Type.LABEL

        every {
            savedStateHandle.get<Int>(ManageLabelsActionSheet.EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID)
        } returns 0

        coEvery { getAllLabels.invoke(any(), any(), any()) } returns listOf(model1label, model2folder)

        viewModel = ManageLabelsActionSheetViewModel(
            savedStateHandle,
            getAllLabels,
            userManager,
            updateLabels,
            moveMessagesToFolder
        )
    }

    @Test
    fun verifyThatAfterOnDoneIsClickedLabelsSuccessIsEmitted() = runBlockingTest {

        // given
        val shallMoveToArchive = true
        coEvery { updateLabels.invoke(any(), any()) } just Runs
        coEvery {
            moveMessagesToFolder(
                listOf(messageId1), StandardFolderLocation.ARCHIVE.id,
                Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString()
            )
        } just Runs

        // when
        viewModel.onDoneClicked(shallMoveToArchive)

        // then
        coVerify { updateLabels.invoke(any(), any()) }
        coVerify {
            moveMessagesToFolder(
                listOf(messageId1), StandardFolderLocation.ARCHIVE.id,
                Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString()
            )
        }
        assertEquals(ManageLabelActionResult.LabelsSuccessfullySaved, viewModel.actionsResult.value)
    }

    @Test
    fun verifyThatAfterOnLabelIsClickedForLabelType() = runBlockingTest {

        // given
        coEvery { userManager.didReachLabelsThreshold(any()) } returns false

        // when
        viewModel.onLabelClicked(model1label)

        // then
        assertEquals(listOf(model1label.copy(isChecked = false)), viewModel.labels.value)
        assertEquals(ManageLabelActionResult.Default, viewModel.actionsResult.value)
    }

    @Test
    fun verifyThatAfterOnLabelIsClickedForFolderTypeMessagesAreMoved() = runBlockingTest {

        // given
        coEvery { userManager.didReachLabelsThreshold(any()) } returns false
        coEvery { moveMessagesToFolder.invoke(any(), any(), any()) } just Runs

        // when
        viewModel.onLabelClicked(model2folder)

        // then
        coVerify { moveMessagesToFolder.invoke(any(), any(), any()) }
        assertEquals(ManageLabelActionResult.MessageSuccessfullyMoved, viewModel.actionsResult.value)
    }
}
