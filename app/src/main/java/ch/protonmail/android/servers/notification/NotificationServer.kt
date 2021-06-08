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
package ch.protonmail.android.servers.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface.BOLD
import android.media.RingtoneManager
import android.media.RingtoneManager.TYPE_NOTIFICATION
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import ch.protonmail.android.R
import ch.protonmail.android.activities.EXTRA_SWITCHED_TO_USER
import ch.protonmail.android.activities.EXTRA_SWITCHED_USER
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.activities.mailbox.MailboxActivity
import ch.protonmail.android.activities.messageDetails.MessageDetailsActivity
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.sendingFailedNotifications.SendingFailedNotification
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.receivers.EXTRA_NOTIFICATION_DELETE_MESSAGE
import ch.protonmail.android.utils.buildArchiveIntent
import ch.protonmail.android.utils.buildReplyIntent
import ch.protonmail.android.utils.buildTrashIntent
import ch.protonmail.android.utils.extensions.getColorCompat
import ch.protonmail.android.utils.extensions.showToast
import timber.log.Timber
import javax.inject.Inject
import ch.protonmail.android.api.models.room.notifications.Notification as RoomNotification

const val CHANNEL_ID_EMAIL = "emails"
const val EXTRA_MAILBOX_LOCATION = "mailbox_location"
const val EXTRA_USERNAME = "username"
const val NOTIFICATION_ID_SENDING_FAILED = 680
private const val CHANNEL_ID_ONGOING_OPS = "ongoingOperations"
private const val CHANNEL_ID_ACCOUNT = "account"
private const val CHANNEL_ID_ATTACHMENTS = "attachments"
private const val NOTIFICATION_ID_SAVE_DRAFT_ERROR = 6812
private const val NOTIFICATION_GROUP_ID_EMAIL = 99
private const val NOTIFICATION_ID_VERIFICATION = 2
private const val NOTIFICATION_ID_LOGGED_OUT = 3
private const val NOTIFICATION_LIGHT_ON = 1500
private const val NOTIFICATION_LIGHT_OFF = 2000
private const val NOTIFICATION_SETTING_SOUND_ONLY = 1
private const val NOTIFICATION_SETTING_VIBRATE_ONLY = 2
public const val NOTIFICATION_SETTING_SOUND_VIBRATE = 3
private const val NOTIFICATION_VIBRATE_ON = 1000L
private const val NOTIFICATION_VIBRATE_OFF = 500L

/**
 * A class that is responsible for creating notification channels, and creating and showing notifications.
 */
class NotificationServer @Inject constructor(
    private val context: Context,
    private val notificationManager: NotificationManager
) : INotificationServer {

    private val lightIndicatorColor by lazy {
        ContextCompat.getColor(context, R.color.light_indicator)
    }

    override fun createAccountChannel(): String {
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

    override fun createAttachmentsChannel(): String {
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

    override fun createEmailsChannel(): String {
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
            val importance = NotificationManager.IMPORTANCE_MIN

            channel = NotificationChannel(CHANNEL_ID_ONGOING_OPS, name, importance)
            channel.description = description
            channel.setSound(null, null)
            channel.setShowBadge(false)
            notificationManager.createNotificationChannel(channel)
        }
        return CHANNEL_ID_ONGOING_OPS
    }

    override fun notifyVerificationNeeded(
        username: String,
        messageSubject: String?,
        messageId: String?,
        messageInline: Boolean,
        messageAddressId: String?
    ) {
        val inboxStyle = NotificationCompat.BigTextStyle()
        inboxStyle.setBigContentTitle(context.getString(R.string.verification_needed))
        inboxStyle.bigText(
            String.format(
                context.getString(R.string.verification_needed_description_notification),
                messageSubject
            )
        )

        inboxStyle.setSummaryText(username)
        val composeIntent = Intent(context, ComposeMessageActivity::class.java)
        composeIntent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID, messageId)
        composeIntent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_RESPONSE_INLINE, messageInline)
        composeIntent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_ID, messageAddressId)
        composeIntent.putExtra(ComposeMessageActivity.EXTRA_VERIFY, true)

        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addParentStack(ComposeMessageActivity::class.java)
        stackBuilder.addNextIntent(composeIntent)
        val contentIntent = stackBuilder.getPendingIntent(
            0,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = createAccountChannel()

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(context.getString(R.string.verification_needed))
            .setContentText(
                String.format(context.getString(R.string.verification_needed_description_notification), messageSubject)
            )
            .setContentIntent(contentIntent)
            .setColor(ContextCompat.getColor(context, R.color.ocean_blue))
            .setStyle(inboxStyle)
            .setLights(lightIndicatorColor, NOTIFICATION_LIGHT_ON, NOTIFICATION_LIGHT_OFF)
            .setAutoCancel(true)

        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID_VERIFICATION, notification)
    }

    override fun createCheckingMailboxNotification(): Notification {
        val channelId = createOngoingOperationChannel()
        val notificationTitle = context.getString(R.string.checking_mailbox)
        return NotificationCompat.Builder(context, channelId)
            .setPriority(PRIORITY_MIN)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(notificationTitle)
            .build()
    }

    override fun notifyUserLoggedOut(user: User?) {
        val inboxStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(context.getString(R.string.logged_out))
            .bigText(context.getString(R.string.logged_out_description))
            .setSummaryText(user?.defaultEmail ?: context.getString(R.string.app_name))

        val channelId = createAccountChannel()

        val clickIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_LOGGED_OUT,
            Intent(), // empty action for now, just to dismiss notification
            0
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setColor(ContextCompat.getColor(context, R.color.ocean_blue))
            .setStyle(inboxStyle)
            .setLights(
                ContextCompat.getColor(context, R.color.light_indicator),
                NOTIFICATION_LIGHT_ON,
                NOTIFICATION_LIGHT_OFF
            )
            .setAutoCancel(true)
            .setContentIntent(clickIntent)

        notificationManager.notify(NOTIFICATION_ID_LOGGED_OUT, builder.build())
    }

    override fun notifyAboutAttachment(
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
            0
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
     * [notifySingleNewEmail] and [notifyMultipleUnreadEmail]
     *
     * @param user [User] for get some Notification's settings
     */
    private fun createGenericEmailNotification(
        user: User
    ): NotificationCompat.Builder {

        // Schedule a Wakelock
        // TODO by Davide Farella: Perhaps schedule with Work Manager?
        val alarmReceiver = AlarmReceiver()
        alarmReceiver.setAlarm(context, true)

        // Create NotificationChannel
        val channelId = createEmailsChannel()

        // Create Delete Intent
        val deleteIntent = Intent(context.getString(R.string.notification_action_delete))
            .putExtra(EXTRA_NOTIFICATION_DELETE_MESSAGE, NOTIFICATION_GROUP_ID_EMAIL)
        val deletePendingIntent =
            PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Set Notification's colors
        val mainColor = context.getColorCompat(R.color.ocean_blue)
        val lightColor = context.getColorCompat(R.color.light_indicator)

        // Create Notification's Builder with the prepared params
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setColor(mainColor)
            .setLights(lightColor, NOTIFICATION_LIGHT_ON, NOTIFICATION_LIGHT_OFF)
            .setAutoCancel(true)
            .setDeleteIntent(deletePendingIntent)
            .setPriority(PRIORITY_HIGH)

        // Set Notification visibility
        if (user.isNotificationVisibilityLockScreen) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        // Set Vibration
        if (
            user.notificationSetting == NOTIFICATION_SETTING_VIBRATE_ONLY ||
            user.notificationSetting == NOTIFICATION_SETTING_SOUND_VIBRATE
        ) {
            builder.setVibrate(longArrayOf(NOTIFICATION_VIBRATE_ON, NOTIFICATION_VIBRATE_OFF))
        }

        // Set Sound
        if (
            user.notificationSetting == NOTIFICATION_SETTING_SOUND_ONLY ||
            user.notificationSetting == NOTIFICATION_SETTING_SOUND_VIBRATE
        ) {
            val notificationSound = try {
                // TODO Make sure we have needed permissions and sound file can be read -
                //  I am not sure if this even does anything (Adam)
                // Asserting the user's ringtone is not null, otherwise an exception will be thrown
                // and so fallback to default ringtone
                user.ringtone?.also { ringtoneUri ->
                    context.contentResolver.openInputStream(ringtoneUri).use {
                        context.grantUriPermission(
                            "com.android.systemui", ringtoneUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }  ?: RingtoneManager.getDefaultUri(TYPE_NOTIFICATION)
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
    override fun notifySingleNewEmail(
        userManager: UserManager,
        user: User,
        message: Message?,
        messageId: String,
        notificationBody: String?,
        sender: String,
        primaryUser: Boolean
    ) {
        // Create content Intent for open MessageDetailsActivity
        val contentIntent = Intent(context, MessageDetailsActivity::class.java)
            .putExtra(MessageDetailsActivity.EXTRA_MESSAGE_ID, messageId)
            .putExtra(MessageDetailsActivity.EXTRA_TRANSIENT_MESSAGE, false)
            .putExtra(MessageDetailsActivity.EXTRA_MESSAGE_RECIPIENT_USERNAME, user.username)

        val stackBuilder = TaskStackBuilder.create(context)
            .addParentStack(MessageDetailsActivity::class.java)
            .addNextIntent(contentIntent)

        val contentPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        // Create Action Intent's
        val archiveIntent = context.buildArchiveIntent(messageId)
        val trashIntent = context.buildTrashIntent(messageId)
        val replyIntent = if (primaryUser)
            message?.let { context.buildReplyIntent(message, user, userManager) }
        else
            null

        // Create Notification Style
        val userDisplayName = user.defaultAddress?.displayName?.ifBlank { user.defaultAddress?.email }
        val inboxStyle = NotificationCompat.InboxStyle().run {
            setBigContentTitle(sender)
            setSummaryText(message?.toListString ?: userDisplayName ?: context.getString(R.string.app_name))
            notificationBody?.let { addLine(it) }
        }


        // Create Notification's Builder with the prepared params
        val builder = createGenericEmailNotification(user).apply {
            setContentTitle(sender)
            notificationBody?.let { setContentText(it) }
            setContentText(notificationBody)
            setContentIntent(contentPendingIntent)
            setStyle(inboxStyle)
            addAction(
                R.drawable.archive,
                context.getString(R.string.archive),
                archiveIntent
            )
            addAction(
                R.drawable.action_notification_trash,
                context.getString(R.string.trash),
                trashIntent
            )
            if (replyIntent != null)
                addAction(
                    R.drawable.action_notification_reply,
                    context.getString(R.string.reply),
                    replyIntent
                )
        }

        // Build the Notification
        val notification = builder.build()
        notificationManager.notify(user.username.hashCode(), notification)
    }

    /**
     * Show a Notification for MORE THAN ONE unread Emails. This will be called ONLY if there are
     * MORE than one unread Notifications
     *
     * @param loggedInUser current logged [User]
     * @param unreadNotifications [List] of [RoomNotification] to show to the user
     */
    override fun notifyMultipleUnreadEmail(
        userManager: UserManager,
        loggedInUser: User,
        unreadNotifications: List<RoomNotification>
    ) {
        val contentPendingIntent = getMailboxActivityIntent(userManager.username, loggedInUser.username)

        // Prepare Notification info
        val notificationTitle = context.getString(R.string.new_emails, unreadNotifications.size)

        // Create Notification Style
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(notificationTitle)
            .setSummaryText(loggedInUser.username)
        unreadNotifications.reversed().forEach { notification ->
            inboxStyle.addLine(
                createSpannableLine(
                    notification.notificationTitle,
                    notification.notificationBody
                )
            )
        }

        // Create Notification's Builder with the prepared params
        val builder = createGenericEmailNotification(loggedInUser)
            .setContentTitle(notificationTitle)
            .setContentIntent(contentPendingIntent)
            .setStyle(inboxStyle)

        // Build the Notification
        val notification = builder.build()

        notificationManager.notify(loggedInUser.username.hashCode(), notification)
    }

    private fun getMailboxActivityIntent(currentUserUsername: String, loggedInUser: String): PendingIntent {
        // Create content Intent for open MailboxActivity
        val contentIntent = Intent(context, MailboxActivity::class.java)
        if (currentUserUsername != loggedInUser) {
            contentIntent.putExtra(EXTRA_SWITCHED_USER, true)
            contentIntent.putExtra(EXTRA_SWITCHED_TO_USER, loggedInUser)
        }
        val requestCode = System.currentTimeMillis().toInt()
        return PendingIntent.getActivity(context, requestCode, contentIntent, 0)
    }

    /** @return [Spannable] a single line [Spannable] where [title] is [BOLD] */
    private fun createSpannableLine(title: String, body: String): Spannable {
        val spannableText = SpannableString("$title $body")
        spannableText.setSpan(StyleSpan(BOLD), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableText
    }

    private fun createSpannableBigText(
        sendingFailedNotifications: List<SendingFailedNotification>
    ): Spannable {
        val spannableStringBuilder = SpannableStringBuilder()
        sendingFailedNotifications.reversed().forEach { sendingFailedNotification ->
            spannableStringBuilder.append(
                createSpannableLine(
                    sendingFailedNotification.messageSubject ?: context.getString(R.string.message_failed),
                    sendingFailedNotification.errorMessage
                )
            )
                .append("\n")
        }
        return spannableStringBuilder.toSpannable()
    }

    private fun createGenericErrorSendingMessageNotification(
        username: String
    ): NotificationCompat.Builder {

        // Create channel and get id
        val channelId = createAccountChannel()

        // Create content Intent to open Drafts
        val contentIntent = Intent(context, MailboxActivity::class.java)
        contentIntent.putExtra(EXTRA_MAILBOX_LOCATION, Constants.MessageLocationType.DRAFT.messageLocationTypeValue)
        contentIntent.putExtra(EXTRA_USERNAME, username)

        val stackBuilder = TaskStackBuilder.create(context)
            .addParentStack(MailboxActivity::class.java)
            .addNextIntent(contentIntent)

        val contentPendingIntent = stackBuilder.getPendingIntent(
            username.hashCode() + NOTIFICATION_ID_SENDING_FAILED,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set Notification's colors
        val mainColor = context.getColorCompat(R.color.ocean_blue)
        val lightColor = context.getColorCompat(R.color.light_indicator)

        // Create notification builder
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentIntent(contentPendingIntent)
            .setColor(mainColor)
            .setLights(lightColor, NOTIFICATION_LIGHT_ON, NOTIFICATION_LIGHT_OFF)
            .setAutoCancel(true)
    }

    override fun notifySingleErrorSendingMessage(error: String, username: String) {
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(context.getString(R.string.message_failed))
            .setSummaryText(username)
            .bigText(error)

        val notificationBuilder = createGenericErrorSendingMessageNotification(username)
            .setStyle(bigTextStyle)

        val notification = notificationBuilder.build()
        notificationManager.notify(username.hashCode() + NOTIFICATION_ID_SENDING_FAILED, notification)
    }

    override fun notifyMultipleErrorSendingMessage(
        unreadSendingFailedNotifications: List<SendingFailedNotification>,
        user: User
    ) {

        val notificationTitle = context.getString(
            R.string.message_sending_failures,
            unreadSendingFailedNotifications.size
        )

        // Create Notification Style
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(notificationTitle)
            .setSummaryText(user.defaultEmail ?: user.username ?: context.getString(R.string.app_name))
            .bigText(createSpannableBigText(unreadSendingFailedNotifications))

        // Create notification builder
        val notificationBuilder = createGenericErrorSendingMessageNotification(user.username)
            .setStyle(bigTextStyle)

        // Build and show notification
        val notification = notificationBuilder.build()
        notificationManager.notify(user.username.hashCode() + NOTIFICATION_ID_SENDING_FAILED, notification)
    }

    override fun notifySaveDraftError(errorMessage: String, messageSubject: String?, username: String) {
        val title = context.getString(R.string.failed_saving_draft_online, messageSubject)

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .setSummaryText(username)
            .bigText(errorMessage)

        val notificationBuilder = createGenericErrorSendingMessageNotification(username)
            .setStyle(bigTextStyle)

        val notification = notificationBuilder.build()
        notificationManager.notify(username.hashCode() + NOTIFICATION_ID_SAVE_DRAFT_ERROR, notification)
    }

}
