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

package ch.protonmail.android.labels.data.mapper

import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.utils.buildFolders
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import kotlin.test.Test
import kotlin.test.assertEquals

private val TEST_USER_ID = UserId("user")

class LabelOrFolderWithChildrenMapperTest {

    private val dispatchers = TestDispatcherProvider()

    private val mapper = LabelOrFolderWithChildrenMapper(dispatchers)

    @Test
    fun takesBothFoldersAndLabels() = runTest(dispatchers.Main) {
        // given
        val first = "first"
        val second = "second"
        val input = listOf(
            buildLabelEntity(first, LabelType.FOLDER),
            buildLabelEntity(second, LabelType.MESSAGE_LABEL),
        )
        val expected = listOf(buildLabel(second)) + buildFolders { folder(first) }

        // when
        val result = mapper.toLabelsAndFoldersWithChildren(input)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun singleParentAndSingleChild() = runTest(dispatchers.Main) {
        // given
        val parent = "parent"
        val child = "child"
        val input = listOf(
            buildLabelEntity(parent),
            buildLabelEntity(child, parent = parent)
        )
        val expected = buildFolders {
            folder(parent) {
                folder(child)
            }
        }

        // when
        val result = mapper.toLabelsAndFoldersWithChildren(input)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun threeLevelSingleParentAndSingleChildAndSingleGrandchild() = runTest(dispatchers.Main) {
        // given
        val parent = "parent"
        val child = "child"
        val grandchild = "grandchild"
        val input = listOf(
            buildLabelEntity(parent),
            buildLabelEntity(child, parent = parent),
            buildLabelEntity(grandchild, parent = child)
        )
        val expected = buildFolders {
            folder(parent) {
                folder(child) {
                    folder(grandchild)
                }
            }
        }

        // when
        val result = mapper.toLabelsAndFoldersWithChildren(input)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun mapsComplexHierarchyOfFolders() = runTest(dispatchers.Main) {
        // given
        val first = "first"
        val second = "second"
        val secondFirst = "second-first"
        val secondSecond = "second-first"
        val third = "third"
        val thirdFirst = "third-first"
        val thirdSecond = "third-second"
        val thirdSecondFirst = "third-second-first"
        val thirdSecondSecond = "third-second-second"
        val thirdSecondThird = "third-second-third"
        val thirdThird = "third-third"
        val thirdThirdFirst = "third-third-first"
        val thirdThirdSecond = "third-third-second"
        val thirdThirdThird = "third-third-third"
        val fourth = "fourth"
        val input = listOf(
            buildLabelEntity(first),
            buildLabelEntity(second),
            buildLabelEntity(secondFirst, parent = second),
            buildLabelEntity(secondSecond, parent = second),
            buildLabelEntity(third),
            buildLabelEntity(thirdFirst, parent = third),
            buildLabelEntity(thirdSecond, parent = third),
            buildLabelEntity(thirdSecondFirst, parent = thirdSecond),
            buildLabelEntity(thirdSecondSecond, parent = thirdSecond),
            buildLabelEntity(thirdSecondThird, parent = thirdSecond),
            buildLabelEntity(thirdThird, parent = third),
            buildLabelEntity(thirdThirdFirst, parent = thirdThird),
            buildLabelEntity(thirdThirdSecond, parent = thirdThird),
            buildLabelEntity(thirdThirdThird, parent = thirdThird),
            buildLabelEntity(fourth)
        )
        val expected = buildFolders {
            folder(first)
            folder(second) {
                folder(secondFirst)
                folder(secondSecond)
            }
            folder(third) {
                folder(thirdFirst)
                folder(thirdSecond) {
                    folder(thirdSecondFirst)
                    folder(thirdSecondSecond)
                    folder(thirdSecondThird)
                }
                folder(thirdThird) {
                    folder(thirdThirdFirst)
                    folder(thirdThirdSecond)
                    folder(thirdThirdThird)
                }
            }
            folder(fourth)
        }

        // when
        val result = mapper.toLabelsAndFoldersWithChildren(input)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun orphanFoldersAreIgnoredFromTheResult() = runTest(dispatchers.Main) {
        // given
        val parent = "parent"
        val child = "child"
        val input = listOf(
            buildLabelEntity(parent),
            buildLabelEntity("orphan", parent = "missing"),
            buildLabelEntity(child, parent = parent)
        )
        val expected = buildFolders {
            folder(parent) {
                folder(child)
            }
        }

        // when
        val result = mapper.toLabelsAndFoldersWithChildren(input)

        // then
        assertEquals(expected, result)
    }

    private fun buildLabelEntity(
        name: String,
        type: LabelType = LabelType.FOLDER,
        parent: String = EMPTY_STRING
    ) = LabelEntity(
        id = LabelId(name),
        userId = TEST_USER_ID,
        name = name,
        color = EMPTY_STRING,
        order = 0,
        type = type,
        path = EMPTY_STRING,
        parentId = parent,
        expanded = 0,
        sticky = 0,
        notify = 0
    )

    private fun buildLabel(name: String) = LabelOrFolderWithChildren.Label(
        id = LabelId(name),
        name = name,
        color = EMPTY_STRING
    )
}
