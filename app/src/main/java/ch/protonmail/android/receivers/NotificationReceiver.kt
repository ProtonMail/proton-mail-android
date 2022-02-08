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
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.utils.AppUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

const val EXTRA_NOTIFICATION_USER_ID = "notification_user_id"
const val EXTRA_NOTIFICATION_MESSAGE_ID = "notification_message_id"
const val EXTRA_NOTIFICATION_NEW_LOCATION_MESSAGE = "notification_new_location_message"
const val EXTRA_NOTIFICATION_DELETE_MESSAGE = "notification_delete_message"

@AndroidEntryPoint
internal class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var moveMessagesToFolder: MoveMessagesToFolder

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras

        if (extras != null) {
            if (extras.containsKey(EXTRA_NOTIFICATION_DELETE_MESSAGE)) {
                AppUtil.clearNotifications(context)
            } else {
                val userId = UserId(checkNotNull(extras.getString(EXTRA_NOTIFICATION_USER_ID)))
                val messageId = checkNotNull(extras.getString(EXTRA_NOTIFICATION_MESSAGE_ID))
                val newLocation = checkNotNull(extras.getString(EXTRA_NOTIFICATION_NEW_LOCATION_MESSAGE))

                coroutineScope.launch {
                    moveMessagesToFolder(
                        listOf(messageId),
                        newLocation,
                        userId = userId
                    )
                    AppUtil.clearNotifications(context)
                }
            }
            val alarmReceiver = AlarmReceiver()
            alarmReceiver.setAlarm(context, true)
        }
    }
}
