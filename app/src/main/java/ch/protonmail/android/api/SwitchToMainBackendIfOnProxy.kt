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

package ch.protonmail.android.api

import android.content.SharedPreferences
import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.di.DefaultSharedPreferences
import timber.log.Timber
import javax.inject.Inject

class SwitchToMainBackendIfOnProxy @Inject constructor(
    private val userManager: UserManager,
    private val switchToMainBackendIfAvailable: SwitchToMainBackendIfAvailable,
    @DefaultSharedPreferences private val sharedPreferences: SharedPreferences
) {

    suspend operator fun invoke(): Result {
        val currentLegacyUser = userManager.currentLegacyUser
        return if (currentLegacyUser == null || currentLegacyUser.usingDefaultApi) {
            AlreadyUsingMainBackend
        } else {
            val proxyBeforeSwitch = Proxies.getInstance(proxyList = null, sharedPreferences)
                .getCurrentActiveProxy()
            if (switchToMainBackendIfAvailable()) {
                Timber.w(
                    """
                    Switched away from proxy ${proxyBeforeSwitch.baseUrl}
                    after ${System.currentTimeMillis() - proxyBeforeSwitch.lastTrialTimestamp} ms
                    """.trimIndent()
                )
                SwitchSuccess
            } else {
                SwitchFailure
            }
        }
    }

    sealed class Result
    object AlreadyUsingMainBackend : Result()
    object SwitchSuccess : Result()
    object SwitchFailure : Result()
}
