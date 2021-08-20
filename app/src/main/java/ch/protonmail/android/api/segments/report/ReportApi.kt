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
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult

class ReportApi(private val apiProvider: ApiProvider) : ReportApiSpec {

    override suspend fun reportBug(
        osName: String,
        appVersion: String,
        client: String,
        clientVersion: String,
        title: String,
        description: String,
        username: String,
        email: String
    ): ApiResult<Unit> =
        apiProvider.get<ReportService>().invoke {
            postBugs(BugsBody(osName, appVersion, client, clientVersion, title, description, username, email))
        }

    override suspend fun postPhishingReport(
        messageId: String,
        messageBody: String,
        mimeType: String,
        userId: UserId
    ): ApiResult<Unit> =
        apiProvider.get<ReportService>(userId).invoke {
            postPhishingReport(
                PostPhishingReportBody(
                    messageId,
                    messageBody,
                    mimeType
                )
            )
        }
}
