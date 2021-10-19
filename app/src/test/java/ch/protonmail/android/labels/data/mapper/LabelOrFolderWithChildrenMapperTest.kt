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

package ch.protonmail.android.labels.data.mapper

import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.utils.buildFolders
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import kotlin.test.Test
import kotlin.test.assertEquals

private val TEST_USER_ID = UserId("user")

class LabelOrFolderWithChildrenMapperTest {

    private val mapper = LabelOrFolderWithChildrenMapper(TestDispatcherProvider)

    @Test
    fun takesBothFoldersAndLabels() = runBlockingTest {
        // given
        val first = "first"
        val second = "second"
        val input = listOf(
            buildLabelEntity(first, LabelType.FOLDER),
            buildLabelEntity(second, LabelType.MESSAGE_LABEL),
        )
        val expected = listOf(buildLabel(second)) + buildFolders { +first }

        // when
        val result = mapper.toLabelsAndFoldersWithChildren(input)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun singleParentAndSingleChild() = runBlockingTest {
        // given
        val parent = "parent"
        val child = "child"
        val input = listOf(
            buildLabelEntity(parent),
            buildLabelEntity(child, parent = parent)
        )
        val expected = buildFolders {
            +parent {
                +child
            }
        }

        // when
        val result = mapper.toLabelsAndFoldersWithChildren(input)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun threeLevelSingleParentAndSingleChildAndSingleGrandchild() = runBlockingTest {
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
            +parent {
                +child {
                    +grandchild
                }
            }
        }

        // when
        val result = mapper.toLabelsAndFoldersWithChildren(input)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun complexHierarchy() = runBlockingTest {
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
        val forth = "forth"
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
            buildLabelEntity(forth)
        )
        val expected = buildFolders {
            +first
            +second {
                +secondFirst
                +secondSecond
            }
            +third {
                +thirdFirst
                +thirdSecond {
                    +thirdSecondFirst
                    +thirdSecondSecond
                    +thirdSecondThird
                }
                +thirdThird {
                    +thirdThirdFirst
                    +thirdThirdSecond
                    +thirdThirdThird
                }
            }
            +forth
        }

        // when
        val result = mapper.toLabelsAndFoldersWithChildren(input)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun orphanChildrenAreIgnored() = runBlockingTest {
        // given
        val parent = "parent"
        val child = "child"
        val input = listOf(
            buildLabelEntity(parent),
            buildLabelEntity("orphan", parent = "missing"),
            buildLabelEntity(child, parent = parent)
        )
        val expected = buildFolders {
            +parent {
                +child
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
