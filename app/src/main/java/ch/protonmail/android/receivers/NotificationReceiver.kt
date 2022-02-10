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
import ch.protonmail.android.notifications.presentation.usecase.ClearNotification
import ch.protonmail.android.notifications.presentation.usecase.ClearNotificationsForUser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

const val EXTRA_NOTIFICATION_USER_ID = "notification_user_id"
const val EXTRA_NOTIFICATION_MESSAGE_ID = "notification_message_id"
const val EXTRA_NOTIFICATION_NEW_LOCATION_MESSAGE = "notification_new_location_message"
const val EXTRA_NOTIFICATION_DISMISS = "notification_dismiss"
const val EXTRA_NOTIFICATION_GROUP_DISMISS = "notification_group_dismiss"

@AndroidEntryPoint
internal class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var moveMessagesToFolder: MoveMessagesToFolder

    @Inject
    lateinit var clearNotification: ClearNotification

    @Inject
    lateinit var clearNotificationsForUser: ClearNotificationsForUser

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras

        if (extras != null) {
            val userId = UserId(
                checkNotNull(extras.getString(EXTRA_NOTIFICATION_USER_ID)) { "Notification extra, user Id is null" }
            )

            if (extras.containsKey(EXTRA_NOTIFICATION_GROUP_DISMISS)) {
                coroutineScope.launch {
                    clearNotificationsForUser(userId)
                }
            } else {
                val messageId = checkNotNull(extras.getString(EXTRA_NOTIFICATION_MESSAGE_ID))

                if (extras.containsKey(EXTRA_NOTIFICATION_DISMISS)) {
                    coroutineScope.launch { clearNotification(userId, messageId) }
                } else {
                    val newLocation = checkNotNull(extras.getString(EXTRA_NOTIFICATION_NEW_LOCATION_MESSAGE))

                    coroutineScope.launch {
                        moveMessagesToFolder(
                            listOf(messageId),
                            newLocation,
                            userId = userId
                        )
                        clearNotification(userId, messageId)
                    }
                }
            }
            val alarmReceiver = AlarmReceiver()
            alarmReceiver.setAlarm(context, true)
        }
    }
}
