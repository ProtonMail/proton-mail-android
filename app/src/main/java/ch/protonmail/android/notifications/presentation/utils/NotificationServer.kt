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
package ch.protonmail.android.notifications.presentation.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.media.RingtoneManager.TYPE_NOTIFICATION
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import ch.protonmail.android.R
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.presentation.ui.SwitchUserAndOpenMessageDetailsActivity
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.User
import ch.protonmail.android.mailbox.presentation.ui.MailboxActivity
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.buildArchiveIntent
import ch.protonmail.android.utils.buildDismissGroupIntent
import ch.protonmail.android.utils.buildDismissIntent
import ch.protonmail.android.utils.buildTrashIntent
import ch.protonmail.android.utils.extensions.getColorFromAttr
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.getMailboxActivityIntent
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import ch.protonmail.android.api.models.User as LegacyUser

const val CHANNEL_ID_EMAIL = "emails"
const val EXTRA_MAILBOX_LOCATION = "mailbox_location"
const val EXTRA_USER_ID = "user.id"
const val NOTIFICATION_ID_SENDING_FAILED = 680
private const val CHANNEL_ID_ONGOING_OPS = "ongoingOperations"
private const val CHANNEL_ID_ACCOUNT = "account"
private const val CHANNEL_ID_ATTACHMENTS = "attachments"
private const val NOTIFICATION_ID_SAVE_DRAFT_ERROR = 6812
private const val NOTIFICATION_ID_ATTACHMENT_ERROR = 6813
// endregion

/**
 * A class that is responsible for creating notification channels, and creating and showing notifications.
 */
class NotificationServer @Inject constructor(
    private val context: Context,
    private val notificationManager: NotificationManager
) {

    private val lightIndicatorColor by lazy {
        context.getColorFromAttr(R.attr.brand_norm)
    }

    fun createAccountChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(CHANNEL_ID_ACCOUNT)
            if (channel != null) {
                return CHANNEL_ID_ACCOUNT
            }
            val name = context.getString(R.string.channel_name_account)
            val description = context.getString(R.string.channel_description_account)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            channel = NotificationChannel(CHANNEL_ID_ACCOUNT, name, importance)
            channel.description = description
            channel.lightColor = lightIndicatorColor
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
        }
        return CHANNEL_ID_ACCOUNT
    }

    fun createAttachmentsChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(CHANNEL_ID_ATTACHMENTS)
            if (channel != null) {
                return CHANNEL_ID_ATTACHMENTS
            }
            val name = context.getString(R.string.channel_name_attachments)
            val description = context.getString(R.string.channel_description_attachments)
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            channel = NotificationChannel(CHANNEL_ID_ATTACHMENTS, name, importance)
            channel.description = description
            channel.setSound(null, null)
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
        }
        return CHANNEL_ID_ATTACHMENTS
    }

    fun createEmailsChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(CHANNEL_ID_EMAIL)
            if (channel != null) {
                return CHANNEL_ID_EMAIL
            }
            val name = context.getString(R.string.channel_name_emails)
            val channelDescription = context.getString(R.string.channel_description_emails)
            val importance = NotificationManager.IMPORTANCE_HIGH
            channel = NotificationChannel(CHANNEL_ID_EMAIL, name, importance).apply {
                description = channelDescription
                lightColor = lightIndicatorColor
                enableLights(true)
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        return CHANNEL_ID_EMAIL
    }

    private fun createOngoingOperationChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(CHANNEL_ID_ONGOING_OPS)
            if (channel != null) {
                return CHANNEL_ID_ONGOING_OPS
            }
            val name = context.getString(R.string.channel_name_ongoing_operations)
            val description = context.getString(R.string.channel_description_ongoing_operations)
            val importance = NotificationManager.IMPORTANCE_LOW

            channel = NotificationChannel(CHANNEL_ID_ONGOING_OPS, name, importance)
            channel.description = description
            channel.setSound(null, null)
            channel.setShowBadge(false)
            notificationManager.createNotificationChannel(channel)
        }
        return CHANNEL_ID_ONGOING_OPS
    }

    fun createRetrievingNotificationsNotification(): Notification {
        val channelId = createOngoingOperationChannel()
        val notificationTitle = context.getString(R.string.retrieving_notifications)
        return NotificationCompat.Builder(context, channelId)
            .setPriority(PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_brand_mail)
            .setContentTitle(notificationTitle)
            .setSilent(true)
            .build()
    }

    fun notifyAboutAttachment(
        filename: String,
        uri: Uri,
        mimeType: String?,
        showNotification: Boolean
    ) {
        val channelId = createAttachmentsChannel()
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(filename)
            .setContentText(context.getString(R.string.download_complete))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setProgress(0, 0, false)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.type = mimeType
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(uri, mimeType)
        val viewAttachmentIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(viewAttachmentIntent)
        if (intent.resolveActivity(context.packageManager) != null) {
            if (showNotification) {
                notificationManager.notify(-1, builder.build())
            }
        } else {
            builder.setContentText(context.getString(R.string.no_application_found))
            notificationManager.notify(-1, builder.build())
            context.showToast(R.string.no_application_found)
        }
    }

    /**
     * @return [NotificationCompat.Builder] with common parameters that will be used from
     * [notifySingleNewEmail]  and [notifyMultipleUnreadEmail]
     *
     * @param user [LegacyUser] for get some Notification's settings
     */
    private fun createGenericEmailNotification(
        notificationSettings: Int,
        ringtoneUri: Uri?,
        isNotificationVisibleInLockScreen: Boolean
    ): NotificationCompat.Builder {

        // Schedule a Wakelock
        // TODO by Davide Farella: Perhaps schedule with Work Manager?
        val alarmReceiver = AlarmReceiver()
        alarmReceiver.setAlarm(context, true)

        // Set Notification's colors
        val lightColor = context.getColorFromAttr(R.attr.brand_norm)

        // Create Notification's Builder with the prepared params
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_EMAIL)
            .setSmallIcon(R.drawable.ic_brand_mail)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setLights(lightColor, 1500, 2000)
            .setAutoCancel(true)
            .setPriority(PRIORITY_HIGH)

        // Set Notification visibility
        if (isNotificationVisibleInLockScreen) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        // Set Vibration - TODO those Int's are not very clear :/
        if (notificationSettings == 2 || notificationSettings == 3) {
            builder.setVibrate(longArrayOf(1000, 500))
        }

        // Set Sound - TODO those Int's are not very clear :/
        if (notificationSettings == 1 || notificationSettings == 3) {
            val notificationSound = try {
                // TODO Make sure we have needed permissions and sound file can be read -
                //  I am not sure if this even does anything (Adam)
                // Asserting the user's ringtone is not null, otherwise an exception will be thrown
                // and so fallback to default ringtone
                ringtoneUri?.also { uri ->
                    context.contentResolver.openInputStream(uri).use {
                        context.grantUriPermission(
                            "com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                } ?: RingtoneManager.getDefaultUri(TYPE_NOTIFICATION)
            } catch (e: Exception) {
                Timber.i(e, "Unable to set notification ringtone")
                RingtoneManager.getDefaultUri(TYPE_NOTIFICATION)
            }

            Timber.v("Setting notification sound: $notificationSound")
            builder.setSound(notificationSound)
        }

        return builder
    }

    /**
     * Show a Notification for a SINGLE new Email. This will be called ONLY if there are not other
     * unread Notifications
     *
     * @param userManager // FIXME: FIND A BETTER SOLUTION - [UserManager] cannot be instantiated on Main Thread :/
     * @param user current logged [User]
     * @param message [Message] received to show to the user
     * @param messageId [String] id for retrieve the [Message] details
     * @param notificationBody [String] body of the Notification
     * @param sender [String] name of the sender of the email
     */
    fun notifySingleNewEmail(
        userManager: UserManager,
        user: User,
        notificationSettings: Int,
        ringtoneUri: Uri?,
        isNotificationVisibleInLockScreen: Boolean,
        message: Message?,
        messageId: String,
        notificationBody: String,
        userId: UserId,
        sender: String,
        primaryUser: Boolean
    ) {
        // Create content Intent for open SwitchUserAndOpenMessageDetailsActivity
        val contentIntent = SwitchUserAndOpenMessageDetailsActivity.Input(
            userId = userId,
            messageId = messageId,
            locationType = message?.location?.let(MessageLocationType::fromInt) ?: MessageLocationType.INVALID,
            messageSubject = notificationBody
        ).toIntent(context)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NO_ANIMATION)

        val summaryContentIntent = getMailboxActivityIntent(userId)

        val backIntent = Intent(context, MailboxActivity::class.java)
        backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val contentPendingIntent = PendingIntent.getActivities(
            context,
            messageId.hashCode(),
            arrayOf(backIntent, contentIntent),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create Action Intent's
        val dismissIntent = context.buildDismissIntent(messageId, user.id)
        val dismissGroupIntent = context.buildDismissGroupIntent(user.id)
        val archiveIntent = context.buildArchiveIntent(messageId, user.id)
        val trashIntent = context.buildTrashIntent(messageId, user.id)
        val replyIntent =
            if (primaryUser) message?.let { context.buildReplyIntent(message, user, userManager) } else null

        // Create Notification Style
        val userDisplayName = user.addresses.primary?.email?.s
            ?: user.name.s

        // Create Notification's Builder with the prepared params
        val notification =
            createGenericEmailNotification(
                notificationSettings,
                ringtoneUri,
                isNotificationVisibleInLockScreen
            ).apply {
                setContentTitle(sender)
                setSubText(message?.toListString ?: userDisplayName)
                setContentText(notificationBody)
                setContentIntent(contentPendingIntent)
                setDeleteIntent(dismissIntent)
                setGroup(userId.id)
                addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_proton_archive_box,
                        context.getString(R.string.archive),
                        archiveIntent
                    )
                )
                addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_proton_trash,
                        context.getString(R.string.trash),
                        trashIntent
                    )
                )
                if (replyIntent != null) {
                    addAction(
                        NotificationCompat.Action(
                            R.drawable.ic_proton_arrow_up_and_left,
                            context.getString(R.string.reply),
                            replyIntent
                        )
                    )
                }
            }.build()

        val summaryNotification = createGenericEmailNotification(
            notificationSettings,
            ringtoneUri,
            isNotificationVisibleInLockScreen
        ).apply {
            setContentText(context.getString(R.string.notification_summary_text_new_messages))
            setSubText(message?.toListString ?: userDisplayName)
            setGroup(userId.id)
            setGroupSummary(true)
            setAutoCancel(true)
            setContentIntent(summaryContentIntent)
            setDeleteIntent(dismissGroupIntent)
        }.build()


        NotificationManagerCompat.from(context).apply {
            notify(messageId.hashCode(), notification)
            notify(userId.id.hashCode(), summaryNotification)
        }
    }

    private fun getMailboxActivityIntent(loggedInUserId: UserId): PendingIntent {
        // Create content Intent for open MailboxActivity
        val contentIntent = context.getMailboxActivityIntent(UserId(loggedInUserId.id))
        val requestCode = System.currentTimeMillis().toInt()
        return PendingIntent.getActivity(context, requestCode, contentIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createGenericErrorSendingMessageNotification(
        userId: UserId
    ): NotificationCompat.Builder {

        // Create channel and get id
        val channelId = createAccountChannel()

        // Create content Intent to open Drafts
        val contentIntent = context.getMailboxActivityIntent(userId, Constants.MessageLocationType.DRAFT)

        val stackBuilder = TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(contentIntent)

        val contentPendingIntent = stackBuilder.getPendingIntent(
            userId.hashCode() + NOTIFICATION_ID_SENDING_FAILED,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set Notification's colors
        val lightColor = context.getColorFromAttr(R.attr.brand_norm)

        // Create notification builder
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_brand_mail)
            .setContentIntent(contentPendingIntent)
            .setLights(lightColor, 1500, 2000)
            .setAutoCancel(true)
    }

    fun notifySingleErrorSendingMessage(
        userId: UserId,
        username: Name,
        error: String,
        subject: String = ""
    ) {
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setSummaryText(username.s)
            .bigText(error)

        val notificationBuilder = createGenericErrorSendingMessageNotification(userId)
            .setContentTitle(context.getString(R.string.message_failed) + " " + subject)
            .setStyle(bigTextStyle)

        val notification = notificationBuilder.build()
        notificationManager.notify(
            (userId.id + error).hashCode() + NOTIFICATION_ID_SENDING_FAILED,
            notification
        )
    }

    private fun notifyErrorWithTitle(
        userId: UserId,
        title: String,
        bigText: String?,
        summaryText: String,
        notificationID: Int
    ) {
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setSummaryText(summaryText)
            .bigText(bigText)

        val notificationBuilder = createGenericErrorSendingMessageNotification(userId).setContentTitle(title)
            .setStyle(bigTextStyle)

        val notification = notificationBuilder.build()
        notificationManager.notify(userId.hashCode() + notificationID, notification)
    }

    fun notifySaveDraftError(
        userId: UserId,
        errorMessage: String?,
        messageSubject: String?,
        username: Name
    ) {
        val title = context.getString(R.string.failed_saving_draft_online, messageSubject)
        notifyErrorWithTitle(
            userId,
            title,
            errorMessage,
            username.s,
            notificationID = NOTIFICATION_ID_SAVE_DRAFT_ERROR,
        )
    }

    fun notifyAttachmentUploadError(
        userId: UserId,
        errorMessage: String,
        messageSubject: String?,
        username: Name
    ) {
        val title = context.getString(R.string.failed_uploading_attachment_online, messageSubject)
        notifyErrorWithTitle(
            userId,
            title,
            errorMessage,
            username.s,
            notificationID = NOTIFICATION_ID_ATTACHMENT_ERROR
        )
    }


    fun notifyOpenUrlNotification(
        user: User,
        notificationSettings: Int,
        ringtoneUri: Uri?,
        isNotificationVisibleInLockScreen: Boolean,
        notificationUrl: String,
        messageId: String,
        notificationBody: String?,
        sender: String
    ) {
        val contentIntent = Intent(Intent.ACTION_VIEW, Uri.parse(notificationUrl))

        val backIntent = Intent(context, MailboxActivity::class.java)
        backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val contentPendingIntent = PendingIntent.getActivities(
            context,
            messageId.hashCode(),
            arrayOf(backIntent, contentIntent),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create Action Intent's
        val dismissIntent = context.buildDismissIntent(messageId, user.id)

        // Create Notification Style
        val userDisplayName = user.addresses.primary?.email?.s
            ?: user.name.s
        val inboxStyle = NotificationCompat.BigTextStyle().run {
            setSummaryText(userDisplayName)
            notificationBody?.let { bigText(it) }
        }

        // Create Notification's Builder with the prepared params
        val notification =
            createGenericEmailNotification(
                notificationSettings,
                ringtoneUri,
                isNotificationVisibleInLockScreen
            ).apply {
                setContentTitle(sender)
                notificationBody?.let { setContentText(it) }
                setContentIntent(contentPendingIntent)
                setDeleteIntent(dismissIntent)
                setStyle(inboxStyle)
                setShowWhen(false)
            }.build()

        NotificationManagerCompat.from(context).apply {
            notify(messageId.hashCode(), notification)
        }
    }
}

private fun Context.buildReplyIntent(
    message: Message,
    user: User,
    userManager: UserManager
): PendingIntent? {

    val intent = Intent(this, ComposeMessageActivity::class.java)
    MessageUtils.addRecipientsToIntent(
        intent,
        ComposeMessageActivity.EXTRA_TO_RECIPIENTS,
        message.senderEmail,
        Constants.MessageActionType.REPLY,
        user.addresses
    )

    val newMessageTitle = MessageUtils.buildNewMessageTitle(
        this,
        Constants.MessageActionType.REPLY, message.subject
    )

    if (message.messageBody != null) {
        message.decrypt(userManager, user.id)
    }

    intent
        .putExtra(ComposeMessageActivity.EXTRA_REPLY_FROM_NOTIFICATION, true)
        .putExtra(ComposeMessageActivity.EXTRA_SENDER_NAME, message.senderName)
        .putExtra(ComposeMessageActivity.EXTRA_SENDER_ADDRESS, message.sender?.emailAddress)
        .putExtra(ComposeMessageActivity.EXTRA_MESSAGE_TITLE, newMessageTitle)
        .putExtra(ComposeMessageActivity.EXTRA_MESSAGE_BODY, message.decryptedHTML)
        .putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID, message.messageId)
        .putExtra(ComposeMessageActivity.EXTRA_MESSAGE_TIMESTAMP, message.timeMs)
        .putExtra(ComposeMessageActivity.EXTRA_PARENT_ID, message.messageId)
        .putExtra(ComposeMessageActivity.EXTRA_ACTION_ID, Constants.MessageActionType.REPLY)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

    val backIntent = Intent(this, MailboxActivity::class.java)
    backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

    return PendingIntent.getActivities(
        this,
        message.messageId.hashCode(),
        arrayOf(backIntent, intent),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
