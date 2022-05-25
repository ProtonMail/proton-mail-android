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
package ch.protonmail.android.api.segments.settings.mail

import ch.protonmail.android.api.models.MailSettingsResponse
import ch.protonmail.android.api.models.ResponseBody
import me.proton.core.domain.entity.UserId
import java.io.IOException

interface MailSettingsApiSpec {

    suspend fun fetchMailSettings(userId: UserId): MailSettingsResponse

    @Throws(IOException::class)
    fun fetchMailSettingsBlocking(userId: UserId): MailSettingsResponse

    @Throws(IOException::class)
    fun updateSignature(signature: String): ResponseBody?

    @Throws(IOException::class)
    fun updateDisplayName(displayName: String): ResponseBody?

    @Throws(IOException::class)
    fun updateAutoShowImages(autoShowImages: Int): ResponseBody?
}
