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
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.presentation.model.LabelIcon
import ch.protonmail.android.labels.presentation.model.LabelsManagerItemUiModel
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

class LabelsManagerItemUiModelMapperTest {

    private val context: Context = mockk {
        every { getColor(any()) } returns 0
    }
    private val mapper = LabelsManagerItemUiModelMapper(context)

    @BeforeTest
    fun setUp() {
        mockkStatic(Color::class)
        every { Color.parseColor(any()) } returns COLOR_INT
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Color::class)
    }

    @Test
    fun `single folder is mapped correctly`() {

        // given
        val input = listOf(buildFolder(name = LABEL_NAME))
        val expected = listOf(buildFolderUiModel(name = LABEL_NAME))

        // when
        val result = mapper.toUiModels(input, checkedLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `single label is mapped correctly`() {

        // given
        val input = listOf(buildLabel(name = LABEL_NAME))
        val expected = listOf(buildLabelUiModel(name = LABEL_NAME))

        // when
        val result = mapper.toUiModels(input, checkedLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `multiple labels are mapped correctly`() {
        // given
        val input = listOf(
            buildLabel("first"),
            buildLabel("second"),
            buildLabel("third")
        )
        val expected = listOf(
            buildLabelUiModel(name = "first"),
            buildLabelUiModel(name = "second"),
            buildLabelUiModel(name = "third")
        )

        // when
        val result = mapper.toUiModels(input, checkedLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `multiple folders with nested folders are mapped correctly`() {
        // given
        val input = buildFolders {
            folder("first") {
                folder("first.first")
            }
            folder("second")
            folder("third") {
                folder("third.first")
                folder("third.second")
            }
        }
        val expected = listOf(
            buildFolderUiModel(name = "first", folderLevel = 0, hasChildren = true),
            buildFolderUiModel(name = "first.first", folderLevel = 1),
            buildFolderUiModel(name = "second", folderLevel = 0),
            buildFolderUiModel(name = "third", folderLevel = 0, hasChildren = true),
            buildFolderUiModel(name = "third.first", folderLevel = 1),
            buildFolderUiModel(name = "third.second", folderLevel = 1)
        )

        // when
        val result = mapper.toUiModels(input, checkedLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `correct items are checked for labels`() {
        // given
        val input = listOf(
            buildLabel("first"),
            buildLabel("second"),
            buildLabel("third"),
            buildLabel("fourth")
        )
        val checkedLabels = listOf(
            LabelId("first"),
            LabelId("fourth")
        )
        val expected = listOf(
            buildLabelUiModel(name = "first", isChecked = true),
            buildLabelUiModel(name = "second"),
            buildLabelUiModel(name = "third"),
            buildLabelUiModel(name = "fourth", isChecked = true)
        )

        // when
        val result = mapper.toUiModels(input, checkedLabels = checkedLabels)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `correct items are checked for folders`() {
        // given
        val input = buildFolders {
            folder("first") {
                folder("first.first")
            }
            folder("second")
            folder("third") {
                folder("third.first")
            }
        }
        val checkedLabels = listOf(
            LabelId("first"),
            LabelId("third.first")
        )
        val expected = listOf(
            buildFolderUiModel(name = "first", folderLevel = 0, hasChildren = true, isChecked = true),
            buildFolderUiModel(name = "first.first", folderLevel = 1),
            buildFolderUiModel(name = "second", folderLevel = 0),
            buildFolderUiModel(name = "third", folderLevel = 0, hasChildren = true),
            buildFolderUiModel(name = "third.first", folderLevel = 1, isChecked = true)
        )

        // when
        val result = mapper.toUiModels(input, checkedLabels = checkedLabels)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `child folder color is used if defined`() {
        // given
        val parent = "parent"
        val child = "child"
        val redString = "red"
        val blueString = "blue"
        val redInt = 1
        val blueInt = 2

        val childFolder = buildFolder(name = child, color = blueString)
        val parentFolder = buildFolder(name = parent, color = redString, children = listOf(childFolder))
        val input = listOf(parentFolder)

        every { Color.parseColor(redString) } returns redInt
        every { Color.parseColor(blueString) } returns blueInt

        val expected = listOf(
            buildFolderUiModel(name = parent, folderLevel = 0, hasChildren = true, colorInt = redInt),
            buildFolderUiModel(name = child, folderLevel = 1, colorInt = blueInt),
        )

        // when
        val result = mapper.toUiModels(input, checkedLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `parent color is used if none defined`() {
        // given
        val parent = "parent"
        val child = "child"
        val redString = "red"
        val redInt = 1

        val childFolder = buildFolder(name = child, color = EMPTY_STRING)
        val parentFolder = buildFolder(name = parent, color = redString, children = listOf(childFolder))
        val input = listOf(parentFolder)

        every { Color.parseColor(redString) } returns redInt
        every { Color.parseColor(EMPTY_STRING) } answers {
            throw IllegalArgumentException("invalid color")
        }

        val expected = listOf(
            buildFolderUiModel(name = parent, folderLevel = 0, hasChildren = true, colorInt = redInt),
            buildFolderUiModel(name = child, folderLevel = 1, colorInt = redInt),
        )

        // when
        val result = mapper.toUiModels(input, checkedLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `default color is used if none defined for folder and parent`() {
        // given
        val parent = "parent"
        val child = "child"

        val childFolder = buildFolder(name = child, color = EMPTY_STRING)
        val parentFolder = buildFolder(name = parent, color = EMPTY_STRING, children = listOf(childFolder))
        val input = listOf(parentFolder)

        every { Color.parseColor(EMPTY_STRING) } answers {
            throw IllegalArgumentException("invalid color")
        }

        val expected = listOf(
            buildFolderUiModel(name = parent, folderLevel = 0, hasChildren = true, colorInt = 0),
            buildFolderUiModel(name = child, folderLevel = 1, colorInt = 0),
        )

        // when
        val result = mapper.toUiModels(input, checkedLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    companion object TestData {

        private const val LABEL_NAME = "label"
        private const val COLOR_INT = 123

        private fun buildLabel(
            name: String = LABEL_NAME,
            color: String = EMPTY_STRING,
        ) = LabelOrFolderWithChildren.Label(
            id = LabelId(name),
            name = name,
            color = color,
        )

        private fun buildFolder(
            name: String = LABEL_NAME,
            color: String = EMPTY_STRING,
            children: Collection<LabelOrFolderWithChildren.Folder> = emptyList()
        ) = LabelOrFolderWithChildren.Folder(
            id = LabelId(name),
            name = name,
            color = color,
            parentId = null,
            path = name,
            children = children
        )

        private fun buildLabelUiModel(
            name: String,
            colorInt: Int = COLOR_INT,
            isChecked: Boolean = false
        ) = LabelsManagerItemUiModel.Label(
            id = LabelId(name),
            name = name,
            icon = LabelIcon.Label(colorInt),
            isChecked = isChecked
        )

        private fun buildFolderUiModel(
            name: String,
            folderLevel: Int = 0,
            hasChildren: Boolean = false,
            colorInt: Int = COLOR_INT,
            isChecked: Boolean = false
        ) = LabelsManagerItemUiModel.Folder(
            id = LabelId(name),
            name = name,
            icon = buildFolderIcon(colorInt, hasChildren),
            parentId = null,
            folderLevel = folderLevel,
            isChecked = isChecked
        )

        private fun buildFolderIcon(colorInt: Int, hasChildren: Boolean): LabelIcon.Folder =
            if (hasChildren) LabelIcon.Folder.WithChildren.Colored(colorInt)
            else LabelIcon.Folder.WithoutChildren.Colored(colorInt)
    }
}
