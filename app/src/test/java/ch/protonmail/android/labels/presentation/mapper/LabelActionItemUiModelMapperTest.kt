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

import android.content.Context
import android.graphics.Color
import ch.protonmail.android.R
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.model.LabelId
import ch.protonmail.android.labels.data.model.LabelType
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LabelActionItemUiModelMapperTest {

    private val defaultColorInt = 890
    private val testColorInt = 123

    private val context: Context = mockk {
        every { getColor(any()) } returns defaultColorInt
    }
    private val mapper = LabelActionItemUiModelMapper(context)

    private val testUserId = UserId("testUserId")

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
        val labelId1 = LabelId("asdasdad")
        val labelName1 = "name1"
        val labelColor1 = "olive"
        val label = LabelEntity(
            labelId1,
            testUserId,
            labelName1,
            labelColor1,
            order = 0,
            type = LabelType.MESSAGE_LABEL,
            path = "a/b",
            parentId = "parentId",
            expanded = 0,
            sticky = 0,
            notify = 0
        )
        val currentLabelsIds = listOf(labelId1.id)
        val sheetType = LabelsActionSheet.Type.LABEL
        val expected = LabelActonItemUiModel(
            labelId1.id,
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
        val labelId1 = LabelId("asdasdad")
        val labelName1 = "name1"
        val labelColor1 = "olive"
        val label = LabelEntity(
            labelId1,
            testUserId,
            labelName1,
            labelColor1,
            order = 0,
            type = LabelType.MESSAGE_LABEL,
            path = "a/b",
            parentId = "parentId",
            expanded = 0,
            sticky = 0,
            notify = 0
        )
        val currentLabelsIds = listOf(labelId1.id)
        val sheetType = LabelsActionSheet.Type.FOLDER
        val expected = LabelActonItemUiModel(
            labelId1.id,
            R.drawable.ic_folder_filled,
            labelName1,
            null,
            testColorInt,
            null,
            LabelsActionSheet.Type.FOLDER.typeInt
        )

        // when
        val result = mapper.mapLabelToUi(label, currentLabelsIds, sheetType)

        // then
        assertEquals(expected, result)
    }
}
