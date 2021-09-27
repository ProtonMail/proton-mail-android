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

import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.room.counters.CountersDatabaseFactory
import ch.protonmail.android.api.models.room.counters.UnreadLabelCounter
import ch.protonmail.android.api.models.room.counters.UnreadLocationCounter
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.MessageCountsEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import timber.log.Timber

private const val FETCH_COUNTS_ID = "instanceIdCounts"

class FetchMessageCountsJob(
    username: String?
) : ProtonMailBaseJob(
    Params(Priority.MEDIUM).singleInstanceBy(FETCH_COUNTS_ID).groupBy(Constants.JOB_GROUP_MISC),
    username
) {

    @Throws(Throwable::class)
    override fun onRun() {
        if (getQueueNetworkUtil().isConnected().not()) {
            Timber.w("no network - cannot fetch unread")
            AppUtil.postEventOnUi(MessageCountsEvent(Status.FAILED))
            return
        }

        try {
            val countersResponse = getApi().fetchMessagesCount(RetrofitTag(username))

            val counters = countersResponse.counts ?: emptyList()
            val (labelCounters, locationCounters) = counters.partition { it.labelId.length > 2 }

            val unreadLabelCounters = labelCounters.map { UnreadLabelCounter(it.labelId, it.unread) }
            val unreadLocationCounters = locationCounters.map {
                val location = Constants.MessageLocationType.fromInt(Integer.valueOf(it.labelId))
                UnreadLocationCounter(location.messageLocationTypeValue, it.unread)
            }

            val unreadCountersDatabase =
                CountersDatabaseFactory.getInstance(ProtonMailApplication.getApplication(), username).getDatabase()
            unreadCountersDatabase.updateUnreadCounters(unreadLocationCounters, unreadLabelCounters)

            AppUtil.postEventOnUi(MessageCountsEvent(Status.SUCCESS, countersResponse))
        } catch (e: Exception) {
            AppUtil.postEventOnUi(MessageCountsEvent(Status.FAILED))
            Timber.e(e)
        }

    }

    override fun getRetryLimit() = 1

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint? =
        RetryConstraint.CANCEL
}
