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
package ch.protonmail.android.api.segments.event

import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.feature.account.allLoggedInBlocking
import ch.protonmail.android.jobs.Priority
import ch.protonmail.android.jobs.ProtonMailBaseJob
import com.birbit.android.jobqueue.Params
import timber.log.Timber
import java.net.ConnectException
import java.util.concurrent.TimeUnit

class FetchUpdatesJob internal constructor(private val eventManager: EventManager) : ProtonMailBaseJob(
    Params(Priority.HIGH).requireNetwork()
) {

    constructor() : this(ProtonMailApplication.getApplication().eventManager)

    @Throws(Throwable::class)
    override fun onRun() {
        val messageDao = MessageDatabase.getInstance(applicationContext, userId!!).getDao()
        if (!getQueueNetworkUtil().isConnected()) {
            Timber.i("no network cannot fetch updates")
            return
        }

        // check for expired messages in the cache and delete them
        val currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        messageDao.deleteExpiredMessages(currentTime)
        try {
            val loggedInUsers = getAccountManager().allLoggedInBlocking()
            eventManager.consumeEventsForBlocking(loggedInUsers)
        } catch (e: Exception) {
            Timber.e(e, "FetchUpdatesJob has failed")
            if (e is ConnectException) {
                getQueueNetworkUtil().retryPingAsPreviousRequestWasInconclusive()
            }
        }
    }

    override fun onProtonCancel(cancelReason: Int, throwable: Throwable?) {}
}
