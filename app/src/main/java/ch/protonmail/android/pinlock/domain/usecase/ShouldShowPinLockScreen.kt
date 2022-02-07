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

import ch.protonmail.android.activities.AddAttachmentsActivity
import ch.protonmail.android.usecase.GetElapsedRealTimeMillis
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.toDuration

class ShouldShowPinLockScreen @Inject constructor(
    private val isPinLockEnabled: IsPinLockEnabled,
    private val getPinLockTimer: GetPinLockTimer,
    private val getElapsedRealTimeMillis: GetElapsedRealTimeMillis
) {

    /**
     * @param wasAppInBackground lambda that returns whether the app was in background before calling this function
     * @param isPinLockScreenShown lambda that returns whether a Pin screen is shown
     * @param isAddingAttachments whether [AddAttachmentsActivity] is shown. In that case, we may not show the Pin Lock
     *  screen, because it can be triggered after closing the File Picker
     * @param lastForegroundTime time in milliseconds when the app has last been in foreground
     */
    suspend operator fun invoke(
        wasAppInBackground: Boolean,
        isPinLockScreenShown: Boolean,
        isAddingAttachments: Boolean,
        lastForegroundTime: Long
    ): Boolean {
        if (wasAppInBackground.not() || isPinLockScreenShown) {
            return false
        }

        val isPinLockEnabled = isPinLockEnabled()
        if (isPinLockEnabled.not()) {
            return false
        }

        val pinLockTimer = getPinLockTimer().duration
        return shouldShowIfAddingAttachments(isAddingAttachments, pinLockTimer) &&
            getTimeDiff(lastForegroundTime) > pinLockTimer
    }

    private fun shouldShowIfAddingAttachments(isAddingAttachments: Boolean, pinLockTimer: Duration) =
        isAddingAttachments.not() || pinLockTimer != Duration.ZERO

    private fun getTimeDiff(lastForegroundTime: Long): Duration =
        (getElapsedRealTimeMillis() - lastForegroundTime).toDuration(MILLISECONDS)
}
