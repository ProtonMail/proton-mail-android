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
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerItemUiModel
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerItemUiModel.Folder.Icon
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

class ParentFolderPickerItemUiModelMapperTest {

    private val context: Context = mockk {
        every { getColor(any()) } returns 0
    }
    private val mapper = ParentFolderPickerItemUiModelMapper(context)


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
        val expected = listOf(
            ParentFolderPickerItemUiModel.None(isSelected = true),
            buildFolderUiModel(name = LABEL_NAME)
        )

        // when
        val result = mapper.toUiModels(
            input,
            currentFolder = ANOTHER_FOLDER_ID,
            selectedParentFolder = null,
            includeNoneUiModel = true
        )

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `nested folders are mapped correctly`() {
        // given
        val first = "first"
        val firstFirst = "first.first"
        val second = "second"
        val third = "third"
        val thirdFirst = "third.first"
        val thirdSecond = "third.second"
        val input = buildFolders {
            folder(first) {
                folder(firstFirst)
            }
            folder(second)
            folder(third) {
                folder(thirdFirst)
                folder(thirdSecond)
            }
        }
        val expected = listOf(
            ParentFolderPickerItemUiModel.None(isSelected = true),
            buildFolderUiModel(name = first, folderLevel = 0, hasChildren = true),
            buildFolderUiModel(name = firstFirst, folderLevel = 1),
            buildFolderUiModel(name = second, folderLevel = 0),
            buildFolderUiModel(name = third, folderLevel = 0, hasChildren = true),
            buildFolderUiModel(name = thirdFirst, folderLevel = 1),
            buildFolderUiModel(name = thirdSecond, folderLevel = 1)
        )

        // when
        val result = mapper.toUiModels(
            input,
            currentFolder = ANOTHER_FOLDER_ID,
            selectedParentFolder = null,
            includeNoneUiModel = true
        )

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `correct folders is selected`() {
        // given
        val first = "first"
        val firstFirst = "first.first"
        val second = "second"
        val third = "third"
        val thirdFirst = "third.first"
        val input = buildFolders {
            folder(first) {
                folder(firstFirst)
            }
            folder(second)
            folder(third) {
                folder(thirdFirst)
            }
        }
        val expected = listOf(
            ParentFolderPickerItemUiModel.None(isSelected = false),
            buildFolderUiModel(name = first, folderLevel = 0, hasChildren = true),
            buildFolderUiModel(name = firstFirst, folderLevel = 1, isSelected = true),
            buildFolderUiModel(name = second, folderLevel = 0),
            buildFolderUiModel(name = third, folderLevel = 0, hasChildren = true),
            buildFolderUiModel(name = thirdFirst, folderLevel = 1)
        )

        // when
        val result = mapper.toUiModels(
            input,
            currentFolder = ANOTHER_FOLDER_ID,
            selectedParentFolder = LabelId(firstFirst),
            includeNoneUiModel = true
        )

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
            ParentFolderPickerItemUiModel.None(isSelected = true),
            buildFolderUiModel(name = parent, folderLevel = 0, hasChildren = true, colorInt = redInt),
            buildFolderUiModel(name = child, folderLevel = 1, colorInt = blueInt),
        )

        // when
        val result = mapper.toUiModels(
            input,
            currentFolder = ANOTHER_FOLDER_ID,
            selectedParentFolder = null,
            includeNoneUiModel = true
        )

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
            ParentFolderPickerItemUiModel.None(isSelected = true),
            buildFolderUiModel(name = parent, folderLevel = 0, hasChildren = true, colorInt = redInt),
            buildFolderUiModel(name = child, folderLevel = 1, colorInt = redInt),
        )

        // when
        val result = mapper.toUiModels(
            input,
            currentFolder = ANOTHER_FOLDER_ID,
            selectedParentFolder = null,
            includeNoneUiModel = true
        )

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
            ParentFolderPickerItemUiModel.None(isSelected = true),
            buildFolderUiModel(name = parent, folderLevel = 0, hasChildren = true, colorInt = 0),
            buildFolderUiModel(name = child, folderLevel = 1, colorInt = 0),
        )

        // when
        val result = mapper.toUiModels(
            input,
            currentFolder = ANOTHER_FOLDER_ID,
            selectedParentFolder = null,
            includeNoneUiModel = true
        )

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `set not enabled current folder and its children`() {
        // given
        val input = buildFolders {
            folder("first") {
                folder("first.first")
            }
            folder("second") {
                folder("second.first")
                folder("second.second")
            }
        }
        val expected = listOf(
            buildFolderUiModel(name = "first", folderLevel = 0, hasChildren = true, isEnabled = true),
            buildFolderUiModel(name = "first.first", folderLevel = 1, hasChildren = false, isEnabled = true),
            buildFolderUiModel(name = "second", folderLevel = 0, hasChildren = true, isEnabled = false),
            buildFolderUiModel(name = "second.first", folderLevel = 1, hasChildren = false, isEnabled = false),
            buildFolderUiModel(name = "second.second", folderLevel = 1, hasChildren = false, isEnabled = false),
        )

        // when
        val result = mapper.toUiModels(
            input,
            currentFolder = LabelId("second"),
            selectedParentFolder = null,
            includeNoneUiModel = false
        )

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `set not enabled current folder when it's a child`() {
        // given
        val input = buildFolders {
            folder("first") {
                folder("first.first")
            }
            folder("second") {
                folder("second.first")
                folder("second.second")
            }
        }
        val expected = listOf(
            buildFolderUiModel(name = "first", folderLevel = 0, hasChildren = true, isEnabled = true),
            buildFolderUiModel(name = "first.first", folderLevel = 1, hasChildren = false, isEnabled = true),
            buildFolderUiModel(name = "second", folderLevel = 0, hasChildren = true, isEnabled = true),
            buildFolderUiModel(name = "second.first", folderLevel = 1, hasChildren = false, isEnabled = false),
            buildFolderUiModel(name = "second.second", folderLevel = 1, hasChildren = false, isEnabled = true),
        )

        // when
        val result = mapper.toUiModels(
            input,
            currentFolder = LabelId("second.first"),
            selectedParentFolder = null,
            includeNoneUiModel = false
        )

        // then
        assertEquals(expected, result)
    }

    companion object TestData {

        private const val LABEL_NAME = "label"
        private const val COLOR_INT = 123
        private val ANOTHER_FOLDER_ID = LabelId("another")

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

        private fun buildFolderUiModel(
            name: String,
            folderLevel: Int = 0,
            hasChildren: Boolean = false,
            colorInt: Int = COLOR_INT,
            isSelected: Boolean = false,
            isEnabled: Boolean = true
        ) = ParentFolderPickerItemUiModel.Folder(
            id = LabelId(name),
            name = name,
            icon = buildIcon(colorInt, hasChildren),
            folderLevel = folderLevel,
            isSelected = isSelected,
            isEnabled = isEnabled
        )

        private fun buildIcon(colorInt: Int, hasChildren: Boolean): Icon {
            val (drawableRes, contentDescriptionRes) =
                if (hasChildren) {
                    Icon.WITH_CHILDREN_COLORED_ICON_RES to Icon.WITH_CHILDREN_CONTENT_DESCRIPTION_RES
                } else {
                    Icon.WITHOUT_CHILDREN_COLORED_ICON_RES to Icon.WITHOUT_CHILDREN_CONTENT_DESCRIPTION_RES
                }
            return Icon(
                drawableRes = drawableRes,
                colorInt = colorInt,
                contentDescriptionRes = contentDescriptionRes
            )
        }
    }
}
