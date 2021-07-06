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
package ch.protonmail.android.api.segments.report

import ch.protonmail.android.api.models.BugsBody
import ch.protonmail.android.api.models.PostPhishingReportBody
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import ch.protonmail.android.core.Constants
import java.io.IOException

class ReportApi(private val service: ReportService) : BaseApi(), ReportApiSpec {

    @Throws(IOException::class)
    override fun reportBug(
        osName: String,
        appVersion: String,
        client: String,
        clientVersion: String,
        title: String,
        description: String,
        username: String,
        email: String
    ): ResponseBody {
        return ParseUtils.parse(
            service.bugs(BugsBody(osName, appVersion, client, clientVersion, title, description, username, email))
                .execute()
        )
    }

    @Throws(IOException::class)
    override fun postPhishingReport(messageId: String, messageBody: String, mimeType: String): ResponseBody? {
        // Accept only 'text/plain' / 'text/html'
        val correctedMimeType =
            if (Constants.MIME_TYPE_PLAIN_TEXT == mimeType) Constants.MIME_TYPE_PLAIN_TEXT else Constants.MIME_TYPE_HTML
        return ParseUtils.parse(
            service.postPhishingReport(
                PostPhishingReportBody(
                    messageId, messageBody,
                    correctedMimeType
                )
            ).execute()
        )
    }
}
