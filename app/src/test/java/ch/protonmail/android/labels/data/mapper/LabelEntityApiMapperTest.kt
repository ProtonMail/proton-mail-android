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
import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import kotlin.test.Test

class LabelEntityApiMapperTest {

    private val testPath = "ParentFolder/MyFolder"
    private val testType = LabelType.FOLDER
    private val testParentId = "parentIdForTests"
    private val testUserId = UserId("TestUserId")
    private val labelsMapper = LabelEntityApiMapper()

    @Test
    fun mappingLabelApiModelToLabelEntitySucceedsWhenAllFieldsAreValid() {
        // given
        val serverLabel = getTestLabelApiModel(testId = "ID")

        // when
        val actual = labelsMapper.toEntity(serverLabel, testUserId)

        // then
        val expected = getTestLabelEntitiy(testId = "ID")
        assertEquals(expected, actual)
    }

    @Test
    fun mappingLabelEntityToServerLabelSucceedsWhenIdIsEmpty() {
        // given
        val contactLabel = getTestLabelEntitiy(testId = "")

        // when
        val actual = labelsMapper.toApiModel(contactLabel)

        // then
        val expected = getTestLabelApiModel(testId = "")
        assertEquals(expected, actual)
    }

    private fun getTestLabelApiModel(testId: String) = LabelApiModel(
        id = testId,
        name = "name",
        color = "color",
        order = 1,
        type = testType,
        expanded = 0,
        sticky = 0,
        path = testPath,
        notify = 0,
        parentId = testParentId
    )

    private fun getTestLabelEntitiy(testId: String) = LabelEntity(
        id = LabelId(testId),
        userId = testUserId,
        name = "name",
        color = "color",
        order = 1,
        type = testType,
        expanded = 0,
        sticky = 0,
        path = testPath,
        notify = 0,
        parentId = testParentId
    )
}
