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

import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject

class RenderDimensionsProvider @Inject constructor() {

    fun getRenderWidth(activity: FragmentActivity): Int {
        val metrics = DisplayMetrics()
        val windowManager = checkNotNull(activity.getSystemService<WindowManager>()) {
            "Impossible to get WindowManager for current Activity"
        }
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        return (metrics.widthPixels / metrics.density).toInt()
    }
}
