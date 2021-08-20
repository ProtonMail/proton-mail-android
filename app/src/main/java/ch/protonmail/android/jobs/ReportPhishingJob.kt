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
package ch.protonmail.android.jobs

import ch.protonmail.android.events.PostPhishingReportEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import me.proton.core.network.domain.ApiResult

class ReportPhishingJob(
    private val messageId: String,
    private val body: String,
    private val mimeType: String
) : ProtonMailEndlessJob(Params(Priority.MEDIUM).requireNetwork().persist()) {

    override fun onAdded() {
        super.onAdded()
        if (!getQueueNetworkUtil().isConnected()) {
            AppUtil.postEventOnUi(PostPhishingReportEvent(Status.NO_NETWORK))
        }
    }

    @Throws(Throwable::class)
    override fun onRun() {
        runBlocking {
            val response = getApi().postPhishingReport(messageId, body, mimeType, requireNotNull(userId))
            if (response is ApiResult.Success) {
                AppUtil.postEventOnUi(PostPhishingReportEvent(Status.SUCCESS))
            } else {
                AppUtil.postEventOnUi(PostPhishingReportEvent(Status.FAILED))
            }
        }
    }


}
