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

package ch.protonmail.android.api

import android.content.Context
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.events.ForceUpgradeEvent
import ch.protonmail.android.utils.AppUtil
import me.proton.core.network.domain.ApiClient
import javax.inject.Inject

class ProtonMailApiClient @Inject constructor(
    context: Context
) : ApiClient {

    override val appVersionHeader: String = "Android_" + AppUtil.getAppVersionName(context)
    override val enableDebugLogging: Boolean = BuildConfig.DEBUG
    override val shouldUseDoh: Boolean = true
    override val userAgent: String = AppUtil.buildUserAgent()

    /**
     * Tells client to force update (this client will no longer be accepted by the API).
     *
     * @param errorMessage the localized error message the user should see.
     */
    override fun forceUpdate(errorMessage: String) {
        AppUtil.postEventOnUi(ForceUpgradeEvent(errorMessage))
    }
}
