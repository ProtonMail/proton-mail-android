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

package ch.protonmail.android.api.models.contacts.receive

import ch.protonmail.android.core.Constants
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.model.Label
import ch.protonmail.android.labels.data.model.LabelId
import ch.protonmail.android.labels.domain.mapper.LabelsMapper
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.Test

class LabelsMapperTest {

    private lateinit var labelsMapper: LabelsMapper

    private val testPath = "a/bpath"
    private val testParentId = "parentIdForTests"
    private val testType = Constants.LABEL_TYPE_MESSAGE_FOLDERS
    private val testUserId = UserId("TestUserId")

    @BeforeTest
    fun setUp() {
        labelsMapper = LabelsMapper()
    }

    @Test
    fun mappingLabelEntityToServerLabelSucceedsWhenAllFieldsAreValid() {
        val contactLabel = LabelEntity(LabelId("ID"), testUserId, "name", "color", 1, testType, testPath,  testParentId, 0, 0, 0)

        val actual = labelsMapper.mapLabelEntityToServerLabel(contactLabel)

        val expected = Label(
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

        val actual = labelsMapper.mapLabelEntityToServerLabel(contactLabel)

        val expected = Label(
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

        val actual = labelsMapper.mapLabelEntityToServerLabel(contactLabel)

        val expected = Label(
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

    @Test
    fun mappingServerLabelToLabelEntitySucceedsWhenAllFieldsAreValid() {
        val serverLabel = Label(
            id = "ID",
            name = "name",
            color = "color",
            order = 1,
            type = Constants.LABEL_TYPE_MESSAGE_FOLDERS,
            path = testPath,
            notify = 0,
            expanded = null,
            sticky = null,
            parentId = testParentId
        )

        val actual = labelsMapper.mapLabelToLabelEntity(serverLabel, testUserId)

        val expected = LabelEntity(
            LabelId("ID"), testUserId, "name", "color", 1, Constants.LABEL_TYPE_MESSAGE_FOLDERS, testPath,
            testParentId, 0, 0, 0
        )
        assertEquals(expected, actual)
    }


}
