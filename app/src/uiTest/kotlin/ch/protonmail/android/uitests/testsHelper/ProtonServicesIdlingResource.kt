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
@file:Suppress("DEPRECATION")

package ch.protonmail.android.uitests.testsHelper

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.test.espresso.IdlingResource
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.api.services.LoginService
import ch.protonmail.android.api.services.LogoutService
import ch.protonmail.android.api.services.MessagesService
import ch.protonmail.android.gcm.PMRegistrationIntentService

/**
 * Espresso [IdlingResource] for some app services.
 * Tests will be paused when at least one service is running.
 */
class ProtonServicesIdlingResource : IdlingResource {

    private val resourceName = "ProtonServicesIdlingResource"
    private lateinit var callback: IdlingResource.ResourceCallback

    override fun getName() = resourceName

    override fun isIdleNow(): Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        for (info in manager.getRunningServices(Int.MAX_VALUE)) {
            Log.d(resourceName, "Running service: ${info.service.className}")
            when (info.service.className) {
                LoginService::class.java.name -> {
                    return if (LoginService().isStopped) {
                        callback.onTransitionToIdle()
                        true
                    } else false
                }
                LogoutService::class.java.name -> {
                    return if (LogoutService().isStopped) {
                        callback.onTransitionToIdle()
                        true
                    } else false
                }
                MessagesService::class.java.name -> {
                    return if (MessagesService().isStopped) {
                        callback.onTransitionToIdle()
                        true
                    } else false
                }
                PMRegistrationIntentService::class.java.name -> {
                    return if (PMRegistrationIntentService().isStopped) {
                        callback.onTransitionToIdle()
                        true
                    } else false
                }
                else -> return false
            }
        }
        callback.onTransitionToIdle()
        return true
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }
}