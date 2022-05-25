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
package ch.protonmail.android.api.segments.report

import ch.protonmail.android.api.models.PostPhishingReportBody
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult

class ReportApi(private val apiProvider: ApiProvider) : ReportApiSpec {

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
