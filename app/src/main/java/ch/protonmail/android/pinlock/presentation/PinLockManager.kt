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

package ch.protonmail.android.pinlock.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import ch.protonmail.android.activities.AddAttachmentsActivity
import ch.protonmail.android.pinlock.domain.usecase.ShouldShowPinLockScreen
import ch.protonmail.android.settings.pin.ChangePinActivity
import ch.protonmail.android.settings.pin.CreatePinActivity
import ch.protonmail.android.settings.pin.ValidatePinActivity
import me.proton.core.presentation.app.AppLifecycleProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinLockManager @Inject constructor(
    private val context: Context,
    private val shouldShowPinLockScreen: ShouldShowPinLockScreen
) {

    private var isLocked = false

    suspend fun shouldLock(
        appState: AppLifecycleProvider.State,
        currentActivity: Activity,
        lastForegroundTime: Long
    ): Boolean = shouldShowPinLockScreen(
        wasAppInBackground = appState == AppLifecycleProvider.State.Background,
        isPinLockScreenShown = isPinScreenActivity(currentActivity),
        isPinLockScreenOpen = isLocked,
        isAddingAttachments = currentActivity is AddAttachmentsActivity,
        lastForegroundTime = lastForegroundTime
    )

    fun lock(activity: Activity) {
        isLocked = true
        launchPinLockActivity(activity)
    }

    fun unlock() {
        isLocked = false
    }

    private fun launchPinLockActivity(callingActivity: Activity) {
        val intent = Intent(context, ValidatePinActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_HISTORY)
        callingActivity.startActivity(intent)
    }

    private fun isPinScreenActivity(activity: Activity): Boolean =
        when (activity) {
            is CreatePinActivity, is ChangePinActivity, is ValidatePinActivity -> true
            else -> false
        }
}
