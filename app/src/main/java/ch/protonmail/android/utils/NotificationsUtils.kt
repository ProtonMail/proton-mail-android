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
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.receivers.EXTRA_NOTIFICATION_ARCHIVE_MESSAGE
import ch.protonmail.android.receivers.EXTRA_NOTIFICATION_READ_MESSAGE
import ch.protonmail.android.receivers.EXTRA_NOTIFICATION_TRASH_MESSAGE
import ch.protonmail.android.receivers.NotificationReceiver

/**
 * Created by dkadrikj on 12/13/15.  */
fun Context.buildReplyIntent(
    message: Message,
    user: User,
    userManager: UserManager
): PendingIntent? {
    val intent = Intent(this, ComposeMessageActivity::class.java)
    MessageUtils.addRecipientsToIntent(intent, ComposeMessageActivity.EXTRA_TO_RECIPIENTS,
            message.senderEmail, Constants.MessageActionType.REPLY, user.addresses)

    val newMessageTitle = MessageUtils.buildNewMessageTitle(this,
            Constants.MessageActionType.REPLY, message.subject)

    if (message.messageBody != null) {
        message.decrypt(userManager, userManager.username)
    }

    intent.putExtra(ComposeMessageActivity.EXTRA_REPLY_FROM_GCM, true)
    intent.putExtra(ComposeMessageActivity.EXTRA_SENDER_NAME, message.senderName)
    intent.putExtra(ComposeMessageActivity.EXTRA_SENDER_ADDRESS, message.sender?.emailAddress)
    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_TITLE, newMessageTitle)
    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_BODY, message.decryptedHTML)
    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID, message.messageId)
    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_TIMESTAMP, message.timeMs)
    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ENCRYPTED, message.isEncrypted())
    intent.putExtra(ComposeMessageActivity.EXTRA_PARENT_ID, message.messageId)
    intent.putExtra(ComposeMessageActivity.EXTRA_ACTION_ID, Constants.MessageActionType.REPLY)

    return PendingIntent.getActivity(this, System.currentTimeMillis().toInt(), intent, 0)
}

//TODO move to GCMService
fun Context.buildArchiveIntent(messageId: String): PendingIntent {
    val intent = Intent(getString(R.string.notification_action_archive))
    intent.putExtra(EXTRA_NOTIFICATION_ARCHIVE_MESSAGE, messageId)
    intent.setClass(this, NotificationReceiver::class.java)
    return PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, 0)
}

//TODO move to GCMService
fun Context.buildTrashIntent(messageId: String): PendingIntent {
    val intent = Intent(getString(R.string.notification_action_trash))
    intent.putExtra(EXTRA_NOTIFICATION_TRASH_MESSAGE, messageId)
    intent.setClass(this, NotificationReceiver::class.java)
    return PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, 0)
}

fun Context.buildReadIntent(messageId: String): PendingIntent {
    val intent = Intent(getString(R.string.notification_action_archive))
    intent.putExtra(EXTRA_NOTIFICATION_READ_MESSAGE, messageId)
    intent.setClass(this, NotificationReceiver::class.java)
    return PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, 0)
}