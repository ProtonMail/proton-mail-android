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

import ch.protonmail.android.api.utils.ProtonHeaders
import ch.protonmail.android.core.Constants.Prefs.PREF_ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES
import ch.protonmail.android.events.ForceUpgradeEvent
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.BuildInfo
import me.proton.core.network.domain.ApiClient
import javax.inject.Inject

class ProtonMailApiClient @Inject constructor(
    buildInfo: BuildInfo,
    private val secureSharedPreferencesFactory: SecureSharedPreferences.Factory
) : ApiClient {

    override val appVersionHeader: String = ProtonHeaders.appVersion
    override val enableDebugLogging: Boolean = buildInfo.isDebugVersion
    override val userAgent = "ProtonMail/${buildInfo.versionName} (Android ${buildInfo.releaseVersion};" +
        " ${buildInfo.brand} ${buildInfo.model})"
    override val shouldUseDoh: Boolean
        get() = secureSharedPreferencesFactory.appPreferences()
            .getBoolean(PREF_ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES, true)

    /**
     * Tells client to force update (this client will no longer be accepted by the API).
     *
     * @param errorMessage the localized error message the user should see.
     */
    override fun forceUpdate(errorMessage: String) {
        AppUtil.postEventOnUi(ForceUpgradeEvent(errorMessage))
    }
}
