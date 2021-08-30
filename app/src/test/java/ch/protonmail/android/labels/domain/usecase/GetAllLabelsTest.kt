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
import ch.protonmail.android.core.Constants
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.model.LabelId
import ch.protonmail.android.labels.data.model.LabelType
import ch.protonmail.android.labels.presentation.mapper.LabelActionItemUiModelMapper
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.labels.presentation.model.StandardFolderLocation
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAllLabelsTest {

    @MockK
    private lateinit var labelsMapper: LabelActionItemUiModelMapper

    @MockK
    private lateinit var accountManager: AccountManager

    @MockK
    private lateinit var repository: LabelRepository

    private lateinit var useCase: GetAllLabels

    private val testUserId = UserId("TestUser")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        useCase = GetAllLabels(labelsMapper, accountManager, repository)
    }

    @Test
    fun verifyThatLabelsTypeAreReceivedAndProcessedCorrectly() = runBlockingTest {
        // given
        val testLabelId1 = LabelId("labelId1")
        val testLabelId2 = LabelId("labelId2")
        val testColorInt = 123
        val currentLabelsSelection = listOf(testLabelId1.id, testLabelId2.id)
        val sheetType = LabelType.MESSAGE_LABEL
        val label1 = mockk<LabelEntity> {
            every { id } returns testLabelId1
            every { type } returns LabelType.MESSAGE_LABEL
        }
        val label2 = mockk<LabelEntity> {
            every { id } returns testLabelId2
            every { type } returns LabelType.MESSAGE_LABEL
        }
        val uiLabel1 = LabelActonItemUiModel(
            testLabelId1.id,
            R.drawable.circle_labels_selection,
            "labelName1",
            null,
            testColorInt,
            true,
            LabelType.MESSAGE_LABEL.typeInt
        )
        val uiLabel2 = LabelActonItemUiModel(
            testLabelId2.id,
            R.drawable.circle_labels_selection,
            "labelName2",
            null,
            testColorInt,
            true,
            LabelType.MESSAGE_LABEL.typeInt
        )
        val expected = listOf(uiLabel1, uiLabel2)
        coEvery { repository.findAllLabels(testUserId,) } returns listOf(label1, label2)
        every { labelsMapper.mapLabelToUi(label1, currentLabelsSelection, sheetType) } returns uiLabel1
        every { labelsMapper.mapLabelToUi(label2, currentLabelsSelection, sheetType) } returns uiLabel2


        // when
        val result = useCase.invoke(
            currentLabelsSelection
        )

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatFolderTypeAreReceivedAndProcessedCorrectly() = runBlockingTest {
        // given
        val testLabelId1 = LabelId("labelId1")
        val testLabelId2 = LabelId("labelId2")
        val testColorInt = 123
        val currentLabelsSelection = listOf(testLabelId1.id, testLabelId2.id)
        val sheetType = LabelType.FOLDER
        val label1 = mockk<LabelEntity> {
            every { id } returns testLabelId1
            every { type } returns LabelType.FOLDER
        }
        val label2 = mockk<LabelEntity> {
            every { id } returns testLabelId2
            every { type } returns LabelType.FOLDER
        }
        val uiLabel1 = LabelActonItemUiModel(
            testLabelId1.id,
            R.drawable.circle_labels_selection,
            "labelName1",
            null,
            testColorInt,
            true,
            LabelType.FOLDER.typeInt
        )
        val uiLabel2 = LabelActonItemUiModel(
            testLabelId2.id,
            R.drawable.circle_labels_selection,
            "labelName2",
            null,
            testColorInt,
            true,
            LabelType.FOLDER.typeInt
        )
        val expected = listOf(uiLabel1, uiLabel2) + getAllStandardFolders()
        coEvery { repository.findAllLabels(testUserId,) } returns listOf(label1, label2)
        every { labelsMapper.mapLabelToUi(label1, currentLabelsSelection, sheetType) } returns uiLabel1
        every { labelsMapper.mapLabelToUi(label2, currentLabelsSelection, sheetType) } returns uiLabel2


        // when
        val result = useCase.invoke(
            currentLabelsSelection,
            LabelType.FOLDER,
            Constants.MessageLocationType.INVALID
        )

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatFolderTypeAreReceivedAndProcessedCorrectlyAndCurrentInboxFolderIsOmitted() = runBlockingTest {
        // given
        val testLabelId1 = LabelId("labelId1")
        val testLabelId2 = LabelId("labelId2")
        val testColorInt = 123
        val currentLabelsSelection = listOf(testLabelId1.id, testLabelId2.id)
        val sheetType = LabelType.FOLDER
        val label1 = mockk<LabelEntity> {
            every { id } returns testLabelId1
            every { type } returns LabelType.FOLDER
        }
        val label2 = mockk<LabelEntity> {
            every { id } returns testLabelId2
            every { type } returns LabelType.FOLDER
        }
        val uiLabel1 = LabelActonItemUiModel(
            testLabelId1.id,
            R.drawable.circle_labels_selection,
            "labelName1",
            null,
            testColorInt,
            true,
            LabelType.FOLDER.typeInt
        )
        val uiLabel2 = LabelActonItemUiModel(
            testLabelId2.id,
            R.drawable.circle_labels_selection,
            "labelName2",
            null,
            testColorInt,
            true,
            LabelType.FOLDER.typeInt
        )
        val expected = listOf(uiLabel1, uiLabel2) + getAllStandardFolders()
            .filter { it.labelId != Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString() }
        coEvery { repository.findAllLabels(testUserId,) } returns listOf(label1, label2)
        every { labelsMapper.mapLabelToUi(label1, currentLabelsSelection, sheetType) } returns uiLabel1
        every { labelsMapper.mapLabelToUi(label2, currentLabelsSelection, sheetType) } returns uiLabel2


        // when
        val result = useCase.invoke(
            currentLabelsSelection,
            LabelType.FOLDER,
            Constants.MessageLocationType.INBOX
        )

        // then
        assertEquals(expected, result)
    }

    private fun getAllStandardFolders(): List<LabelActonItemUiModel> =
        StandardFolderLocation.values()
            .map { location ->
                LabelActonItemUiModel(
                    labelId = location.id,
                    iconRes = location.iconRes,
                    titleRes = location.title,
                    labelType = LabelType.FOLDER.typeInt
                )
            }


}
