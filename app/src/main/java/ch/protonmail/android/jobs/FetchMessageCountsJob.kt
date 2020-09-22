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
import ch.protonmail.android.utils.Logger
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint

// region constants
private const val COUNTS_SINGLE_INSTANCE = "instanceIdCounts"
private const val TAG_JOB_FETCH_UNREAD = "FetchUnReadJob"
// endregion

class FetchMessageCountsJob(username: String?) : ProtonMailBaseJob(Params(Priority.MEDIUM).singleInstanceBy(COUNTS_SINGLE_INSTANCE).groupBy(Constants.JOB_GROUP_MISC), username) {

    @Throws(Throwable::class)
    override fun onRun() {
        if (!getQueueNetworkUtil().isConnected()) {
            Logger.doLog(TAG_JOB_FETCH_UNREAD, "no network - cannot fetch unread")
            AppUtil.postEventOnUi(MessageCountsEvent(Status.FAILED))
            return
        }

        try {
            val totalResponse = getApi().fetchMessagesCount(RetrofitTag(username?: getUserManager().username))
            val messageCounts = totalResponse.counts?: emptyList()
            val locationCounters = messageCounts.filter { it.labelId.length <= 2 }.map {
                val location = Constants.MessageLocationType.fromInt(Integer.valueOf(it.labelId))
                UnreadLocationCounter(location.messageLocationTypeValue, it.unread)
            }
            val unreadCountersDatabase = CountersDatabaseFactory.getInstance(ProtonMailApplication.getApplication(), username).getDatabase()
            val labelCounters = messageCounts.filter { it.labelId.length > 2 }.map { UnreadLabelCounter(it.labelId, it.unread) }
            unreadCountersDatabase.updateUnreadCounters(locationCounters, labelCounters)


            AppUtil.postEventOnUi(MessageCountsEvent(Status.SUCCESS, totalResponse))
        } catch (e: Exception) {
            AppUtil.postEventOnUi(MessageCountsEvent(Status.FAILED))
            Logger.doLogException(e)
        }

    }

    override fun getRetryLimit(): Int {
        return 1
    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint? {
        return RetryConstraint.CANCEL
    }
}
