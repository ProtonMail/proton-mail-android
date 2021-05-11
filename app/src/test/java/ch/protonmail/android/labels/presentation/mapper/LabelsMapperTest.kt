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

package ch.protonmail.android.labels.presentation.mapper

import android.graphics.Color
import ch.protonmail.android.R
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.labels.presentation.model.ManageLabelItemUiModel
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LabelsMapperTest {

    val mapper = LabelsMapper()

    val testColorInt = 123

    @BeforeTest
    fun setUp() {
        mockkStatic(Color::class)
        every { Color.parseColor(any()) } returns testColorInt
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Color::class)
    }

    @Test
    fun verifyThatLabelTypeMessagesAreMappedCorrectly() {

        // given
        val labelId1 = "asdasdad"
        val labelName1 = "name1"
        val labelColor1 = "olive"
        val label = Label(labelId1, labelName1, labelColor1)
        val currentLabelsIds = listOf(labelId1)
        val sheetType = LabelsActionSheet.Type.LABEL
        val expected = ManageLabelItemUiModel(
            labelId1,
            R.drawable.circle_labels_selection,
            labelName1,
            null,
            testColorInt,
            true,
            LabelsActionSheet.Type.LABEL.typeInt
        )

        // when
        val result = mapper.mapLabelToUi(label, currentLabelsIds, sheetType)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatFolderTypeMessagesAreMappedCorrectly() {

        // given
        val labelId1 = "asdasdad"
        val labelName1 = "name1"
        val labelColor1 = "olive"
        val label = Label(labelId1, labelName1, labelColor1)
        val currentLabelsIds = listOf(labelId1)
        val sheetType = LabelsActionSheet.Type.FOLDER
        val expected = ManageLabelItemUiModel(
            labelId1,
            R.drawable.ic_folder,
            labelName1,
            null,
            Color.BLACK,
            null,
            LabelsActionSheet.Type.FOLDER.typeInt
        )

        // when
        val result = mapper.mapLabelToUi(label, currentLabelsIds, sheetType)

        // then
        assertEquals(expected, result)
    }
}
