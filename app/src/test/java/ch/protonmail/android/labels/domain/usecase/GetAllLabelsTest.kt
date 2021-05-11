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

package ch.protonmail.android.labels.domain.usecase

import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.labels.presentation.model.StandardFolderLocation
import ch.protonmail.android.labels.presentation.mapper.LabelsMapper
import ch.protonmail.android.labels.presentation.model.ManageLabelItemUiModel
import ch.protonmail.android.labels.presentation.ui.ManageLabelsActionSheet
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAllLabelsTest {

    @MockK
    private lateinit var repository: MessageDetailsRepository

    @MockK
    private lateinit var labelsMapper: LabelsMapper

    private lateinit var useCase: GetAllLabels

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = GetAllLabels(repository, labelsMapper)
    }

    @Test
    fun verifyThatLabelsTypeAreReceivedAndProcessedCorrectly() = runBlockingTest {
        // given
        val testLabelId1 = "labelId1"
        val testLabelId2 = "labelId2"
        val testColorInt = 123
        val currentLabelsSelection = listOf(testLabelId1, testLabelId2)
        val sheetType = ManageLabelsActionSheet.Type.LABEL
        val label1 = mockk<Label> {
            every { id } returns testLabelId1
            every { exclusive } returns false // false for normal label/not a folder (api v3)
        }
        val label2 = mockk<Label> {
            every { id } returns testLabelId2
            every { exclusive } returns false // false for normal label/not a folder (api v3)
        }
        val uiLabel1 = ManageLabelItemUiModel(
            testLabelId1,
            R.drawable.circle_labels_selection,
            "labelName1",
            null,
            testColorInt,
            true,
            ManageLabelsActionSheet.Type.LABEL.typeInt
        )
        val uiLabel2 = ManageLabelItemUiModel(
            testLabelId2,
            R.drawable.circle_labels_selection,
            "labelName2",
            null,
            testColorInt,
            true,
            ManageLabelsActionSheet.Type.LABEL.typeInt
        )
        val expected = listOf(uiLabel1, uiLabel2)
        coEvery { repository.getAllLabels() } returns listOf(label1, label2)
        every { labelsMapper.mapLabelToUi(label1, currentLabelsSelection, sheetType) } returns uiLabel1
        every { labelsMapper.mapLabelToUi(label2, currentLabelsSelection, sheetType) } returns uiLabel2


        // when
        val result = useCase.invoke(currentLabelsSelection)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatFolderTypeAreReceivedAndProcessedCorrectly() = runBlockingTest {
        // given
        val testLabelId1 = "labelId1"
        val testLabelId2 = "labelId2"
        val testColorInt = 123
        val currentLabelsSelection = listOf(testLabelId1, testLabelId2)
        val sheetType = ManageLabelsActionSheet.Type.FOLDER
        val label1 = mockk<Label> {
            every { id } returns testLabelId1
            every { exclusive } returns true // true for a folder/not a normal label (api v3)
        }
        val label2 = mockk<Label> {
            every { id } returns testLabelId2
            every { exclusive } returns true  // true for a folder/not a normal label (api v3)
        }
        val uiLabel1 = ManageLabelItemUiModel(
            testLabelId1,
            R.drawable.circle_labels_selection,
            "labelName1",
            null,
            testColorInt,
            true,
            ManageLabelsActionSheet.Type.FOLDER.typeInt
        )
        val uiLabel2 = ManageLabelItemUiModel(
            testLabelId2,
            R.drawable.circle_labels_selection,
            "labelName2",
            null,
            testColorInt,
            true,
            ManageLabelsActionSheet.Type.FOLDER.typeInt
        )
        val expected = listOf(uiLabel1, uiLabel2) + getAllStandardFolders()
        coEvery { repository.getAllLabels() } returns listOf(label1, label2)
        every { labelsMapper.mapLabelToUi(label1, currentLabelsSelection, sheetType) } returns uiLabel1
        every { labelsMapper.mapLabelToUi(label2, currentLabelsSelection, sheetType) } returns uiLabel2


        // when
        val result = useCase.invoke(
            currentLabelsSelection,
            ManageLabelsActionSheet.Type.FOLDER,
            Constants.MessageLocationType.INVALID
        )

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatFolderTypeAreReceivedAndProcessedCorrectlyAndCurrentInboxFolderIsOmitted() = runBlockingTest {
        // given
        val testLabelId1 = "labelId1"
        val testLabelId2 = "labelId2"
        val testColorInt = 123
        val currentLabelsSelection = listOf(testLabelId1, testLabelId2)
        val sheetType = ManageLabelsActionSheet.Type.FOLDER
        val label1 = mockk<Label> {
            every { id } returns testLabelId1
            every { exclusive } returns true // true for a folder/not a normal label (api v3)
        }
        val label2 = mockk<Label> {
            every { id } returns testLabelId2
            every { exclusive } returns true  // true for a folder/not a normal label (api v3)
        }
        val uiLabel1 = ManageLabelItemUiModel(
            testLabelId1,
            R.drawable.circle_labels_selection,
            "labelName1",
            null,
            testColorInt,
            true,
            ManageLabelsActionSheet.Type.FOLDER.typeInt
        )
        val uiLabel2 = ManageLabelItemUiModel(
            testLabelId2,
            R.drawable.circle_labels_selection,
            "labelName2",
            null,
            testColorInt,
            true,
            ManageLabelsActionSheet.Type.FOLDER.typeInt
        )
        val expected = listOf(uiLabel1, uiLabel2) + getAllStandardFolders()
            .filter { it.labelId !=  Constants.MessageLocationType.INBOX.toString()}
        coEvery { repository.getAllLabels() } returns listOf(label1, label2)
        every { labelsMapper.mapLabelToUi(label1, currentLabelsSelection, sheetType) } returns uiLabel1
        every { labelsMapper.mapLabelToUi(label2, currentLabelsSelection, sheetType) } returns uiLabel2


        // when
        val result = useCase.invoke(
            currentLabelsSelection,
            ManageLabelsActionSheet.Type.FOLDER,
            Constants.MessageLocationType.INBOX
        )

        // then
        assertEquals(expected, result)
    }

    private fun getAllStandardFolders(): List<ManageLabelItemUiModel> =
        StandardFolderLocation.values()
            .map { location ->
                ManageLabelItemUiModel(
                    labelId = location.id,
                    iconRes = location.iconRes,
                    titleRes = location.title,
                    labelType = ManageLabelsActionSheet.Type.FOLDER.typeInt
                )
            }


}
