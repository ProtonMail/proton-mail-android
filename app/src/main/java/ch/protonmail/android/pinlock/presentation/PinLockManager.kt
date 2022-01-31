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

class PinLockManager @Inject constructor(
    private val context: Context,
    private val shouldShowPinLockScreen: ShouldShowPinLockScreen
) {

    suspend fun shouldLock(
        appState: AppLifecycleProvider.State,
        currentActivity: Activity,
        lastForegroundTime: Long
    ): Boolean = shouldShowPinLockScreen(
        wasAppInBackground = appState == AppLifecycleProvider.State.Background,
        isPinLockScreenShown = isPinScreenActivity(currentActivity),
        isAddingAttachments = currentActivity is AddAttachmentsActivity,
        lastForegroundTime = lastForegroundTime
    )

    fun lock(activity: Activity) {
        launchPinLockActivity(activity)
    }

    private fun launchPinLockActivity(callingActivity: Activity) {
        val intent = Intent(context, ValidatePinActivity::class.java)
        callingActivity.startActivity(intent)
    }

    private fun isPinScreenActivity(activity: Activity) =
        activity is ValidatePinActivity ||
            activity is CreatePinActivity ||
            activity is ChangePinActivity
}
