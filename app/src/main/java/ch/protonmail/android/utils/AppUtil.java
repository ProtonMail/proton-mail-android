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
package ch.protonmail.android.utils;

import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_FORCE_UPGRADE;
import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_INVALID_APP_CODE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_APP_VERSION;
import static ch.protonmail.android.core.Constants.Prefs.PREF_EXISTING_USER_ONBOARDING_SHOWN;
import static ch.protonmail.android.core.Constants.Prefs.PREF_NEW_USER_ONBOARDING_SHOWN;
import static ch.protonmail.android.core.Constants.Prefs.PREF_PREVIOUS_APP_VERSION;
import static ch.protonmail.android.core.Constants.RESPONSE_CODE_API_OFFLINE;
import static ch.protonmail.android.core.ProtonMailApplication.getApplication;
import static ch.protonmail.android.core.UserManagerKt.PREF_PIN;
import static ch.protonmail.android.prefs.SecureSharedPreferencesKt.PREF_SYMMETRIC_KEY;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

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
import ch.protonmail.android.data.local.AttachmentMetadataDao;
import ch.protonmail.android.data.local.AttachmentMetadataDatabase;
import ch.protonmail.android.data.local.ContactDao;
import ch.protonmail.android.data.local.ContactDatabase;
import ch.protonmail.android.data.local.MessageDao;
import ch.protonmail.android.data.local.MessageDatabase;
import ch.protonmail.android.events.ApiOfflineEvent;
import ch.protonmail.android.events.ForceUpgradeEvent;
import ch.protonmail.android.mailbox.data.local.ConversationDao;
import ch.protonmail.android.pendingaction.data.PendingActionDao;
import ch.protonmail.android.pendingaction.data.PendingActionDatabase;
import ch.protonmail.android.storage.AttachmentClearingService;
import ch.protonmail.android.storage.MessageBodyClearingService;
import me.proton.core.domain.entity.UserId;
import timber.log.Timber;

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
        byte[] fileContent;
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            fileContent = out.toByteArray();
        }

        return fileContent;
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
        FileOutputStream fileOutputStream = null;

        try {
            file = File.createTempFile(DateUtil.generateTimestamp(), null, context.getCacheDir());
            fileOutputStream = new FileOutputStream(file);
            out = new BufferedOutputStream(fileOutputStream);
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();

            if (out != null) {
                out.close();
            }

            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }

        return file;
    }

    @Deprecated // Use ClearUserData use case
    public static void deleteDatabases(Context context, UserId userId) {
        deleteDatabases(context, userId, null, true);
    }

    @Deprecated // Use ClearUserData use case
    public static void deleteDatabases(Context context, UserId userId, boolean clearContacts) {
        deleteDatabases(context, userId, null, clearContacts);
    }

    @Deprecated // Use ClearUserData use case
    public static void deleteDatabases(Context context, UserId userId, IDBClearDone clearDoneListener) {
        deleteDatabases(context, userId, clearDoneListener, true);
    }

    @Deprecated // Use ClearUserData use case
    private static void deleteDatabases(Context context, UserId userId, IDBClearDone clearDoneListener, boolean clearContacts) {
        try {
            clearStorage(
                    context,
                    userId,
                    ContactDatabase.Companion.getInstance(context, userId).getDao(),
                    MessageDatabase.Factory.getInstance(context, userId).getDao(),
                    MessageDatabase.Factory.getInstance(context, userId).getConversationDao(),
                    AttachmentMetadataDatabase.Companion.getInstance(context, userId).getDao(),
                    PendingActionDatabase.Companion.getInstance(context, userId).getDao(),
                    clearDoneListener, clearContacts
            );
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

    @Deprecated // Use plain Intent without this method
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

    @Deprecated // Use ClearUserData use case
    public static void clearStorage(
            final Context context,
            final UserId userId,
            final ContactDao contactDao,
            final MessageDao messageDao,
            final MessageDao searchDatabase,
            final ConversationDao conversationDao,
            final AttachmentMetadataDao attachmentMetadataDao,
            final PendingActionDao pendingActionDao,
            final boolean clearContacts
    ) {
        clearStorage(
                context,
                userId,
                contactDao,
                messageDao,
                conversationDao,
                attachmentMetadataDao,
                pendingActionDao,
                null,
                clearContacts
        );
    }

    @Deprecated // Use ClearUserData use case
    private static void clearStorage(
            final Context context,
            final UserId userId,
            final ContactDao contactDao,
            final MessageDao messageDao,
            final ConversationDao conversationDao,
            final AttachmentMetadataDao attachmentMetadataDao,
            final PendingActionDao pendingActionDao,
            final IDBClearDone clearDone,
            final boolean clearContacts
    ) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                AttachmentClearingService.startClearUpImmediatelyService(context, userId);
                pendingActionDao.clearPendingSendCache();
                pendingActionDao.clearPendingUploadCache();
                if (clearContacts) {
                    contactDao.clearContactEmailsCache();
                    contactDao.clearContactDataCache();
                    contactDao.clearFullContactDetailsCache();
                }
                messageDao.clearMessagesCache();
                messageDao.clearAttachmentsCache();
                conversationDao.clear();
                attachmentMetadataDao.clearAttachmentMetadataCache();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
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
        SharedPreferences defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(ProtonMailApplication.getApplication());

        String sekrit = defaultSharedPrefs.getString(PREF_SYMMETRIC_KEY, null);
        int previousAppVersion = defaultSharedPrefs.getInt(PREF_PREVIOUS_APP_VERSION, Integer.MIN_VALUE);
        int appVersion = defaultSharedPrefs.getInt(PREF_APP_VERSION, Integer.MIN_VALUE);
        boolean isNewUserOnboardingShown = defaultSharedPrefs
                .getBoolean(PREF_NEW_USER_ONBOARDING_SHOWN, false);
        boolean isExistingUserOnboardingShown = defaultSharedPrefs
                .getBoolean(PREF_EXISTING_USER_ONBOARDING_SHOWN, false);
        defaultSharedPrefs.edit().clear().apply();
        defaultSharedPrefs.edit()
                .putString(PREF_SYMMETRIC_KEY, sekrit)
                .putInt(PREF_APP_VERSION, appVersion)
                .putInt(PREF_PREVIOUS_APP_VERSION, previousAppVersion)
                .putBoolean(PREF_NEW_USER_ONBOARDING_SHOWN, isNewUserOnboardingShown)
                .putBoolean(PREF_EXISTING_USER_ONBOARDING_SHOWN, isExistingUserOnboardingShown)
                .apply();
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

    public interface IDBClearDone {
        void onDatabaseClearingCompleted();
    }

}
