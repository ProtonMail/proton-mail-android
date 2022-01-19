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

package ch.protonmail.android.pinlock.domain.usecase

import ch.protonmail.android.usecase.GetElapsedRealTimeMillis
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.toDuration

class ShouldShowPinLockScreen @Inject constructor(
    private val isPinLockEnabled: IsPinLockEnabled,
    private val getPinLockTimer: GetPinLockTimer,
    private val getElapsedRealTimeMillis: GetElapsedRealTimeMillis
) {

    /**
     * @param wasAppInBackground lambda that returns whether the app was in background before calling this function
     * @param isPinLockScreenShown lambda that returns whether the Pin Lock screen is shown
     * @param lastForegroundTime time in milliseconds when the app has last been in foreground
     */
    suspend operator fun invoke(
        wasAppInBackground: Boolean,
        isPinLockScreenShown: Boolean,
        lastForegroundTime: Long
    ): Boolean =
        wasAppInBackground && isPinLockScreenShown.not() && isPinLockEnabled() &&
            getTimeDiff(lastForegroundTime) > getPinLockTimer().duration

    private fun getTimeDiff(lastForegroundTime: Long): Duration =
        (getElapsedRealTimeMillis() - lastForegroundTime).toDuration(MILLISECONDS)
}
