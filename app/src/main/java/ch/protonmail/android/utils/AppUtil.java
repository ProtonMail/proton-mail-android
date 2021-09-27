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
package ch.protonmail.android.utils;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.birbit.android.jobqueue.JobManager;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import ch.protonmail.android.BuildConfig;
import ch.protonmail.android.activities.BaseActivity;
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabase;
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabaseFactory;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory;
import ch.protonmail.android.api.models.room.counters.CountersDatabase;
import ch.protonmail.android.api.models.room.counters.CountersDatabaseFactory;
import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.api.models.room.notifications.NotificationsDatabase;
import ch.protonmail.android.api.models.room.notifications.NotificationsDatabaseFactory;
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase;
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory;
import ch.protonmail.android.api.models.room.sendingFailedNotifications.SendingFailedNotificationsDatabase;
import ch.protonmail.android.api.models.room.sendingFailedNotifications.SendingFailedNotificationsDatabaseFactory;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.ApiOfflineEvent;
import ch.protonmail.android.events.ForceUpgradeEvent;
import ch.protonmail.android.storage.AttachmentClearingService;
import ch.protonmail.android.storage.MessageBodyClearingService;

import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_FORCE_UPGRADE;
import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_INVALID_APP_CODE;
import static ch.protonmail.android.core.Constants.RESPONSE_CODE_API_OFFLINE;
import static ch.protonmail.android.core.ProtonMailApplication.getApplication;
import static ch.protonmail.android.core.UserManagerKt.PREF_PIN;
import static ch.protonmail.android.prefs.SecureSharedPreferencesKt.PREF_SYMMETRIC_KEY;
import static ch.protonmail.android.servers.notification.NotificationServerKt.NOTIFICATION_ID_SENDING_FAILED;

public class AppUtil {

    private static final int BUFFER_SIZE = 4096;

    private AppUtil() {
    }

    public static boolean isDebug() {
        return (getApplication().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public static void postEventOnUi(final Object event) {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> ProtonMailApplication.getApplication().getBus().post(event));
    }

    public static byte[] getByteArray(@NonNull File file) throws IOException {

        final byte[] buffer = new byte[BUFFER_SIZE];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = new FileInputStream(file)) {
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }

        return out.toByteArray();
    }

    public static String buildUserAgent() {
        return String.format(Locale.US, "%s/%s (Android %s; %s %s)", "ProtonMail",
                BuildConfig.VERSION_NAME, Build.VERSION.RELEASE, Build.BRAND, Build.MODEL);
    }

    @Deprecated
    @kotlin.Deprecated(message = "Use 'BuildConfig.VERSION_NAME'")
    public static String getAppVersionName(Context context) {
        return BuildConfig.VERSION_NAME;
    }

    public static String getAppVersion() {
        return String.format("%s (%d) ", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
    }

    @Deprecated
    @kotlin.Deprecated(message = "Use 'BuildConfig.VERSION_CODE'")
    public static int getAppVersionCode(Context context) {
        return BuildConfig.VERSION_CODE;
    }

    public static File createTempFileFromInputStream(Context context, @NonNull InputStream in) throws IOException {
        File file;
        final byte[] buffer = new byte[BUFFER_SIZE];
        OutputStream out = null;

        try {
            file = File.createTempFile(DateUtil.generateTimestamp(), null, context.getCacheDir());
            out = new BufferedOutputStream(new FileOutputStream(file));
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();

            if (out != null) {
                out.close();
            }
        }

        return file;
    }

    public static void deleteDatabases(Context context, String username) {
        deleteDatabases(context, username, null, true);
    }

    public static void deleteDatabases(Context context, String username, boolean clearContacts) {
        deleteDatabases(context, username, null, clearContacts);
    }

    public static void deleteDatabases(Context context, String username, IDBClearDone clearDoneListener) {
        deleteDatabases(context, username, clearDoneListener, true);
    }

    private static void deleteDatabases(Context context, String username, IDBClearDone clearDoneListener, boolean clearContacts) {
        try {
            if (!TextUtils.isEmpty(username)) {
                clearStorage(ContactsDatabaseFactory.Companion.getInstance(context, username).getDatabase(),
                        MessagesDatabaseFactory.Companion.getInstance(context, username).getDatabase(),
                        MessagesDatabaseFactory.Companion.getSearchDatabase(context).getDatabase(),
                        NotificationsDatabaseFactory.Companion.getInstance(context, username).getDatabase(),
                        CountersDatabaseFactory.Companion.getInstance(context, username).getDatabase(),
                        AttachmentMetadataDatabaseFactory.Companion.getInstance(context, username).getDatabase(),
                        PendingActionsDatabaseFactory.Companion.getInstance(context, username).getDatabase(),
                        clearDoneListener, true, username, clearContacts);
            } else {
                clearStorage(ContactsDatabaseFactory.Companion.getInstance(context).getDatabase(),
                        MessagesDatabaseFactory.Companion.getInstance(context).getDatabase(),
                        MessagesDatabaseFactory.Companion.getSearchDatabase(context).getDatabase(),
                        NotificationsDatabaseFactory.Companion.getInstance(context).getDatabase(),
                        CountersDatabaseFactory.Companion.getInstance(context).getDatabase(),
                        AttachmentMetadataDatabaseFactory.Companion.getInstance(context).getDatabase(),
                        PendingActionsDatabaseFactory.Companion.getInstance(context).getDatabase(),
                        clearDoneListener, false, null, clearContacts);
            }
        } catch (Exception e) {
            Logger.doLogException(e);
        }
    }

    public static StringBuilder getExceptionStringBuilder(Throwable throwable) {
        StringBuilder exceptionStringBuilder = new StringBuilder();
        if (throwable != null) {
            exceptionStringBuilder.append(throwable.getMessage());
            exceptionStringBuilder.append("\n");
            exceptionStringBuilder.append(throwable.getCause());
            exceptionStringBuilder.append("\n");
            StackTraceElement[] stackTraceElements = throwable.getStackTrace();
            if (stackTraceElements != null) {
                for (StackTraceElement stackTraceElement : stackTraceElements) {
                    exceptionStringBuilder.append(stackTraceElement.toString());
                    exceptionStringBuilder.append("\n");
                }
            }
            exceptionStringBuilder.append(throwable.getStackTrace());
        }
        return exceptionStringBuilder;
    }

    public static boolean isAppInBackground() {
        return ProtonMailApplication.getApplication().isAppInBackground();
    }

    public static boolean isLockTaskModeRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return activityManager.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE;
        }
        // Deprecated in API level 23.
        return activityManager.isInLockTaskMode();
    }

    public static void clearNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        final NotificationsDatabase notificationsDatabase = NotificationsDatabaseFactory.Companion.getInstance(context).getDatabase();
        new ClearNotificationsFromDatabaseTask(notificationsDatabase).execute();
    }

    public static void clearNotifications(Context context, String username) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(username.hashCode());
        final NotificationsDatabase notificationsDatabase = NotificationsDatabaseFactory.Companion.getInstance(context).getDatabase();
        new ClearNotificationsFromDatabaseTask(notificationsDatabase).execute();
    }

    public static void clearNotifications(Context context, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    public static void clearSendingFailedNotifications(Context context, String username) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(username.hashCode() + NOTIFICATION_ID_SENDING_FAILED);
        final SendingFailedNotificationsDatabase sendingFailedNotificationsDatabase = SendingFailedNotificationsDatabaseFactory.Companion.getInstance(context).getDatabase();
        new ClearSendingFailedNotificationsFromDatabaseTask(sendingFailedNotificationsDatabase).execute();
    }

    /// read string from raw
    public static String readTxt(Context content, int id) {
        InputStream inputStream = content.getResources().openRawResource(id);
        System.out.println(inputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int i;
        try {
            i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return byteArrayOutputStream.toString();
    }

    public static Intent decorInAppIntent(Intent intent) {
        intent.putExtra(BaseActivity.EXTRA_IN_APP, true);
        return intent;
    }

    public static boolean checkForErrorCodes(int code, String message) {
        if (code == RESPONSE_CODE_INVALID_APP_CODE || code == RESPONSE_CODE_FORCE_UPGRADE) {
            AppUtil.postEventOnUi(new ForceUpgradeEvent(message));
            return true;
        } else if (code == RESPONSE_CODE_API_OFFLINE) {
            AppUtil.postEventOnUi(new ApiOfflineEvent(message));
            return true;
        }
        return false;
    }

    public static void clearTasks(final JobManager jobManager) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                jobManager.stop();
                jobManager.clear();
                return null;
            }
        }.execute();
    }

    public static void clearStorage(final ContactsDatabase contactsDatabase,
                                    final MessagesDatabase messagesDatabase,
                                    final MessagesDatabase searchDatabase,
                                    final NotificationsDatabase notificationsDatabase,
                                    final CountersDatabase countersDatabase,
                                    final AttachmentMetadataDatabase attachmentMetadataDatabase,
                                    final PendingActionsDatabase pendingActionsDatabase,
                                    final boolean clearContacts) {
        clearStorage(contactsDatabase, messagesDatabase, searchDatabase, notificationsDatabase, countersDatabase,
                attachmentMetadataDatabase, pendingActionsDatabase, null, false, null, clearContacts);
    }

    private static void clearStorage(final ContactsDatabase contactsDatabase,
                                     final MessagesDatabase messagesDatabase,
                                     final MessagesDatabase searchDatabase,
                                     final NotificationsDatabase notificationsDatabase,
                                     final CountersDatabase countersDatabase,
                                     final AttachmentMetadataDatabase attachmentMetadataDatabase,
                                     final PendingActionsDatabase pendingActionsDatabase,
                                     final IDBClearDone clearDone,
                                     final boolean deleteTables,
                                     final String username,
                                     final boolean clearContacts) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                pendingActionsDatabase.clearPendingSendCache();
                pendingActionsDatabase.clearPendingUploadCache();
                if (clearContacts) {
                    contactsDatabase.clearContactEmailsLabelsJoin();
                    contactsDatabase.clearContactEmailsCacheBlocking();
                    contactsDatabase.clearContactDataCache();
                    contactsDatabase.clearContactGroupsLabelsTableBlocking();
                    contactsDatabase.clearFullContactDetailsCache();
                }
                messagesDatabase.clearMessagesCache();
                messagesDatabase.clearAttachmentsCache();
                messagesDatabase.clearLabelsCache();
                searchDatabase.clearMessagesCache();
                searchDatabase.clearAttachmentsCache();
                searchDatabase.clearLabelsCache();
                notificationsDatabase.clearNotificationCache();
                countersDatabase.clearUnreadLocationsTable();
                countersDatabase.clearUnreadLabelsTable();
                countersDatabase.clearTotalLocationsTable();
                countersDatabase.clearTotalLabelsTable();
                attachmentMetadataDatabase.clearAttachmentMetadataCache();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                // TODO: test this in future and uncomment
//                if (deleteTables) {
//                    AttachmentClearingService.startClearUpImmediatelyServiceAndDeleteTables(username);
//                } else {
                AttachmentClearingService.startClearUpImmediatelyService();
//                }
                if (clearDone != null) {
                    clearDone.onDatabaseClearingCompleted();
                }
                MessageBodyClearingService.Companion.startClearUpService();
            }
        }.execute();
    }

    /**
     * Deletes global Shared Preferences and preserves some important values.
     */
    public static void deletePrefs() {
        SharedPreferences defaultSharedPrefs = ProtonMailApplication.getApplication().getDefaultSharedPreferences();

        String sekrit = defaultSharedPrefs.getString(PREF_SYMMETRIC_KEY, null);
        defaultSharedPrefs.edit().clear().apply();
        defaultSharedPrefs.edit().putString(PREF_SYMMETRIC_KEY, sekrit).apply();
    }

    /**
     * Deletes all backup Shared Preferences.
     */
    public static void deleteBackupPrefs() {
        SharedPreferences backupSharedPrefs = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences globalSecureSharedPreferences = ProtonMailApplication.getApplication().getSecureSharedPreferences();
        globalSecureSharedPreferences.edit().remove(PREF_PIN).apply();
        backupSharedPrefs.edit().clear().apply();
    }

    /**
     * Deletes user's Secure Shared Preferences and preserves some important values.
     */
    public static void deleteSecurePrefs(@NonNull String username, boolean deletePin) {
        SharedPreferences secureSharedPrefs = ProtonMailApplication.getApplication().getSecureSharedPreferences(username);
        if (!deletePin) {
            String mailboxPin = secureSharedPrefs.getString(PREF_PIN, null);
            secureSharedPrefs.edit().clear().apply();
            secureSharedPrefs.edit().putString(PREF_PIN, mailboxPin).apply();
        } else {
            secureSharedPrefs.edit().clear().apply();

        }
    }

    // TODO: Rewrite with coroutines after the whole AppUtil file is converted to Kotlin
    private static class ClearNotificationsFromDatabaseTask extends AsyncTask<Void, Void, Void> {
        private final NotificationsDatabase notificationsDatabase;

        ClearNotificationsFromDatabaseTask(
                NotificationsDatabase notificationsDatabase) {
            this.notificationsDatabase = notificationsDatabase;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            notificationsDatabase.clearNotificationCache();
            return null;
        }
    }

    // TODO: Rewrite with coroutines after the whole AppUtil file is converted to Kotlin
    private static class ClearSendingFailedNotificationsFromDatabaseTask extends AsyncTask<Void, Void, Void> {
        private final SendingFailedNotificationsDatabase sendingFailedNotificationsDatabase;

        ClearSendingFailedNotificationsFromDatabaseTask(SendingFailedNotificationsDatabase sendingFailedNotificationsDatabase) {
            this.sendingFailedNotificationsDatabase = sendingFailedNotificationsDatabase;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            sendingFailedNotificationsDatabase.clearSendingFailedNotifications();
            return null;

        }
    }

    public interface IDBClearDone {
        void onDatabaseClearingCompleted();
    }

}
