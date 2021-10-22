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

package ch.protonmail.android.drawer.presentation.mapper

import android.graphics.Color
import ch.protonmail.android.R
import ch.protonmail.android.drawer.presentation.mapper.DrawerLabelUiModelMapper.Companion.AQUA_BASE_V3_COLOR
import ch.protonmail.android.drawer.presentation.mapper.DrawerLabelUiModelMapper.Companion.SAGE_BASE_V3_COLOR
import ch.protonmail.android.drawer.presentation.model.DrawerLabelUiModel
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.model.LabelType
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
private const val TEST_COLOR = "#123"
private const val DUMMY_PARSED_COLOR = 0
private const val DUMMY_AQUA_BASE_COLOR = 1357
private const val DUMMY_SAGE_BASE_COLOR = 2468
private const val DUMMY_ICON_INVERTED_COLOR = 123

class DrawerLabelUiModelMapperTest {

    private val mapper = DrawerLabelUiModelMapper(
        context = mockk {
            every { getColor(R.color.aqua_base) } returns DUMMY_AQUA_BASE_COLOR
            every { getColor(R.color.sage_base) } returns DUMMY_SAGE_BASE_COLOR
            every { getColor(R.color.icon_inverted) } returns DUMMY_ICON_INVERTED_COLOR
        }
    )

    @BeforeTest
    fun setup() {
        mockkStatic(Color::class)
        every { Color.parseColor(any()) } answers {
            val stringColorArg = firstArg<String>()
            val basicColorRegex = """#(\d{3,8})""".toRegex()
            if (basicColorRegex.matches(stringColorArg).not()) {
                throw IllegalArgumentException("Invalid color format")
            }
            DUMMY_PARSED_COLOR
        }
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Color::class)
    }

    @Test
    fun singleLabelIsMapperCorrectly() {
        // given
        val name = "label"
        val input = buildLabel(name)
        val expected = listOf(
            DrawerLabelUiModel(
                labelId = name,
                name = name,
                icon = DrawerLabelUiModel.Icon(R.drawable.shape_ellipse, DUMMY_PARSED_COLOR),
                type = LabelType.MESSAGE_LABEL,
                folderLevel = 0
            )
        )

        // when
        val result = mapper.toUiModels(input)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun singleFolderIsMapperCorrectly() {
        // given
        val name = "folder"
        val input = buildFolders { folder(name) }
        val expected = listOf(
            DrawerLabelUiModel(
                labelId = name,
                name = name,
                icon = DrawerLabelUiModel.Icon(R.drawable.ic_folder_filled, DUMMY_ICON_INVERTED_COLOR),
                type = LabelType.FOLDER,
                folderLevel = 0
            )
        )

        // when
        val result = mapper.toUiModels(input.first())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun worksCorrectlyWithCorrectColor() {
        // given
        val input = buildLabel(color = "#123")

        // when
        val result = mapper.toUiModel(input)

        // then
        assertEquals(DUMMY_PARSED_COLOR, result.icon.colorInt)
    }

    @Test
    fun defaultColorIsSetIfColorIsEmpty() {
        // given
        val input = buildLabel(color = EMPTY_STRING)

        // when
        val result = mapper.toUiModel(input)

        // then
        assertEquals(DUMMY_ICON_INVERTED_COLOR, result.icon.colorInt)
    }

    @Test
    fun defaultColorIsSetIfColorCantBeParsed() {
        // given
        val input = buildLabel(color = "incorrect")

        // when
        val result = mapper.toUiModel(input)

        // then
        assertEquals(DUMMY_ICON_INVERTED_COLOR, result.icon.colorInt)
    }

    @Test
    fun aquaBaseColorIsReplacedCorrectly() {
        // given
        val input = buildLabel(color = AQUA_BASE_V3_COLOR)

        // when
        val result = mapper.toUiModel(input)

        // then
        assertEquals(DUMMY_AQUA_BASE_COLOR, result.icon.colorInt)
    }

    @Test
    fun sageBaseColorIsReplacedCorrectly() {
        // given
        val input = buildLabel(color = SAGE_BASE_V3_COLOR)

        // when
        val result = mapper.toUiModel(input)

        // then
        assertEquals(DUMMY_SAGE_BASE_COLOR, result.icon.colorInt)
    }

    private fun buildLabel(
        name: String = TEST_LABEL_NAME,
        color: String = TEST_COLOR
    ) = LabelOrFolderWithChildren.Label(
        id = LabelId(name),
        name = name,
        color = color
    )
}
