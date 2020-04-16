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
package ch.protonmail.android.api.segments.settings.mail

import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.SrpResponseBody
import ch.protonmail.android.api.models.UserSettingsResponse
import ch.protonmail.android.api.models.requests.NotificationEmail
import ch.protonmail.android.api.models.requests.PasswordChange
import ch.protonmail.android.api.models.requests.UpdateNotify
import ch.protonmail.android.api.models.requests.UpgradePasswordBody
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.segments.settings.user.UserSettingsService
import ch.protonmail.android.api.utils.ParseUtils
import java.io.IOException


class UserSettingsApi(private val service: UserSettingsService) : BaseApi(), UserSettingsApiSpec {

    @Throws(IOException::class)
    override fun fetchUserSettings(): UserSettingsResponse =
            ParseUtils.parse(service.fetchUserSettings().execute())

    @Throws(IOException::class)
    override fun fetchUserSettings(username:String): UserSettingsResponse =
            ParseUtils.parse(service.fetchUserSettings(RetrofitTag(username)).execute())

    @Throws(IOException::class)
    override fun updateNotify(updateNotify: Boolean): ResponseBody? =
            ParseUtils.parse(service.updateNotify(UpdateNotify(updateNotify)).execute())

    @Throws(IOException::class)
    override fun updateNotificationEmail(srpSession: String, clientEpheremal: String, clientProof: String, twoFactorCode: String?, email: String): SrpResponseBody? =
            ParseUtils.parse(service.updateNotificationEmail(NotificationEmail(srpSession, clientEpheremal, clientProof, twoFactorCode, email)).execute())

    @Throws(IOException::class)
    override fun updateLoginPassword(passwordChangeBody: PasswordChange): SrpResponseBody? =
            ParseUtils.parse(service.updateLoginPassword(passwordChangeBody).execute())

    @Throws(IOException::class)
    override fun upgradeLoginPassword(upgradePasswordBody: UpgradePasswordBody): ResponseBody? =
            ParseUtils.parse(service.upgradeLoginPassword(upgradePasswordBody).execute())
}
