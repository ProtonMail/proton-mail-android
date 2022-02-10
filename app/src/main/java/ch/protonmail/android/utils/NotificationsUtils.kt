/*
 * Copyright (c) 2022 Proton Technologies AG
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
@file:JvmName("NotificationsUtils")

package ch.protonmail.android.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.receivers.EXTRA_NOTIFICATION_DISMISS
import ch.protonmail.android.receivers.EXTRA_NOTIFICATION_GROUP_DISMISS
import ch.protonmail.android.receivers.EXTRA_NOTIFICATION_MESSAGE_ID
import ch.protonmail.android.receivers.EXTRA_NOTIFICATION_NEW_LOCATION_MESSAGE
import ch.protonmail.android.receivers.EXTRA_NOTIFICATION_USER_ID
import ch.protonmail.android.receivers.NotificationReceiver
import me.proton.core.domain.entity.UserId

fun Context.buildArchiveIntent(messageId: String, userId: UserId): PendingIntent {
    val intent = Intent(getString(R.string.notification_action_archive))
    intent.putExtra(EXTRA_NOTIFICATION_USER_ID, userId.id)
    intent.putExtra(EXTRA_NOTIFICATION_MESSAGE_ID, messageId)
    intent.putExtra(
        EXTRA_NOTIFICATION_NEW_LOCATION_MESSAGE,
        Constants.MessageLocationType.ARCHIVE.asLabelIdString()
    )
    intent.setClass(this, NotificationReceiver::class.java)
    return PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE)
}

fun Context.buildTrashIntent(messageId: String, userId: UserId): PendingIntent {
    val intent = Intent(getString(R.string.notification_action_trash))
    intent.putExtra(EXTRA_NOTIFICATION_USER_ID, userId.id)
    intent.putExtra(EXTRA_NOTIFICATION_MESSAGE_ID, messageId)
    intent.putExtra(
        EXTRA_NOTIFICATION_NEW_LOCATION_MESSAGE, Constants.MessageLocationType.TRASH.asLabelIdString()
    )
    intent.setClass(this, NotificationReceiver::class.java)
    return PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE)
}

fun Context.buildDismissIntent(messageId: String, userId: UserId): PendingIntent {
    val intent = Intent(getString(R.string.notification_action_dismiss))
    intent.putExtra(EXTRA_NOTIFICATION_USER_ID, userId.id)
    intent.putExtra(EXTRA_NOTIFICATION_MESSAGE_ID, messageId)
    intent.putExtra(EXTRA_NOTIFICATION_DISMISS, true)
    intent.setClass(this, NotificationReceiver::class.java)
    return PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE)
}

fun Context.buildDismissGroupIntent(userId: UserId): PendingIntent {
    val intent = Intent(getString(R.string.notification_action_dismiss))
    intent.putExtra(EXTRA_NOTIFICATION_USER_ID, userId.id)
    intent.putExtra(EXTRA_NOTIFICATION_GROUP_DISMISS, true)
    intent.setClass(this, NotificationReceiver::class.java)
    return PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE)
}
