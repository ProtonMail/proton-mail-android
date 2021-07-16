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
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.drawer.presentation.mapper.DrawerLabelUiModelMapper.Companion.AQUA_BASE_V3_COLOR
import ch.protonmail.android.drawer.presentation.mapper.DrawerLabelUiModelMapper.Companion.SAGE_BASE_V3_COLOR
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.invoke
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for [DrawerLabelUiModelMapper]
 */
class DrawerLabelUiModelMapperTest {

    private val dummyParsedColor = 0
    private val dummyAquaBaseColor = 1357
    private val dummySageBaseColor = 2468
    private val dummyIconInvertedColor = 123

    private val mapper = DrawerLabelUiModelMapper(
        context = mockk {
            every { getColor(R.color.aqua_base) } returns dummyAquaBaseColor
            every { getColor(R.color.sage_base) } returns dummySageBaseColor
            every { getColor(R.color.icon_inverted) } returns dummyIconInvertedColor
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
            dummyParsedColor
        }
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Color::class)
    }

    @Test
    fun worksCorrectlyWithCorrectColor() {
        // given
        val input = mockk<Label>(relaxed = true) {
            every { color } returns "#123"
        }

        // when
        val result = mapper { input.toUiModel() }

        // then
        assertEquals(dummyParsedColor, result.icon.colorInt)
    }

    @Test
    fun defaultColorIsSetIfColorIsEmpty() {
        // given
        val input = mockk<Label>(relaxed = true) {
            every { color } returns EMPTY_STRING
        }

        // when
        val result = mapper { input.toUiModel() }

        // then
        assertEquals(dummyIconInvertedColor, result.icon.colorInt)
    }

    @Test
    fun defaultColorIsSetIfColorCantBeParsed() {
        // given
        val input = mockk<Label>(relaxed = true) {
            every { color } returns "incorrect"
        }

        // when
        val result = mapper { input.toUiModel() }

        // then
        assertEquals(dummyIconInvertedColor, result.icon.colorInt)
    }

    @Test
    fun aquaBaseColorIsReplacedCorrectly() {
        // given
        val input = mockk<Label>(relaxed = true) {
            every { color } returns AQUA_BASE_V3_COLOR
        }

        // when
        val result = mapper { input.toUiModel() }

        // then
        assertEquals(dummyAquaBaseColor, result.icon.colorInt)
    }

    @Test
    fun sageBaseColorIsReplacedCorrectly() {
        // given
        val input = mockk<Label>(relaxed = true) {
            every { color } returns SAGE_BASE_V3_COLOR
        }

        // when
        val result = mapper { input.toUiModel() }

        // then
        assertEquals(dummySageBaseColor, result.icon.colorInt)
    }
}
