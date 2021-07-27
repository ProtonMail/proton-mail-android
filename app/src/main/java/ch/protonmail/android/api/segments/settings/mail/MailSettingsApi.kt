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

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.MailSettingsResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.requests.DisplayName
import ch.protonmail.android.api.models.requests.ShowImages
import ch.protonmail.android.api.models.requests.Signature
import ch.protonmail.android.api.models.requests.SwipeLeft
import ch.protonmail.android.api.models.requests.SwipeRight
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import ch.protonmail.android.domain.entity.Id
import java.io.IOException

class MailSettingsApi(private val service: MailSettingsService) : BaseApi(), MailSettingsApiSpec {

    override suspend fun fetchMailSettings(userId: Id): MailSettingsResponse =
        service.fetchMailSettings(UserIdTag(userId))

    @Throws(IOException::class)
    override fun fetchMailSettingsBlocking(userId: Id): MailSettingsResponse =
        ParseUtils.parse(service.fetchMailSettingsCall(UserIdTag(userId)).execute())

    @Throws(IOException::class)
    override fun updateSignature(signature: String): ResponseBody? =
        ParseUtils.parse(service.updateSignature(Signature(signature)).execute())

    @Throws(IOException::class)
    override fun updateDisplayName(displayName: String): ResponseBody? =
        ParseUtils.parse(service.updateDisplay(DisplayName(displayName)).execute())

    @Throws(IOException::class)
    override fun updateLeftSwipe(swipeSelection: Int): ResponseBody? =
        ParseUtils.parse(service.updateLeftSwipe(SwipeLeft(swipeSelection)).execute())

    @Throws(IOException::class)
    override fun updateRightSwipe(swipeSelection: Int): ResponseBody? =
        ParseUtils.parse(service.updateRightSwipe(SwipeRight(swipeSelection)).execute())

    @Throws(IOException::class)
    override fun updateAutoShowImages(autoShowImages: Int): ResponseBody? =
        ParseUtils.parse(service.updateAutoShowImages(ShowImages(autoShowImages)).execute())
}
