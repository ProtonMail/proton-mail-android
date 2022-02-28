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
package ch.protonmail.android.jobs

import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.Constants.MessageLocationType.Companion.fromInt
import ch.protonmail.android.data.local.CounterDatabase
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.UnreadLocationCounter
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.ArrayList

class PostUnstarJob(private val messageIds: List<String>) : ProtonMailEndlessJob(
    Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL)
) {

    override fun onAdded() {
        messageIds.forEach { messageId ->
            val message = runBlocking { getMessageDetailsRepository().findMessageById(messageId).firstOrNull() }
            if (message == null) {
                Timber.d("Trying to unstar a message which was not found in the DB. messageId = $messageId")
                return
            }

            unstarLocalMessage(message)

            val messageLocation = fromInt(message.location)
            val isUnread = !message.isRead
            if (messageLocation !== MessageLocationType.INVALID && isUnread) {
                updateUnreadMessagesCounter(messageLocation)
            }
        }
    }

    override fun onRun() {
        val messageIds: List<String> = ArrayList(messageIds)
        getApi().unlabelMessages(IDList(MessageLocationType.STARRED.messageLocationTypeValue.toString(), messageIds))
    }

    private fun unstarLocalMessage(message: Message) {
        runBlocking {
            message.removeLabels(listOf(MessageLocationType.STARRED.messageLocationTypeValue.toString()))
            message.isStarred = false
            getMessageDetailsRepository().saveMessage(message)
        }
    }

    private fun updateUnreadMessagesCounter(messageLocation: MessageLocationType) {
        val counterDao = CounterDatabase
            .getInstance(applicationContext, userId!!)
            .getDao()

        val locationId = messageLocation.messageLocationTypeValue
        val unreadLocationCounter = counterDao.findUnreadLocationByIdBlocking(locationId) ?: return
        unreadLocationCounter.increment(messageIds.size)

        val countersToUpdate: MutableList<UnreadLocationCounter> = ArrayList()
        countersToUpdate.add(unreadLocationCounter)

        val starredUnread = counterDao.findUnreadLocationByIdBlocking(MessageLocationType.STARRED.messageLocationTypeValue)
        if (starredUnread != null) {
            starredUnread.increment()
            countersToUpdate.add(starredUnread)
        }
        counterDao.insertAllUnreadLocations(countersToUpdate)
    }
}
