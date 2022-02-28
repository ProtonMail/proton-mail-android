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
     * @param wasAppInBackground `true` if the app was in background before calling this function
     * @param isPinLockScreenShown `true` if a Pin screen is shown
     * @param isPinLockScreenOpen `true` if PinLock is open in the background, but not currently shown. This means that
     *  another Activity has been launcher at the top of it, without the user has correctly unlocked it
     * @param isAddingAttachments `true` if [AddAttachmentsActivity] is shown. In that case, we may not show the Pin
     *  Lock screen, because it can be triggered after closing the File Picker
     * @param lastForegroundTime time in milliseconds when the app has last been in foreground
     */
    suspend operator fun invoke(
        wasAppInBackground: Boolean,
        isPinLockScreenShown: Boolean,
        isPinLockScreenOpen: Boolean,
        isAddingAttachments: Boolean,
        lastForegroundTime: Long
    ): Boolean {
        if (isPinLockScreenOpen && isPinLockScreenShown.not()) {
            return true
        }

        if (wasAppInBackground.not() || isPinLockScreenShown || isPinLockEnabled().not()) {
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
