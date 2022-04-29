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
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import ch.protonmail.android.core.Constants.Prefs.PREF_EXISTING_USER_ONBOARDING_SHOWN
import ch.protonmail.android.databinding.LayoutOnboardingToNewBrandItemBinding
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.utils.EmptyActivityLifecycleCallbacks
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

            showOnboardingDialog(activity)
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

    private fun showOnboardingDialog(context: Context) {
        val binding = LayoutOnboardingToNewBrandItemBinding.inflate(LayoutInflater.from(context))
        var dialog: AlertDialog? = null
        binding.onboardingDescriptionTextView.movementMethod = LinkMovementMethod.getInstance()
        binding.button.setOnClickListener { dialog?.dismiss() }

        dialog = MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setView(binding.root)
            .show()
    }
}
