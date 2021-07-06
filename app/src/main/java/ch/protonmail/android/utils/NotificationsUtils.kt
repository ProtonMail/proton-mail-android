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
@file:JvmName("NotificationsUtils")

package ch.protonmail.android.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ch.protonmail.android.R
import ch.protonmail.android.receivers.EXTRA_NOTIFICATION_ARCHIVE_MESSAGE
import ch.protonmail.android.receivers.EXTRA_NOTIFICATION_TRASH_MESSAGE
import ch.protonmail.android.receivers.NotificationReceiver

// TODO move to GCMService
fun Context.buildArchiveIntent(messageId: String): PendingIntent {
    val intent = Intent(getString(R.string.notification_action_archive))
    intent.putExtra(EXTRA_NOTIFICATION_ARCHIVE_MESSAGE, messageId)
    intent.setClass(this, NotificationReceiver::class.java)
    return PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, 0)
}

// TODO move to GCMService
fun Context.buildTrashIntent(messageId: String): PendingIntent {
    val intent = Intent(getString(R.string.notification_action_trash))
    intent.putExtra(EXTRA_NOTIFICATION_TRASH_MESSAGE, messageId)
    intent.setClass(this, NotificationReceiver::class.java)
    return PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, 0)
}
