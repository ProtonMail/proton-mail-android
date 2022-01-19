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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import ch.protonmail.android.pinlock.domain.usecase.ShouldShowPinLockScreen
import ch.protonmail.android.settings.pin.ValidatePinActivity
import ch.protonmail.android.usecase.GetElapsedRealTimeMillis
import ch.protonmail.android.utils.EmptyActivityLifecycleCallbacks
import ch.protonmail.android.utils.extensions.app
import kotlinx.coroutines.runBlocking
import me.proton.core.presentation.app.AppLifecycleProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinLockManager @Inject constructor(
    private val context: Context,
    private val getElapsedRealTimeMillis: GetElapsedRealTimeMillis,
    private val shouldShowPinLockScreen: ShouldShowPinLockScreen
) : DefaultLifecycleObserver {

    private var appState: AppLifecycleProvider.State = AppLifecycleProvider.State.Background
    private var lastForegroundTime: Long = 0

    init {
        context.app.registerActivityLifecycleCallbacks(object : EmptyActivityLifecycleCallbacks {

            override fun onActivityStarted(activity: Activity) {
                super.onActivityStarted(activity)
                Timber.v("Activity started")

                @Suppress("BlockingMethodInNonBlockingContext") // This needs to be run blocking, in order to
                //  prevent the last activity to be displayed
                runBlocking {
                    val shouldLock = shouldShowPinLockScreen(
                        wasAppInBackground = appState == AppLifecycleProvider.State.Background,
                        isPinLockScreenShown = activity is ValidatePinActivity,
                        lastForegroundTime = lastForegroundTime
                    )
                    if (shouldLock) {
                        launchPinLockActivity(activity)
                    }
                }
            }
        })
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.v("App Foreground")

        appState = AppLifecycleProvider.State.Foreground
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Timber.v("App Background")

        appState = AppLifecycleProvider.State.Background
        lastForegroundTime = getElapsedRealTimeMillis()
    }

    private fun launchPinLockActivity(callingActivity: Activity) {
        val intent = Intent(context, ValidatePinActivity::class.java)
        callingActivity.startActivity(intent)
    }
}
