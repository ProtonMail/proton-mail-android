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

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import me.proton.core.domain.entity.UserId
import me.proton.core.metrics.domain.MetricsManager
import me.proton.core.metrics.domain.entity.Metrics
import javax.inject.Inject

const val METRICS_ACTION = "action"
const val METRICS_LOG_TAG = "dark_styles"
const val DARK_STYLES_METRICS_TITLE = "update_dark_styles"
const val APPLY_DARK_STYLES_METRICS_ACTION = "apply_dark_styles"
const val REMOVE_DARK_STYLES_METRICS_ACTION = "remove_dark_styles"

class SendMetricsForViewInDarkModePreference @Inject constructor(
    private val metricsManager: MetricsManager
) {

    operator fun invoke(userId: UserId, viewInDarkMode: Boolean) {
        val action = if (viewInDarkMode) APPLY_DARK_STYLES_METRICS_ACTION else REMOVE_DARK_STYLES_METRICS_ACTION
        val metrics = Metrics(
            METRICS_LOG_TAG,
            DARK_STYLES_METRICS_TITLE,
            JsonObject(mapOf(METRICS_ACTION to JsonPrimitive(action)))
        )
        metricsManager.send(userId, metrics)
    }
}
