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

import ch.protonmail.android.api.models.MailSettingsResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.domain.entity.Id
import java.io.IOException

interface MailSettingsApiSpec {

    @Throws(IOException::class)
    fun fetchMailSettingsBlocking(): MailSettingsResponse

    suspend fun fetchMailSettings(): MailSettingsResponse

    @Throws(IOException::class)
    fun fetchMailSettingsBlocking(userId: Id): MailSettingsResponse

    @Throws(IOException::class)
    fun updateSignature(signature: String): ResponseBody?

    @Throws(IOException::class)
    fun updateDisplayName(displayName: String): ResponseBody?

    @Throws(IOException::class)
    fun updateLeftSwipe(swipeSelection: Int): ResponseBody?

    @Throws(IOException::class)
    fun updateRightSwipe(swipeSelection: Int): ResponseBody?

    @Throws(IOException::class)
    fun updateAutoShowImages(autoShowImages: Int): ResponseBody?
}
