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

package ch.protonmail.android.utils.ui.screen

import android.content.Context
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class RenderDimensionsProviderTest {

    private val defaultDisplayMock = mockk<Display>()
    private val windowManagerMock = mockk<WindowManager> {
        every { defaultDisplay } returns defaultDisplayMock
    }
    private val contextMock = mockk<Context> {
        every { getSystemService(android.view.WindowManager::class.java) } returns windowManagerMock
    }
    private val renderDimensionsProvider = RenderDimensionsProvider(contextMock)

    @Test
    fun `should calculate the rendered width`() {
        // given
        val displayWidth = 200
        val displayDensity = 2.0f
        val expectedRenderedWidth = 100
        every { defaultDisplayMock.getMetrics(any()) } answers {
            val metrics = it.invocation.args.first() as DisplayMetrics
            metrics.widthPixels = displayWidth
            metrics.density = displayDensity
        }

        // when
        val calculatedRenderedWidth = renderDimensionsProvider.getRenderWidth()

        // then
        assertEquals(expectedRenderedWidth, calculatedRenderedWidth)
    }
}
