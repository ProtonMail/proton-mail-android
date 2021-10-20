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
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.labels.utils.buildFolders
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import me.proton.core.util.kotlin.EMPTY_STRING
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TEST_LABEL_NAME = "label"
private val TEST_LABEL_ID = LabelId("label")
private const val TEST_COLOR_INT = 123

class LabelDomainActionItemUiMapperTest {

    private val context: Context = mockk {
        every { getColor(any()) } returns 0
    }
    private val mapper = LabelDomainActionItemUiMapper(context)


    @BeforeTest
    fun setUp() {
        mockkStatic(Color::class)
        every { Color.parseColor(any()) } returns TEST_COLOR_INT
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Color::class)
    }

    @Test
    fun labelIsMappedCorrectly() {

        // given
        val input = listOf(
            LabelOrFolderWithChildren.Label(
                id = TEST_LABEL_ID,
                name = TEST_LABEL_NAME,
                color = EMPTY_STRING,
            )
        )
        val expected = listOf(
            LabelActonItemUiModel(
                labelId = TEST_LABEL_ID,
                iconRes = R.drawable.circle_labels_selection,
                title = TEST_LABEL_NAME,
                titleRes = null,
                colorInt = TEST_COLOR_INT,
                folderLevel = 0,
                isChecked = false,
                labelType = LabelType.MESSAGE_LABEL
            )
        )

        // when
        val result = mapper.toUiModels(input, emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun labelIsCheckedCorrectly() {

        // given
        val input = listOf(buildLabel())
        val currentLabelsIds = listOf(TEST_LABEL_ID.id)

        // when
        val result = mapper.toUiModels(input, currentLabelsIds)

        // then
        assertEquals(true, result.first().isChecked)
    }

    @Test
    fun singleFolderIsMapperCorrectly() {

        // given
        val input = listOf(
            LabelOrFolderWithChildren.Folder(
                id = TEST_LABEL_ID,
                name = TEST_LABEL_NAME,
                color = EMPTY_STRING,
                path = TEST_LABEL_NAME,
                parentId = null,
                children = emptyList()
            )
        )
        val expected = listOf(
            LabelActonItemUiModel(
                labelId = TEST_LABEL_ID,
                iconRes = R.drawable.ic_folder_filled,
                title = TEST_LABEL_NAME,
                titleRes = null,
                colorInt = TEST_COLOR_INT,
                folderLevel = 0,
                isChecked = null,
                labelType = LabelType.FOLDER
            )
        )

        // when
        val result = mapper.toUiModels(input, emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun nestedFoldersAreMappedCorrectly() {
        // given
        val first = "first"
        val firstFirst = "first.first"
        val second = "second"
        val third = "third"
        val thirdFirst = "third.first"
        val thirdFirstFirst = "third.first.first"
        val thirdFirstSecond = "third.first.second"
        val thirdSecond = "third.second"
        val input = buildFolders {
            +first {
                +firstFirst
            }
            +second
            +third {
                +thirdFirst {
                    +thirdFirstFirst
                    +thirdFirstSecond
                }
                +thirdSecond
            }
        }
        val expected = listOf(
            buildActionItem(name = first, folderLevel = 0),
            buildActionItem(name = firstFirst, folderLevel = 1),
            buildActionItem(name = second, folderLevel = 0),
            buildActionItem(name = third, folderLevel = 0),
            buildActionItem(name = thirdFirst, folderLevel = 1),
            buildActionItem(name = thirdFirstFirst, folderLevel = 2),
            buildActionItem(name = thirdFirstSecond, folderLevel = 2),
            buildActionItem(name = thirdSecond, folderLevel = 1)
        )

        // when
        val result = mapper.toUiModels(input, emptyList())

        // then
        assertEquals(expected, result)
    }

    private fun buildLabel() = LabelOrFolderWithChildren.Label(
        id = TEST_LABEL_ID,
        name = TEST_LABEL_NAME,
        color = EMPTY_STRING
    )

    private fun buildActionItem(
        name: String,
        folderLevel: Int
    ) = LabelActonItemUiModel(
        labelId = LabelId(name),
        title = name,
        folderLevel = folderLevel,
        iconRes = R.drawable.ic_folder_filled,
        colorInt = TEST_COLOR_INT,
        labelType = LabelType.FOLDER
    )
}
