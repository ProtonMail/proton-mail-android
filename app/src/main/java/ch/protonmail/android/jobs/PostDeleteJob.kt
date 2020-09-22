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
import ch.protonmail.android.events.MessageDeletedEvent
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.Params
import java.util.ArrayList

class PostDeleteJob(private val messageIds: List<String>) : ProtonMailEndlessJob(Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE)) {

    override fun onAdded() {
        val validAndInvalidMessagesPair = messageIds.getValidAndInvalidMessages()
        val validMessageIdList = validAndInvalidMessagesPair.first

        for (id in validMessageIdList) {

            val message = getMessageDetailsRepository().findMessageById(id)
            val searchMessage = getMessageDetailsRepository().findSearchMessageById(id)

            if (message != null) {
                message.deleted = true
                getMessageDetailsRepository().saveMessageInDB(message)
            }
            if (searchMessage != null) {
                searchMessage.deleted = true
                getMessageDetailsRepository().saveSearchMessageInDB(searchMessage)
            }
        }
    }

    @Throws(Throwable::class)
    override fun onRun() {
        val validAndInvalidMessagesPair = messageIds.getValidAndInvalidMessages()
        val validMessageIdList = validAndInvalidMessagesPair.first
        val invalidMessageIdList = validAndInvalidMessagesPair.second

        if (validMessageIdList.isNotEmpty()) {
            getApi().deleteMessage(IDList(validMessageIdList))
        }
        if (invalidMessageIdList.isNotEmpty()) {
            AppUtil.postEventOnUi(MessageDeletedEvent(invalidMessageIdList))
        }
    }

    private fun List<String>.getValidAndInvalidMessages(): Pair<List<String>, List<String>> {
        val validMessageIdList = ArrayList<String>()
        val invalidMessageIdList = ArrayList<String>()

        for (id in this) {
            if (id.isEmpty()) {
                continue
            }
            val pendingUploads = getMessageDetailsRepository().findPendingUploadById(id)
            val pendingForSending = getMessageDetailsRepository().findPendingSendByMessageId(id)
            val sent = pendingForSending?.sent
            if (pendingUploads == null && (pendingForSending == null || (sent != null && !sent))) {
                // do the logic below if there is no pending upload and not pending send for the message trying to be deleted
                // or if there is a failed pending send expressed by value `false` in the nullable Sent property of the PendingSend class
                validMessageIdList.add(id)
            } else {
                invalidMessageIdList.add(id)
            }
        }

        return Pair(validMessageIdList, invalidMessageIdList)
    }
}
