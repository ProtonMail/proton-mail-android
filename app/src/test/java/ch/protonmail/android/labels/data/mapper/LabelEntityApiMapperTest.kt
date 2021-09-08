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
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.data.remote.model.LabelApiModel
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
        val serverLabel = LabelApiModel(
            id = "ID",
            name = "name",
            color = "color",
            order = 1,
            type = LabelType.FOLDER,
            path = testPath,
            notify = 0,
            expanded = null,
            sticky = null,
            parentId = testParentId
        )

        val actual = labelsMapper.toEntity(serverLabel, testUserId)

        val expected = LabelEntity(
            LabelId("ID"), testUserId, "name", "color", 1, LabelType.FOLDER, testPath,
            testParentId, 0, 0, 0
        )
        assertEquals(expected, actual)
    }

    @Test
    fun mappingLabelEntityToServerLabelSucceedsWhenAllFieldsAreValid() {
        val contactLabel = LabelEntity(LabelId("ID"), testUserId, "name", "color", 1, testType, testPath,  testParentId, 0, 0, 0)

        val actual = labelsMapper.toApiModel(contactLabel)

        val expected = LabelApiModel(
            id = "ID",
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
        assertEquals(expected, actual)
    }

    @Test
    fun mappingLabelEntityToServerLabelSucceedsWhenSomeFieldsAreNotPassedExplicitly() {
        val contactLabel = LabelEntity(LabelId("ID"), testUserId, "name", "color", 1, testType, testPath,  testParentId, 0, 0, 0)

        val actual = labelsMapper.toApiModel(contactLabel)

        val expected = LabelApiModel(
            id = "ID",
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
        assertEquals(expected, actual)
    }

    @Test
    fun mappingLabelEntityToServerLabelSucceedsWhenIdIsEmpty() {
        val contactLabel = LabelEntity(LabelId(""), testUserId, "name", "color", 1, testType, testPath,  testParentId, 0, 0, 0)

        val actual = labelsMapper.toApiModel(contactLabel)

        val expected = LabelApiModel(
            id = "",
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
        assertEquals(expected, actual)
    }
}
