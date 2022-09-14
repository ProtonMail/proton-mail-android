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

import ch.protonmail.android.core.UserManager
import dagger.Lazy
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private val PING_TIMEOUT = 20.seconds

class SwitchToMainBackendIfAvailable @Inject constructor(
    private val userManager: UserManager,
    apiManagerLazy: Lazy<ProtonMailApiManager>,
    networkSwitcherLazy: Lazy<NetworkSwitcher>
) {

    private val networkSwitcher: NetworkSwitcher by lazy { networkSwitcherLazy.get() }
    private val apiManager: ProtonMailApiManager by lazy { apiManagerLazy.get() }

    suspend operator fun invoke(): Boolean = runCatching {
        withTimeout(PING_TIMEOUT) {
            apiManager.pingMainBackend()
        }
    }.fold(
        onSuccess = {
            networkSwitcher.forceSwitchToMainBackend()
            userManager.requireCurrentLegacyUser().usingDefaultApi = true
            true
        },
        onFailure = { false }
    )
}
