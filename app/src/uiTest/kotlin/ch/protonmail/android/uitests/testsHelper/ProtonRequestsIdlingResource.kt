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
@file:Suppress("DEPRECATION")

package ch.protonmail.android.uitests.testsHelper

import androidx.test.espresso.IdlingResource
import ch.protonmail.android.api.interceptors.ProtonMailRequestInterceptor

/**
 * Espresso [IdlingResource] for network requests.
 * Tests will be paused when at least one request is ongoing.
 */
class ProtonRequestsIdlingResource : IdlingResource {

    private val resourceName = "InterceptorIdlingResource"
    private lateinit var callback: IdlingResource.ResourceCallback

    override fun getName() = resourceName

    override fun isIdleNow(): Boolean {

        return if (ProtonMailRequestInterceptor.requestCount == 0) {
            callback.onTransitionToIdle()
            true
        } else {
            false
        }
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }
}