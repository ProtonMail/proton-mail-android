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

import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.notifier.UserNotifier
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import me.proton.core.network.domain.ApiResult

class ReportBugsJob(
    private val osName: String,
    private val appVersion: String,
    private val client: String,
    private val clientVersion: String,
    private val title: String,
    private val description: String,
    private val userName: String,
    private val email: String,
    private val userNotifier: UserNotifier
) : ProtonMailEndlessJob(Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_BUGS)) {

    @Throws(Throwable::class)
    override fun onRun() {
        runBlocking {
            val response = getApi().reportBug(
                osName,
                appVersion,
                client,
                clientVersion,
                title,
                description,
                userName,
                email
            )
            if (response is ApiResult.Success) {
                userNotifier.showError(R.string.received_report)
            } else {
                userNotifier.showError(R.string.not_received_report)
            }
        }
    }
}
