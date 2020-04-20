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
package ch.protonmail.android.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import ch.protonmail.android.BuildConfig;
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository;
import ch.protonmail.android.api.ProtonMailApi;
import ch.protonmail.android.api.local.SnoozeSettings;
import ch.protonmail.android.api.models.DatabaseProvider;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.messages.receive.MessageResponse;
import ch.protonmail.android.api.models.messages.receive.MessagesResponse;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.api.models.room.notifications.Notification;
import ch.protonmail.android.api.models.room.notifications.NotificationsDatabase;
import ch.protonmail.android.api.segments.event.AlarmReceiver;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.QueueNetworkUtil;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.gcm.models.NotificationData;
import ch.protonmail.android.gcm.models.NotificationEncryptedData;
import ch.protonmail.android.gcm.models.NotificationSender;
import ch.protonmail.android.servers.notification.INotificationServer;
import ch.protonmail.android.servers.notification.NotificationServer;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.crypto.Crypto;
import ch.protonmail.android.utils.crypto.TextCiphertext;
import ch.protonmail.android.utils.crypto.TextDecryptionResult;
import io.sentry.Sentry;
import io.sentry.event.EventBuilder;

public class GcmIntentService extends IntentService {

    private static final String TAG_GCM_INTENT_SERVICE = "GcmIntentService";

    public static final String EXTRA_READ = "CMD_READ";
    private static final String EXTRA_ENCRYPTED_DATA = "encryptedMessage";
    private static final String EXTRA_UID = "UID";

    @Inject
    ProtonMailApi mApi;
    @Inject
    UserManager mUserManager;
    @Inject
    QueueNetworkUtil mNetworkUtils;
    @Inject
    MessageDetailsRepository messageDetailsRepository;
    @Inject
    DatabaseProvider databaseProvider;

    private NotificationsDatabase notificationsDatabase;
    private INotificationServer notificationServer;

    public GcmIntentService() {
        super("GCM");
        setIntentRedelivery(true);
        ProtonMailApplication.getApplication().getAppComponent().inject(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationServer = new NotificationServer(this, notificationManager);
    }

    private void startMeForeground() {
        final int messageId = (int) System.currentTimeMillis();
        final android.app.Notification notification = notificationServer.createCheckingMailboxNotification();
        startForeground(messageId, notification);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        startMeForeground();

        final Bundle extras = intent.getExtras();
        if (extras != null && !extras.isEmpty()) {
            if (!extras.containsKey("CMD")) {
                // we are always registering for push in MailboxActivity

                boolean isAppInBackground = AppUtil.isAppInBackground();
                if (!isAppInBackground) {
                    AlarmReceiver alarmReceiver = new AlarmReceiver();
                    alarmReceiver.setAlarm(this, true);
                }

                mNetworkUtils.setCurrentlyHasConnectivity(true);
                NotificationData notificationData = null;
                NotificationEncryptedData messageData = null;
                String sessionId = "";

                if (extras.containsKey(EXTRA_UID)) {
                    sessionId = extras.getString(EXTRA_UID, "");
                }

                String notificationUsername = mUserManager.getUsernameBySessionId(sessionId);
                if (TextUtils.isEmpty(notificationUsername)) {
                    // we do not show notifications for unknown/inactive users
                    return;
                }

                User user = mUserManager.getUser(notificationUsername);
                notificationsDatabase = databaseProvider.provideNotificationsDao(notificationUsername);
                if (!user.isBackgroundSync()) {
                    return;
                }

                try {
                    if (extras.containsKey(EXTRA_ENCRYPTED_DATA)) {
                        String encryptedStr = extras.getString(EXTRA_ENCRYPTED_DATA);
                        Crypto crypto = Crypto.forUser(mUserManager, notificationUsername);
                        TextDecryptionResult textDecryptionResult = crypto.decryptForUser(TextCiphertext.fromArmor(encryptedStr), notificationUsername);
                        String decryptedStr = textDecryptionResult.getDecryptedData();
                        notificationData = tryParseNotificationModel(decryptedStr);
                        messageData = notificationData.getData();
                    }
                } catch (Exception e) {
                    // can not deliver notification
                    if (!BuildConfig.DEBUG) {
                        EventBuilder eventBuilder = new EventBuilder().withTag("GCM_MU", TextUtils.isEmpty(notificationUsername) ? "EMPTY" : "NOT_EMPTY");
                        Sentry.capture(eventBuilder);
                        Sentry.capture(e);
                    }
                }

                if (notificationData == null || messageData == null) {
                    return;
                }

                final String messageId = messageData.getMessageId();
                final String notificationBody = messageData.getBody();
                NotificationSender notificationSender = messageData.getSender();
                String sender = notificationSender.getSenderName();
                if (TextUtils.isEmpty(sender)) {
                    sender = notificationSender.getSenderEmail();
                }
                boolean primaryUser = mUserManager.getUsername().equals(notificationUsername);
                if (extras.containsKey(EXTRA_READ) && extras.getBoolean(EXTRA_READ)) {
                    removeNotification(user, messageId, primaryUser);
                    return;
                }

                boolean isQuickSnoozeEnabled = mUserManager.isSnoozeQuickEnabled();
                boolean isScheduledSnoozeEnabled = mUserManager.isSnoozeScheduledEnabled();
                if (!isQuickSnoozeEnabled && (!isScheduledSnoozeEnabled || shouldShowNotificationWhileScheduledSnooze(user))) {
                    sendNotification(user, messageId, notificationBody, sender, primaryUser);
                }
            }
        }
        stopForeground(true);
    }

    /**
     * Remove the Notification with the given id and eventually recreate a notification with other
     * unread Notifications from database
     *
     * @param user      current logged {@link User}
     * @param messageId String id of {@link Message} for delete relative {@link Notification}
     */
    private void removeNotification(final User user,
                                    final String messageId,
                                    final boolean primaryUser) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Cancel all the Status Bar Notifications
        notificationManager.cancelAll();

        // Remove the Notification from Database
        notificationsDatabase.deleteByMessageId(messageId);
        List<Notification> notifications = notificationsDatabase.findAllNotifications();

        // Return if there are no more unreadNotifications
        if (notifications.isEmpty()) return;

        Message message = fetchMessage(user, messageId);
        if (notifications.size() > 1) {
            notificationServer.notifyMultipleUnreadEmail(mUserManager, user, notifications);
        } else {
            Notification notification = notifications.get(0);
            notificationServer.notifySingleNewEmail(
                    mUserManager, user, message, messageId,
                    notification.getNotificationBody(),
                    notification.getNotificationTitle(), primaryUser
            );
        }
    }

    /**
     * Show a Notification for a new email received.
     *
     * @param user             current logged {@link User}
     * @param messageId        String id for retrieve the {@link Message} details
     * @param notificationBody String body of the Notification
     * @param sender           String name of the sender of the email
     */
    private void sendNotification(
            final User user,
            final String messageId,
            @Nullable final String notificationBody,
            final String sender,
            final boolean primaryUser
    ) {

        // Insert current Notification in Database
        Notification notification = new Notification(messageId, sender, notificationBody != null ? notificationBody : "");
        notificationsDatabase.insertNotification(notification);

        List<Notification> notifications = notificationsDatabase.findAllNotifications();

        Message message = fetchMessage(user, messageId);
        if (notifications.size() > 1) {
            notificationServer.notifyMultipleUnreadEmail(mUserManager, user, notifications);
        } else {
            notificationServer.notifySingleNewEmail(
                    mUserManager, user, message, messageId, notificationBody, sender, primaryUser
            );
        }
    }

    private Message fetchMessage(final User user, final String messageId) {
        // Fetch message details if required by the current config
        boolean fetchMessageDetails = user.isGcmDownloadMessageDetails();
        Message message;
        if (fetchMessageDetails) {
            message = fetchMessageDetails(messageId);
        } else {
            message = fetchMessageMetadata(messageId);
        }
        if (message == null) {
            // try to find the message in the local storage, maybe it was received from the event
            message = messageDetailsRepository.findMessageById(messageId);
        }

        return message;
    }

    private Message fetchMessageMetadata(final String messageId) {
        Message message = null;
        try {
            MessagesResponse messageResponse = mApi.fetchSingleMessageMetadata(messageId);
            if (messageResponse != null) {
                List<Message> messages = messageResponse.getMessages();
                if (messages.size() > 0) {
                    message = messages.get(0);
                }
                if (message != null) {
                    Message savedMessage = messageDetailsRepository.findMessageById(message.getMessageId());
                    if (savedMessage != null) {
                        message.setInline(savedMessage.isInline());
                    }
                    message.setDownloaded(false);
                    messageDetailsRepository.saveMessageInDB(message);
                } else {
                    // check if the message is already in local store
                    message = messageDetailsRepository.findMessageById(messageId);
                }
            }
        } catch (Exception error) {
            Logger.doLogException(TAG_GCM_INTENT_SERVICE, "error while fetching message detail", error);
        }
        return message;
    }

    private Message fetchMessageDetails(final String messageId) {
        Message message = null;
        try {
            MessageResponse messageResponse = mApi.messageDetail(messageId);
            message = messageResponse.getMessage();
            Message savedMessage = messageDetailsRepository.findMessageById(messageId);
            if (savedMessage != null) {
                message.setInline(savedMessage.isInline());
            }
            message.setDownloaded(true);
            messageDetailsRepository.saveMessageInDB(message);
        } catch (Exception error) {
            Logger.doLogException(TAG_GCM_INTENT_SERVICE, "error while fetching message detail", error);
        }

        return message;
    }

    private boolean shouldShowNotificationWhileScheduledSnooze(User user) {
        Calendar rightNow = Calendar.getInstance();
        SnoozeSettings snoozeSettings = mUserManager.getSnoozeSettings();
        return !snoozeSettings.shouldSuppressNotification(rightNow);
    }

    private NotificationData tryParseNotificationModel(String decryptedStr) {
        Gson gson = new Gson();
        return gson.fromJson(decryptedStr, NotificationData.class);
    }
}
