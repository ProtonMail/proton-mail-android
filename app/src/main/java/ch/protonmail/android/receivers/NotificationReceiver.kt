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
package ch.protonmail.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.jobs.PostArchiveJob
import ch.protonmail.android.jobs.PostTrashJobV2
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// region constants
const val EXTRA_NOTIFICATION_ARCHIVE_MESSAGE = "notification_archive_message"
const val EXTRA_NOTIFICATION_DELETE_MESSAGE = "notification_delete_message"
const val EXTRA_NOTIFICATION_TRASH_MESSAGE = "notification_trash_message"
// endregion

/*
 * Created by dkadrikj on 29.9.15.
 */
@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var jobManager: JobManager

    @Inject
    lateinit var messageDetailsRepository: MessageDetailsRepository

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras
        if (extras != null && extras.containsKey(EXTRA_NOTIFICATION_ARCHIVE_MESSAGE)) {
            val messageId = extras.getString(EXTRA_NOTIFICATION_ARCHIVE_MESSAGE)
            val job: Job = PostArchiveJob(listOf(messageId))
            jobManager.addJobInBackground(job)
            AppUtil.clearNotifications(context)
        } else if (extras != null && extras.containsKey(EXTRA_NOTIFICATION_TRASH_MESSAGE)) {
            val messageId = extras.getString(EXTRA_NOTIFICATION_TRASH_MESSAGE)
            if (messageId != null) { // double check null because of @Nullable for getString (but will never be null)
                coroutineScope.launch {
                    moveMessageToTrash(jobManager, messageDetailsRepository, messageId)
                }
                AppUtil.clearNotifications(context)
            }
        } else if (extras != null && extras.containsKey(EXTRA_NOTIFICATION_DELETE_MESSAGE)) {
            AppUtil.clearNotifications(context)
        }
        val alarmReceiver = AlarmReceiver()
        alarmReceiver.setAlarm(context, true)
    }

    private suspend fun moveMessageToTrash(
        jobManager: JobManager,
        messageDetailsRepository: MessageDetailsRepository,
        messageId: String
    ) {
        withContext(Dispatchers.Default) {
            val message = messageDetailsRepository.findMessageByIdBlocking(messageId)
            if (message != null) {
                val job: Job = PostTrashJobV2(listOf(message.messageId), null)
                jobManager.addJobInBackground(job)
            }
        }
    }
}
