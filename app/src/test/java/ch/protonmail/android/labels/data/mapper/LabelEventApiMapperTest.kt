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

import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.data.remote.model.LabelEventModel
import ch.protonmail.android.labels.domain.model.LabelType
import kotlin.test.Test
import kotlin.test.assertEquals

class LabelEventApiMapperTest{

    private val labelEventApiMapper = LabelEventApiMapper()

    @Test
    fun validateAllFieldsAreValidAfterToApiModelIsCalled() {
        // given
        val eventModel = LabelEventModel(
            id = "id",
            name = "name",
            path = "name",
            type = 3,
            color = "#e6c04c",
            order = 320000,
            notify = 0,
            expanded = null,
            sticky = null,
            parentId = null
        )
        val expectedResult = LabelApiModel(
            id = "id",
            name = "name",
            path = "name",
            type = LabelType.FOLDER,
            color = "#e6c04c",
            order = 320000,
            notify = 0,
            expanded = null,
            sticky = null,
            parentId = null
        )

        // when
        val result = labelEventApiMapper.toApiModel(eventModel)

        // then
        assertEquals(expectedResult, result)
    }
}
