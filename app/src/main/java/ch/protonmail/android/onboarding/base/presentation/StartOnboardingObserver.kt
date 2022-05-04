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

package ch.protonmail.android.onboarding.base.presentation

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import ch.protonmail.android.core.Constants.Prefs.PREF_EXISTING_USER_ONBOARDING_SHOWN
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.onboarding.existinguser.presentation.ExistingUserOnboardingActivity
import ch.protonmail.android.utils.EmptyActivityLifecycleCallbacks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartOnboardingObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    @DefaultSharedPreferences private val defaultSharedPreferences: SharedPreferences,
    private val externalScope: CoroutineScope,
    private val dispatchers: DispatcherProvider
) {

    private val app: Application
        get() = context as Application

    private val activityLifecycleObserver = object : EmptyActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            super.onActivityCreated(activity, savedInstanceState)

            activity.startActivity(Intent(context, ExistingUserOnboardingActivity::class.java))
            externalScope.launch(dispatchers.Io) {
                defaultSharedPreferences.edit()
                    .putBoolean(PREF_EXISTING_USER_ONBOARDING_SHOWN, true)
                    .apply()
            }
            app.unregisterActivityLifecycleCallbacks(this)
        }
    }

    init {
        app.registerActivityLifecycleCallbacks(activityLifecycleObserver)
    }
}
