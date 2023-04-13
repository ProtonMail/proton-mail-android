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

package ch.protonmail.android.feature.rating

import android.content.Context
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Test

class StartRateAppFlowTest {

    private val context: Context = mockk(relaxed = true)

    private val startRateAppFlow = StartRateAppFlow(context)

    @After
    fun tearDown() {
        unmockkStatic(ReviewManagerFactory::class)
    }

    @Test
    fun `request review flow from google play review manager`() {
        // given
        val reviewMangerMock = mockk<ReviewManager> {
            every { this@mockk.requestReviewFlow() } returns mockk(relaxed = true)
        }
        mockkStatic(ReviewManagerFactory::class)
        every { ReviewManagerFactory.create(context) } returns reviewMangerMock

        // when
        startRateAppFlow()

        // then
        verify { reviewMangerMock.requestReviewFlow() }
    }
}