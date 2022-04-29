/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.metrics.domain

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import me.proton.core.domain.entity.UserId
import me.proton.core.metrics.domain.MetricsManager
import me.proton.core.metrics.domain.entity.Metrics
import kotlin.test.Test

/**
 * Tests the behaviour of [SendMetricsForViewInDarkModePreference]
 */
class SendMetricsForViewInDarkModePreferenceTest {

    private val userId = UserId("userId")

    private val metricsManager: MetricsManager = mockk {
        every { send(userId, any()) } just runs
    }
    private val sendMetricsForViewInDarkModePreference = SendMetricsForViewInDarkModePreference(metricsManager)

    @Test
    fun `should send metrics about using dark mode when view in dark mode preference is true`() {
        // given
        val expectedResult = Metrics(
            METRICS_LOG_TAG,
            DARK_STYLES_METRICS_TITLE,
            JsonObject(mapOf(METRICS_ACTION to JsonPrimitive(APPLY_DARK_STYLES_METRICS_ACTION)))
        )

        // when
        sendMetricsForViewInDarkModePreference(userId, viewInDarkMode = true)

        // then
        verify { metricsManager.send(userId, expectedResult) }
    }

    @Test
    fun `should send metrics about not using dark mode when view in dark mode preference is false`() {
        // given
        val expectedResult = Metrics(
            METRICS_LOG_TAG,
            DARK_STYLES_METRICS_TITLE,
            JsonObject(mapOf(METRICS_ACTION to JsonPrimitive(REMOVE_DARK_STYLES_METRICS_ACTION)))
        )

        // when
        sendMetricsForViewInDarkModePreference(userId, viewInDarkMode = false)

        // then
        verify { metricsManager.send(userId, expectedResult) }
    }
}
