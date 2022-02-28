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

package ch.protonmail.android.utils.ui.screen

import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class RenderDimensionsProviderTest {

    private val defaultDisplayMock = mockk<Display>()
    private val windowManagerMock = mockk<WindowManager> {
        @Suppress("DEPRECATION")
        every { defaultDisplay } returns defaultDisplayMock
    }
    private val activity = mockk<FragmentActivity> {
        every { getSystemService<WindowManager>() } returns windowManagerMock
    }
    private val renderDimensionsProvider = RenderDimensionsProvider()

    @Test
    fun `should calculate the rendered width`() {
        // given
        val displayWidth = 200
        val displayDensity = 2.0f
        val expectedRenderedWidth = 100
        @Suppress("DEPRECATION")
        every { defaultDisplayMock.getMetrics(any()) } answers {
            val metrics = it.invocation.args.first() as DisplayMetrics
            metrics.widthPixels = displayWidth
            metrics.density = displayDensity
        }

        // when
        val calculatedRenderedWidth = renderDimensionsProvider.getRenderWidth(activity)

        // then
        assertEquals(expectedRenderedWidth, calculatedRenderedWidth)
    }
}
