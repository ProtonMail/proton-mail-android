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

import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.Constants.MessageLocationType.Companion.fromInt
import ch.protonmail.android.data.local.CounterDatabase
import ch.protonmail.android.data.local.model.UnreadLocationCounter
import com.birbit.android.jobqueue.Params
import java.util.ArrayList

class PostUnstarJob(private val mMessageIds: List<String>) : ProtonMailEndlessJob(
    Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL)
) {

    override fun onAdded() {
        val counterDao = CounterDatabase
            .getInstance(applicationContext, userId!!)
            .getDao()

        for (id in mMessageIds) {
            getMessageDetailsRepository().updateStarred(id, false)
            var messageLocation = MessageLocationType.INVALID
            var isUnread = false
            val message = getMessageDetailsRepository().findMessageByIdBlocking(
                id
            )
            if (message != null) {
                messageLocation = fromInt(message.location)
                isUnread = !message.isRead
            }
            if (messageLocation !== MessageLocationType.INVALID && isUnread) {
                val unreadLocationCounter =
                    counterDao.findUnreadLocationById(messageLocation.messageLocationTypeValue) ?: return
                unreadLocationCounter.increment(mMessageIds.size)
                val countersToUpdate: MutableList<UnreadLocationCounter> = ArrayList()
                countersToUpdate.add(unreadLocationCounter)
                val starredUnread =
                    counterDao.findUnreadLocationById(MessageLocationType.STARRED.messageLocationTypeValue)
                if (starredUnread != null) {
                    starredUnread.increment()
                    countersToUpdate.add(starredUnread)
                }
                counterDao.insertAllUnreadLocations(countersToUpdate)
            }
        }
    }

    @Throws(Throwable::class)
    override fun onRun() {
        val messageIds: List<String> = ArrayList(mMessageIds)
        getApi().unlabelMessages(IDList(MessageLocationType.STARRED.messageLocationTypeValue.toString(), messageIds))
    }
}
