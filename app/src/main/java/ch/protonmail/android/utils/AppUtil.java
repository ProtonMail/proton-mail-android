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
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.data.local.AttachmentMetadataDao;
import ch.protonmail.android.data.local.AttachmentMetadataDatabase;
import ch.protonmail.android.data.local.ContactDao;
import ch.protonmail.android.data.local.ContactDatabase;
import ch.protonmail.android.data.local.MessageDao;
import ch.protonmail.android.data.local.MessageDatabase;
import ch.protonmail.android.data.local.NotificationDao;
import ch.protonmail.android.data.local.NotificationDatabase;
import ch.protonmail.android.data.local.PendingActionDao;
import ch.protonmail.android.data.local.PendingActionDatabase;
import ch.protonmail.android.data.local.SendingFailedNotificationDao;
import ch.protonmail.android.data.local.SendingFailedNotificationDatabase;
import ch.protonmail.android.domain.entity.Id;
import ch.protonmail.android.events.ApiOfflineEvent;
import ch.protonmail.android.events.ForceUpgradeEvent;
import ch.protonmail.android.storage.AttachmentClearingService;
import ch.protonmail.android.storage.MessageBodyClearingService;
import timber.log.Timber;

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
                clearStorage(ContactDatabase.Companion.getInstance(context, username).getDao(),
                        MessageDatabase.Companion.getInstance(context, username).getDao(),
                        MessageDatabase.Companion.getSearchDatabase(context).getDao(),
                        NotificationDatabase.Companion.getInstance(context, username).getDao(),
                        CounterDatabase.Companion.getInstance(context, username).getDao(),
                        AttachmentMetadataDatabase.Companion.getInstance(context, username).getDao(),
                        PendingActionDatabase.Companion.getInstance(context, username).getDao(),
                        clearDoneListener, clearContacts);
            } else {
                clearStorage(ContactDatabase.Companion.getInstance(context).getDao(),
                        MessageDatabase.Companion.getInstance(context).getDao(),
                        MessageDatabase.Companion.getSearchDatabase(context).getDao(),
                        NotificationDatabase.Companion.getInstance(context).getDao(),
                        CounterDatabase.Companion.getInstance(context).getDao(),
                        AttachmentMetadataDatabase.Companion.getInstance(context).getDao(),
                        PendingActionDatabase.Companion.getInstance(context).getDao(),
                        clearDoneListener, clearContacts);
            }
        } catch (Exception e) {
            Timber.e(e);
            if (clearDoneListener != null)
                clearDoneListener.onDatabaseClearingCompleted();
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

    @Deprecated // Use with User Id
    public static void clearNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        final UserManager userManager = ((ProtonMailApplication) context.getApplicationContext()).getUserManager();
        final Id userId = userManager.requireCurrentUserId();
        final NotificationDao notificationDao = NotificationDatabase.Companion
                .getInstance(context, userId)
                .getDao();
        new ClearNotificationsFromDatabaseTask(notificationDao).execute();
    }

    public static void clearNotifications(Context context, Id userId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(userId.hashCode());
        final NotificationDao notificationDao = NotificationDatabase.Companion
                .getInstance(context, userId)
                .getDao();
        new ClearNotificationsFromDatabaseTask(notificationDao).execute();
    }

    public static void clearNotifications(Context context, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    public static void clearSendingFailedNotifications(Context context, Id userId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(userId.hashCode() + NOTIFICATION_ID_SENDING_FAILED);
        final SendingFailedNotificationDao sendingFailedNotificationDao = SendingFailedNotificationDatabase.Companion
                .getInstance(context, userId)
                .getDao();
        new ClearSendingFailedNotificationsFromDatabaseTask(sendingFailedNotificationDao).execute();
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

    public static void clearStorage(
            final ContactDao contactDao,
            final MessageDao messageDao,
            final MessageDao searchDatabase,
            final NotificationDao notificationDao,
            final CounterDao counterDao,
            final AttachmentMetadataDao attachmentMetadataDao,
            final PendingActionDao pendingActionDao,
            final boolean clearContacts
    ) {
        clearStorage(contactDao, messageDao, searchDatabase, notificationDao, counterDao,
                attachmentMetadataDao, pendingActionDao, null, clearContacts);
    }

    private static void clearStorage(
            final ContactDao contactDao,
            final MessageDao messageDao,
            final MessageDao searchDatabase,
            final NotificationDao notificationDao,
            final CounterDao counterDao,
            final AttachmentMetadataDao attachmentMetadataDao,
            final PendingActionDao pendingActionDao,
            final IDBClearDone clearDone,
            final boolean clearContacts
    ) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                pendingActionDao.clearPendingSendCache();
                pendingActionDao.clearPendingUploadCache();
                if (clearContacts) {
                    contactDao.clearContactEmailsLabelsJoin();
                    contactDao.clearContactEmailsCacheBlocking();
                    contactDao.clearContactDataCache();
                    contactDao.clearContactGroupsLabelsTableBlocking();
                    contactDao.clearFullContactDetailsCache();
                }
                messageDao.clearMessagesCache();
                messageDao.clearAttachmentsCache();
                messageDao.clearLabelsCache();
                searchDatabase.clearMessagesCache();
                searchDatabase.clearAttachmentsCache();
                searchDatabase.clearLabelsCache();
                notificationDao.clearNotificationCache();
                counterDao.clearUnreadLocationsTable();
                counterDao.clearUnreadLabelsTable();
                counterDao.clearTotalLocationsTable();
                counterDao.clearTotalLabelsTable();
                attachmentMetadataDao.clearAttachmentMetadataCache();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                AttachmentClearingService.startClearUpImmediatelyService();
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
    public static void deleteSecurePrefs(
            @NonNull SharedPreferences userPreferences,
            boolean deletePin
    ) {
        String mailboxPinBackup = userPreferences.getString(PREF_PIN, null);
        SharedPreferences.Editor editor = userPreferences.edit()
                .clear();
        if (!deletePin) {
            editor.putString(PREF_PIN, mailboxPinBackup);
        }
        editor.apply();
    }

    /**
     * Deletes user's Secure Shared Preferences and preserves some important values.
     */
    @Deprecated
    @kotlin.Deprecated(message = "Use with SharedPreferences directly")
    public static void deleteSecurePrefs(@NonNull String username, boolean deletePin) {
        throw new UnsupportedOperationException("Use with SharedPreferences directly");
    }

    // TODO: Rewrite with coroutines after the whole AppUtil file is converted to Kotlin
    private static class ClearNotificationsFromDatabaseTask extends AsyncTask<Void, Void, Void> {
        private final NotificationDao notificationDao;

        ClearNotificationsFromDatabaseTask(
                NotificationDao notificationDao) {
            this.notificationDao = notificationDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            notificationDao.clearNotificationCache();
            return null;
        }
    }

    // TODO: Rewrite with coroutines after the whole AppUtil file is converted to Kotlin
    private static class ClearSendingFailedNotificationsFromDatabaseTask extends AsyncTask<Void, Void, Void> {
        private final SendingFailedNotificationDao sendingFailedNotificationDao;

        ClearSendingFailedNotificationsFromDatabaseTask(SendingFailedNotificationDao sendingFailedNotificationDao) {
            this.sendingFailedNotificationDao = sendingFailedNotificationDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            sendingFailedNotificationDao.clearSendingFailedNotifications();
            return null;

        }
    }

    public interface IDBClearDone {
        void onDatabaseClearingCompleted();
    }

}
