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
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.ArrayList

class PostStarJob(private val messageIds: List<String>) : ProtonMailEndlessJob(
    Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL)
) {

    override fun onAdded() {
        messageIds.forEach { starLocalMessage(it) }
    }

    @Throws(Throwable::class)
    override fun onRun() {
        val messageIds: List<String> = ArrayList(messageIds)
        getApi().labelMessages(
            IDList(Constants.MessageLocationType.STARRED.messageLocationTypeValue.toString(), messageIds)
        )
    }

    private fun starLocalMessage(messageId: String) {
        runBlocking {
            val message = getMessageDetailsRepository().findMessageById(messageId).firstOrNull()
            if (message == null) {
                Timber.d("Trying to star message which was not found in the DB. messageId = $messageId")
                return@runBlocking
            }

            message.addLabels(listOf(Constants.MessageLocationType.STARRED.messageLocationTypeValue.toString()))
            message.isStarred = true
            getMessageDetailsRepository().saveMessage(message)
        }
    }

}
