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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.ArrayList

class PostUnstarJob(private val messageIds: List<String>) : ProtonMailEndlessJob(
    Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL)
) {

    override fun onAdded() = messageIds.forEach { messageId ->
        val counterDao = CounterDatabase
            .getInstance(applicationContext, userId!!)
            .getDao()

        unstarLocalMessage(messageId)

        var messageLocation = MessageLocationType.INVALID
        var isUnread = false
        val message = getMessageDetailsRepository().findMessageByIdBlocking(
            messageId
        )
        if (message != null) {
            messageLocation = fromInt(message.location)
            isUnread = !message.isRead
        }
        if (messageLocation !== MessageLocationType.INVALID && isUnread) {
            val unreadLocationCounter =
                counterDao.findUnreadLocationById(messageLocation.messageLocationTypeValue) ?: return
            unreadLocationCounter.increment(messageIds.size)
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

    @Throws(Throwable::class)
    override fun onRun() {
        val messageIds: List<String> = ArrayList(messageIds)
        getApi().unlabelMessages(IDList(MessageLocationType.STARRED.messageLocationTypeValue.toString(), messageIds))
    }

    private fun unstarLocalMessage(messageId: String) = runBlocking {
        val message = getMessageDetailsRepository().findMessageById(messageId).first() ?: return@runBlocking
        message.removeLabels(listOf(MessageLocationType.STARRED.messageLocationTypeValue.toString()))
        message.isStarred = false
        getMessageDetailsRepository().saveMessage(message)
    }

}
