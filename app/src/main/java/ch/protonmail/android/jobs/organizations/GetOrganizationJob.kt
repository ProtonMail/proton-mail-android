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
package ch.protonmail.android.jobs.organizations

import ch.protonmail.android.core.Constants
import ch.protonmail.android.events.Status
import ch.protonmail.android.events.organizations.OrganizationEvent
import ch.protonmail.android.jobs.Priority
import ch.protonmail.android.jobs.ProtonMailBaseJob
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import me.proton.core.network.domain.ApiResult
import timber.log.Timber

class GetOrganizationJob : ProtonMailBaseJob(
    Params(Priority.HIGH).requireNetwork().groupBy(Constants.JOB_GROUP_PAYMENT)
) {
    @Throws(Throwable::class)
    override fun onRun() {
        runBlocking {
            val response = getApi().fetchOrganization(requireNotNull(userId))
            val keysResponse = if (response is ApiResult.Success) {
                Timber.v("fetching Organization Keys")
                getApi().fetchOrganizationKeys()
            } else {
                Timber.i("Get Organization failure: $response")
                null
            }
            if (keysResponse is ApiResult.Success) {
                AppUtil.postEventOnUi(OrganizationEvent(Status.SUCCESS, response))
            } else {
                Timber.i("Get Organization keys failure: $response")
                AppUtil.postEventOnUi(OrganizationEvent(Status.FAILED, response))
            }
        }
    }
}
