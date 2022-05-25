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

package ch.protonmail.android.labels.presentation.mapper

import android.graphics.Color
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.utils.UiUtil
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import me.proton.core.util.kotlin.EMPTY_STRING
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LabelChipUiModelMapperTest {

    private val mapper = LabelChipUiModelMapper()

    @BeforeTest
    fun setup() {
        mockkStatic(Color::class, UiUtil::class)
        every { Color.parseColor(NORMALIZED_COLOR) } returns TEST_COLOR_INT
        every { UiUtil.normalizeColor(TEST_COLOR_STRING) } returns NORMALIZED_COLOR
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Color::class, UiUtil::class)
    }

    @Test
    fun `correctly maps a single label`() {

        // given
        val labelName = "label"
        val label = buildLabel(labelName)
        val expected = LabelChipUiModel(
            id = LabelId(labelName),
            name = Name(labelName),
            color = TEST_COLOR_INT
        )

        // when
        val result = mapper.toUiModel(label)

        // then
        assertEquals(expected, result)
        verify {
            UiUtil.normalizeColor(TEST_COLOR_STRING)
            Color.parseColor(NORMALIZED_COLOR)
        }
    }

    companion object TestData {

        private const val TEST_COLOR_INT = 123
        private const val TEST_COLOR_STRING = "label color"
        private const val NORMALIZED_COLOR = "normalized color"

        private fun buildLabel(name: String) = Label(
            id = LabelId(name),
            name = name,
            color = TEST_COLOR_STRING,
            order = 0,
            type = LabelType.MESSAGE_LABEL,
            path = name,
            parentId = EMPTY_STRING
        )
    }
}
