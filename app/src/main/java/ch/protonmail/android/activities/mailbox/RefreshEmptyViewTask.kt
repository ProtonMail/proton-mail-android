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
package ch.protonmail.android.activities.mailbox

import android.os.AsyncTask
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.data.local.CounterDao
import ch.protonmail.android.data.local.MessageDao
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

internal class RefreshEmptyViewTask(
    private val mailboxActivityWeakReference: WeakReference<MailboxActivity>,
    private val counterDao: CounterDao,
    private val messageDao: MessageDao,
    private val mailboxLocation: MessageLocationType,
    private val labelId: String?
) : AsyncTask<Void, Void, Int>() {

    override fun doInBackground(vararg voids: Void): Int? {
        if (mailboxLocation == MessageLocationType.STARRED) {
            return null
        }
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        val counter = if (mailboxLocation in listOf(MessageLocationType.LABEL, MessageLocationType.LABEL_FOLDER)) {
            labelId?.let(counterDao::findTotalLabelById)
        } else {
            counterDao.findTotalLocationById(mailboxLocation.messageLocationTypeValue)
        }

        val localCounter = if (mailboxLocation in listOf(MessageLocationType.LABEL, MessageLocationType.LABEL_FOLDER)) {
            messageDao.getMessagesCountByByLabelId(labelId!!)
        } else {
            messageDao.getMessagesCountByLocation(mailboxLocation.messageLocationTypeValue)
        }

        val apiCounter = counter?.count ?: 0
        return if (localCounter > apiCounter) localCounter else apiCounter
    }

    override fun onPostExecute(messageCount: Int?) {
        messageCount ?: return

        val mailboxActivity = mailboxActivityWeakReference.get()
        mailboxActivity?.refreshEmptyView(messageCount)
        mailboxActivity?.setRefreshing(false)
    }
}
