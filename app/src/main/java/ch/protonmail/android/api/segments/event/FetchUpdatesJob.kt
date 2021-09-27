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

import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.FetchUpdatesEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.Priority
import ch.protonmail.android.jobs.ProtonMailBaseJob
import ch.protonmail.android.utils.AppUtil
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
        val messageDao = MessagesDatabaseFactory.getInstance(applicationContext).getDatabase()
        if (!getQueueNetworkUtil().isConnected()) {
            Timber.i("no network cannot fetch updates")
            return
        }

        // check for expired messages in the cache and delete them
        val currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        messageDao.deleteExpiredMessages(currentTime)
        try {
            val loggedInUsers = AccountManager.getInstance(ProtonMailApplication.getApplication()).getLoggedInUsers()
            eventManager.start(loggedInUsers)
            AppUtil.postEventOnUi(FetchUpdatesEvent(Status.SUCCESS))
        } catch (e: Exception) {
            Timber.e(e, "FetchUpdatesJob has failed")
            if (e is ConnectException) {
                getQueueNetworkUtil().retryPingAsPreviousRequestWasInconclusive()
            }
        }
    }

    override fun onProtonCancel(cancelReason: Int, throwable: Throwable?) {}
}
