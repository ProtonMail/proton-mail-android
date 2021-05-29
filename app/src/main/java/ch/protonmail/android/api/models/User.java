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
package ch.protonmail.android.api.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.protonmail.android.R;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.mapper.bridge.UserBridgeMapper;
import timber.log.Timber;

import static ch.protonmail.android.core.Constants.LogTags.SENDING_FAILED_REASON_TAG;
import static ch.protonmail.android.core.Constants.LogTags.SENDING_FAILED_TAG;
import static ch.protonmail.android.core.Constants.Prefs.PREF_ADDRESS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_ADDRESS_ID;
import static ch.protonmail.android.core.Constants.Prefs.PREF_ALIASES;
import static ch.protonmail.android.core.Constants.Prefs.PREF_ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES;
import static ch.protonmail.android.core.Constants.Prefs.PREF_AUTO_LOCK_PIN_PERIOD;
import static ch.protonmail.android.core.Constants.Prefs.PREF_AUTO_LOGOUT;
import static ch.protonmail.android.core.Constants.Prefs.PREF_BACKGROUND_SYNC;
import static ch.protonmail.android.core.Constants.Prefs.PREF_BLOCK_SWIPING_GESTURES;
import static ch.protonmail.android.core.Constants.Prefs.PREF_COMBINED_CONTACTS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_DELINQUENT;
import static ch.protonmail.android.core.Constants.Prefs.PREF_DISPLAY_MOBILE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_DISPLAY_NAME;
import static ch.protonmail.android.core.Constants.Prefs.PREF_DISPLAY_SIGNATURE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_GCM_DOWNLOAD_MESSAGE_DETAILS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_KEYS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_LAST_INTERACTION;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MANUALLY_LOCKED;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MAX_ATTACHMENT_STORAGE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MAX_SPACE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MAX_UPLOAD_FILE_SIZE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MOBILE_SIGNATURE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_NOTIFICATION;
import static ch.protonmail.android.core.Constants.Prefs.PREF_NOTIFICATION_VISIBILITY_LOCK_SCREEN;
import static ch.protonmail.android.core.Constants.Prefs.PREF_NUM_MESSAGE_PER_PAGE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_PREVENT_TAKING_SCREENSHOTS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_RINGTONE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_ROLE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_SIGNATURE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_SUBSCRIBED;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USED_SPACE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_CREDIT;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_CURRENCY;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_ID;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_LEGACY_ACCOUNT;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_NAME;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_PRIVATE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_SERVICES;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USE_FINGERPRINT;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USE_PIN;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USING_REGULAR_API;


public class User {

    private final static String GENERIC_DEPRECATION_MESSAGE = "Use from new User entity " +
            "'ch.protonmail.android.domain.entity.user.User'\n" +
            "Replacements options are:\n" +
            "-  'loadNew' instead of  'load'\n" +
            "- manual mapping with help of  'UserBridgeMapper'.\n" +
            "- function 'toNewUser' is available when proper DI is not possible";

    @SerializedName(Fields.User.NAME)
    private String name;
    @SerializedName(Fields.User.USED_SPACE)
    private long usedSpace;
    @SerializedName(Fields.User.MAX_SPACE)
    private long maxSpace;
    @SerializedName(Fields.User.MAX_UPLOAD)
    private int maxUpload;
    @SerializedName(Fields.User.ROLE)
    private int role;
    @SerializedName(Fields.User.SUBSCRIBED)
    private int subscribed;
    @SerializedName(Fields.User.DELINQUENT)
    private int delinquent;
    @SerializedName(Fields.User.KEYS)
    private List<Keys> keys;

    private String NotificationEmail; // used only for Memento, real settings are in UserSettings
    private int SwipeLeft; // used only for Memento, real settings are in MailSettings
    private int SwipeRight;

    private String Signature;

    private String MobileSignature;
    private boolean ShowMobileSignature = true;
    private boolean ShowSignature = false;
    private String DisplayName;
    private int NumMessagePerPage;

    private List<Address> Addresses;
    private String AddressId;
    private String DefaultAddress;

    // new
    @SerializedName(Fields.User.ID)
    private String id;
    @SerializedName(Fields.User.CURRENCY)
    private String currency;
    @SerializedName(Fields.User.CREDIT)
    private int credit;
    @SerializedName(Fields.User.PRIVATE)
    private int isPrivate;
    @SerializedName(Fields.User.SERVICES)
    private int services;

    // region these are local only - do not touch them
    private boolean AutoLogout; // this can remain here, local only setting
    private int AutoLockPINPeriod = -1; // this can remain here, local only setting
    private long LastInteraction; // this can remain here, local only setting

    /**
     * TODO use enum. Return value is not clear at all!
     * This should represent the type of the notification:
     * * no interruption
     * * only sound
     * * only vibration
     * * sound and vibration
     */
    private int NotificationSetting = -1; // this can remain here, local only setting
    private boolean BackgroundSync = true; // this can remain here, local only setting
    private int NotificationVisibilityLockScreen = -1; // this can remain here, local only setting
    private int PreventTakingScreenshots = 0; // this can remain here, local only setting
    private boolean GcmDownloadMessageDetails; // this can remain here, local only setting
    private boolean UsePin; // this can remain here, local only setting
    private boolean UseFingerprint; // this can remain here, local only setting
    private Uri ringtone; // this can remain here, local only setting
    private int MaxAttachmentStorage; // this can remain here, local only setting
    private boolean ManuallyLocked; // this can remain here, local only setting
    private String username; // this can remain here, local only setting
    private boolean CombinedContacts; // this can remain here, local only setting
    private boolean BlockSwipingGestures;
    private boolean isLegacyAccount;
    // endregion

    @NonNull
    public static User load(String username) {
        final SharedPreferences securePrefs = ProtonMailApplication.getApplication().getSecureSharedPreferences(username);
        final User user = new User();
        user.name = securePrefs.getString(PREF_USER_NAME, "");
        if (!TextUtils.isEmpty(username)) {
            user.username = username;
        }
        user.usedSpace = securePrefs.getLong(PREF_USED_SPACE, 0L);
        user.Signature = securePrefs.getString(PREF_SIGNATURE, "");
        user.role = securePrefs.getInt(PREF_ROLE, 0);
        user.subscribed = securePrefs.getInt(PREF_SUBSCRIBED, 0);
        if (!user.isPaidUserSignatureEdit()) {
            user.MobileSignature = ProtonMailApplication.getApplication().getString(R.string.default_mobile_signature);
        } else {
            user.MobileSignature = securePrefs.getString(PREF_MOBILE_SIGNATURE, ProtonMailApplication.getApplication().getString(R.string.default_mobile_signature));
        }
        user.ShowMobileSignature = securePrefs.getBoolean(PREF_DISPLAY_MOBILE, true);
        if (!user.ShowMobileSignature && !user.isPaidUserSignatureEdit()) {
            user.ShowMobileSignature = true;
            user.setShowMobileSignature(true);
        }
        user.ShowSignature = securePrefs.getBoolean(PREF_DISPLAY_SIGNATURE, false);
        user.DisplayName = securePrefs.getString(PREF_DISPLAY_NAME, "");
        user.maxSpace = securePrefs.getLong(PREF_MAX_SPACE, 0L);
        user.maxUpload = securePrefs.getInt(PREF_MAX_UPLOAD_FILE_SIZE, 0);
        user.NumMessagePerPage = securePrefs.getInt(PREF_NUM_MESSAGE_PER_PAGE, 0);
        user.AddressId = securePrefs.getString(PREF_ADDRESS_ID, "");
        user.DefaultAddress = securePrefs.getString(PREF_ADDRESS, "");
        user.Addresses = deserializeAddresses(securePrefs.getString(PREF_ALIASES, ""));
        user.keys = deserializeKeys(securePrefs.getString(PREF_KEYS, ""));
        user.NotificationSetting = user.loadNotificationSettingsFromBackup();
        user.BlockSwipingGestures = securePrefs.getBoolean(PREF_BLOCK_SWIPING_GESTURES, false);
        user.BackgroundSync = securePrefs.getBoolean(PREF_BACKGROUND_SYNC, true);
        user.PreventTakingScreenshots = securePrefs.getInt(PREF_PREVENT_TAKING_SCREENSHOTS, 0);
        user.GcmDownloadMessageDetails = securePrefs.getBoolean(PREF_GCM_DOWNLOAD_MESSAGE_DETAILS, false);
        user.delinquent = securePrefs.getInt(PREF_DELINQUENT, 0);
        user.NotificationVisibilityLockScreen = user.loadNotificationVisibilityLockScreenSettingsFromBackup();
        int maxAttachmentStorage = securePrefs.getInt(PREF_MAX_ATTACHMENT_STORAGE, Constants.MIN_ATTACHMENT_STORAGE_IN_MB);
        if (maxAttachmentStorage <= 0) {
            maxAttachmentStorage = Constants.MIN_ATTACHMENT_STORAGE_IN_MB; // defaulting to the min allowed att storage
        }
        user.MaxAttachmentStorage = maxAttachmentStorage;
        user.AutoLogout = user.loadAutoLogoutFromBackup();
        user.AutoLockPINPeriod = user.loadAutoLockPINPeriodFromBackup();
        user.UsePin = user.loadUsePinFromBackup();
        user.UseFingerprint = user.loadUseFingerprintFromBackup();
        user.CombinedContacts = user.loadCombinedContactsFromBackup();
        user.LastInteraction = user.loadLastInteractionFromBackup();
        String notificationRingtone = user.loadRingtoneBackup();
        if (!TextUtils.isEmpty(notificationRingtone)) {
            user.ringtone = Uri.parse(notificationRingtone);
        }
        user.ManuallyLocked = securePrefs.getBoolean(PREF_MANUALLY_LOCKED, false);

        user.id = securePrefs.getString(PREF_USER_ID, "id");
        user.currency = securePrefs.getString(PREF_USER_CURRENCY, "eur");
        user.credit = securePrefs.getInt(PREF_USER_CREDIT, 0);
        user.isPrivate = securePrefs.getInt(PREF_USER_PRIVATE, 0);
        user.services = securePrefs.getInt(PREF_USER_SERVICES, 0);
        user.isLegacyAccount = securePrefs.getBoolean(PREF_USER_LEGACY_ACCOUNT, true);

        return user;
    }

    /**
     * Returns the new User entity.
     * It relies on old load mechanism for load User, and then map it to new entity.
     * This is supposed to be replaced by extracting outside of this class, and locate into a proper
     * place
     *
     * @param username of the User intended to load
     * @return new {@link ch.protonmail.android.domain.entity.user.User}
     */
    public ch.protonmail.android.domain.entity.user.User loadNew(String username) {
        return load(username).toNewUser();
    }

    public void save() {
        final SharedPreferences pref;
        if (!TextUtils.isEmpty(this.username)) {
            pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
        } else {
            pref = ProtonMailApplication.getApplication().getSecureSharedPreferences();
        }
        Log.d("PMTAG", "saving User for username: `" + username + "`");

        if (NotificationSetting == -1) {
            NotificationSetting = loadNotificationSettingsFromBackup();
        } else {
            saveNotificationSettingsBackup();
        }


        String uriRingtone = loadRingtoneBackup();
        if (!TextUtils.isEmpty(uriRingtone)) {
            ringtone = Uri.parse(uriRingtone);
        }
        AutoLogout = loadAutoLogoutFromBackup();
        AutoLockPINPeriod = loadAutoLockPINPeriodFromBackup();
        UsePin = loadUsePinFromBackup();
        UseFingerprint = loadUseFingerprintFromBackup();
        LastInteraction = loadLastInteractionFromBackup();
        BackgroundSync = loadBackgroundSyncSetting();
        GcmDownloadMessageDetails = loadGcmDownloadMessageDetailsSetting();
        CombinedContacts = loadCombinedContactsFromBackup();
        isLegacyAccount = loadUserAccountLegacy();

        if (NotificationVisibilityLockScreen == -1) {
            NotificationVisibilityLockScreen = loadNotificationVisibilityLockScreenSettingsFromBackup();
        }

        if (MobileSignature == null) {
            MobileSignature = pref.getString(PREF_MOBILE_SIGNATURE, ProtonMailApplication.getApplication().getString(R.string.default_mobile_signature));
        }

        ShowSignature = loadShowSignatureSetting();
        ShowMobileSignature = loadShowMobileSignatureSetting();

        BlockSwipingGestures = loadBlockSwipingGestures();

        pref.edit()
                .putLong(PREF_USED_SPACE, usedSpace)
                .putString(PREF_SIGNATURE, Signature)
                .putString(PREF_MOBILE_SIGNATURE, MobileSignature)
                .putBoolean(PREF_DISPLAY_MOBILE, ShowMobileSignature)
                .putBoolean(PREF_DISPLAY_SIGNATURE, ShowSignature)
                .putBoolean(PREF_BLOCK_SWIPING_GESTURES, BlockSwipingGestures)
                .putString(PREF_DISPLAY_NAME, DisplayName)
                .putLong(PREF_MAX_SPACE, maxSpace)
                .putInt(PREF_MAX_UPLOAD_FILE_SIZE, maxUpload)
                .putInt(PREF_NUM_MESSAGE_PER_PAGE, NumMessagePerPage)
                .putString(PREF_ADDRESS_ID, AddressId)
                .putString(PREF_ADDRESS, DefaultAddress)
                .putString(PREF_ALIASES, serializeAddresses())
                .putString(PREF_KEYS, serializeKeys())
                .putBoolean(PREF_BACKGROUND_SYNC, BackgroundSync)
                .putInt(PREF_PREVENT_TAKING_SCREENSHOTS, PreventTakingScreenshots)
                .putBoolean(PREF_GCM_DOWNLOAD_MESSAGE_DETAILS, GcmDownloadMessageDetails)
                .putInt(PREF_ROLE, role)
                .putInt(PREF_DELINQUENT, delinquent)
                .putInt(PREF_SUBSCRIBED, subscribed)
                .putBoolean(PREF_AUTO_LOGOUT, AutoLogout)
                .putBoolean(PREF_MANUALLY_LOCKED, ManuallyLocked)
                .putInt(PREF_MAX_ATTACHMENT_STORAGE, MaxAttachmentStorage)
                .putBoolean(PREF_COMBINED_CONTACTS, CombinedContacts)
                .putString(PREF_USER_ID, id)
                .putString(PREF_USER_CURRENCY, currency)
                .putInt(PREF_USER_CREDIT, credit)
                .putInt(PREF_USER_PRIVATE, isPrivate)
                .putInt(PREF_USER_SERVICES, services)
                .putBoolean(PREF_USER_LEGACY_ACCOUNT, isLegacyAccount)
                .apply();
    }

    private void saveShowSignatureSetting() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
        pref.edit().putBoolean(PREF_DISPLAY_SIGNATURE, ShowSignature).apply();
    }

    private boolean loadShowSignatureSetting() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
        return pref.getBoolean(PREF_DISPLAY_SIGNATURE, false);
    }

    private void saveShowMobileSignatureSetting() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
        pref.edit().putBoolean(PREF_DISPLAY_MOBILE, ShowMobileSignature).apply();
    }

    private boolean loadShowMobileSignatureSetting() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
        return pref.getBoolean(PREF_DISPLAY_MOBILE, true);
    }

    private void saveGcmDownloadMessageDetailsSetting() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
        pref.edit().putBoolean(PREF_GCM_DOWNLOAD_MESSAGE_DETAILS, GcmDownloadMessageDetails).apply();
    }

    private boolean loadGcmDownloadMessageDetailsSetting() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
        return pref.getBoolean(PREF_GCM_DOWNLOAD_MESSAGE_DETAILS, false);
    }

    private void saveBackgroundSyncSetting() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
        pref.edit().putBoolean(PREF_BACKGROUND_SYNC, BackgroundSync).apply();
    }

    private boolean loadBackgroundSyncSetting() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
        return pref.getBoolean(PREF_BACKGROUND_SYNC, true);
    }

    private void savePreventTakingScreenshotsSetting() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
        pref.edit().putInt(PREF_PREVENT_TAKING_SCREENSHOTS, 0).apply();
    }

    private int loadPreventTakingScreenshotsSetting() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
        return pref.getInt(PREF_PREVENT_TAKING_SCREENSHOTS, 0);
    }

    public void setAllowSecureConnectionsViaThirdParties(boolean allowSecureConnectionsViaThirdParties) {
        SharedPreferences secureSharedPreferences = ProtonMailApplication.getApplication().getSecureSharedPreferences();
        secureSharedPreferences.edit().putBoolean(PREF_ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES, allowSecureConnectionsViaThirdParties).apply();
    }

    public boolean getAllowSecureConnectionsViaThirdParties() {
        SharedPreferences secureSharedPreferences = ProtonMailApplication.getApplication().getSecureSharedPreferences();
        return secureSharedPreferences.getBoolean(PREF_ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES, true); // automatic opt-in for users
    }

    /**
     * Using default proton api (not proxy).
     * @param useDefaultApi boolean
     */
    public void setUsingDefaultApi(boolean useDefaultApi) {
        SharedPreferences secureSharedPreferences = ProtonMailApplication.getApplication().getDefaultSharedPreferences();
        secureSharedPreferences.edit().putBoolean(PREF_USING_REGULAR_API, useDefaultApi).apply();
    }

    public boolean getUsingDefaultApi() {
        SharedPreferences sharedPreferences = ProtonMailApplication.getApplication().getDefaultSharedPreferences();
        return sharedPreferences.getBoolean(PREF_USING_REGULAR_API, true); // false);
    }

    public void saveNotificationSettingsBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putInt(PREF_NOTIFICATION, NotificationSetting).apply();
    }

    private int loadNotificationSettingsFromBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getInt(PREF_NOTIFICATION, 3);
    }

    public void saveBlockSwipingGestures() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putBoolean(PREF_BLOCK_SWIPING_GESTURES, BlockSwipingGestures).apply();
    }

    private boolean loadBlockSwipingGestures() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(PREF_BLOCK_SWIPING_GESTURES, false);
    }

    public void saveNotificationVisibilityLockScreenSettingsBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putInt(PREF_NOTIFICATION_VISIBILITY_LOCK_SCREEN, NotificationVisibilityLockScreen).apply();
    }


    private int loadNotificationVisibilityLockScreenSettingsFromBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getInt(PREF_NOTIFICATION_VISIBILITY_LOCK_SCREEN, -1);
    }

    public void saveAutoLogoutBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putBoolean(PREF_AUTO_LOGOUT, AutoLogout).apply();
    }

    public void saveAutoLockPINPeriodBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putInt(PREF_AUTO_LOCK_PIN_PERIOD, AutoLockPINPeriod).apply();
    }

    public void saveUsePinBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putBoolean(PREF_USE_PIN, UsePin).apply();
    }

    public void saveUseFingerprintBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putBoolean(PREF_USE_FINGERPRINT, UseFingerprint).apply();
    }

    private void saveLastInteractionBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putLong(PREF_LAST_INTERACTION, LastInteraction).apply();
    }

    private long loadLastInteractionFromBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getLong(PREF_LAST_INTERACTION, SystemClock.elapsedRealtime());
    }

    private boolean loadAutoLogoutFromBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(PREF_AUTO_LOGOUT, false);
    }

    private int loadAutoLockPINPeriodFromBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getInt(PREF_AUTO_LOCK_PIN_PERIOD, -1);
    }

    private boolean loadUsePinFromBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(PREF_USE_PIN, false);
    }

    private boolean loadUseFingerprintFromBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(PREF_USE_FINGERPRINT, false);
    }

    public void saveRingtoneBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putString(PREF_RINGTONE, ringtone != null ? ringtone.toString() : null).apply();
    }

    private String loadRingtoneBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getString(PREF_RINGTONE, null);
    }

    public void saveCombinedContactsBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putBoolean(PREF_COMBINED_CONTACTS, CombinedContacts).apply();
    }

    private boolean loadCombinedContactsFromBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(PREF_COMBINED_CONTACTS, false);
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public long getUsedSpace() {
        return usedSpace;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public int getRole() {
        return role;
    }

    public boolean isPaidUserSignatureEdit() {
        boolean allowMobileSignatureEdit = ProtonMailApplication.getApplication().getResources().getBoolean(R.bool.allow_mobile_signature_edit);
        return allowMobileSignatureEdit || role > 0;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public int getPrivate() {
        return isPrivate;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE + "\nnew name:  'plans'")
    public int getSubscribed() {
        return subscribed;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE + "\nnew name:  'plans'")
    public int getServices() {
        return services;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE + "\nnew name:  'plans'")
    public boolean isPaidUser() {
        return subscribed > 0;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public int getCredit() {
        return credit;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getCurrency() {
        return currency;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public int getDelinquentValue() {
        return delinquent;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'delinquent.mailRoutesAccessible'")
    public boolean getDelinquent() {
        return delinquent >= 3;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE + "\new name:  'totalUploadLimit'")
    public int getMaxUpload() {
        return maxUpload;
    }

    public void setAndSaveUsedSpace(long usedSpace) {
        if (this.usedSpace != usedSpace) {
            this.usedSpace = usedSpace;
            final SharedPreferences pref;
            if (!TextUtils.isEmpty(this.username)) {
                pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
            } else {
                pref = ProtonMailApplication.getApplication().getSecureSharedPreferences();
            }
            Log.d("PMTAG", "setAndSaveUsedSpace for username: `" + username + "`");
            pref.edit().putLong(PREF_USED_SPACE, this.usedSpace).apply();
        }
    }

    // TODO use enum. Return value is not clear at all!
    public int getNotificationSetting() {
        return (NotificationSetting == -1) ? 3 : NotificationSetting;
    }

    public void setNotificationSetting(int setting) {
        this.NotificationSetting = setting;
        saveNotificationSettingsBackup();
    }

    public String getSignature() {
        return Signature;
    }

    public String getSignatureForAddress(String addressId) {
        tryLoadAddresses();
        for (Address address : Addresses) {
            if (address.getID().equals(addressId)) {
                return address.getSignature();
            }
        }

        return Signature == null ? "" : Signature;
    }

    public String getMobileSignature() {
        return MobileSignature == null ? "" : MobileSignature;
    }

    public void setMobileSignature(String mobileSignature) {
        MobileSignature = mobileSignature;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.primary?.id' for primary address id")
    public String getAddressId() {
        tryLoadAddresses();
        if (Addresses != null && Addresses.size() > 0) {
            AddressId = Addresses.get(0).getID();
        }
        return AddressId;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\n from:  'addresses.primary'")
    public Address getDefaultAddress() {
        tryLoadAddresses();
        Address defaultAddress = Addresses.get(0);
        for (Address address : Addresses) {
            if (address.getOrder() == 1) {
                defaultAddress = address;
                break;
            }
        }
        return defaultAddress;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.primary?.email'")
    public String getDefaultEmail() {
        if (TextUtils.isEmpty(DefaultAddress)) {
            tryLoadAddresses();
            Address alias = getDefaultAddress();
            DefaultAddress = alias.getEmail();
            save();
        }
        return DefaultAddress;
    }

    private void tryLoadAddresses() {
        if (Addresses == null || Addresses.size() == 0) {
            final SharedPreferences securePrefs;
            if (!TextUtils.isEmpty(this.username)) {
                securePrefs = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);
            } else {
                securePrefs = ProtonMailApplication.getApplication().getSecureSharedPreferences();
            }
            Addresses = deserializeAddresses(securePrefs.getString(PREF_ALIASES, ""));

            // TODO try to verify and decrypt private key here?
        }
        sortAddresses();
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public CopyOnWriteArrayList<Address> getAddresses() {
        if (Addresses == null || Addresses.size() == 0) {
            tryLoadAddresses();
        } else {
            sortAddresses();
        }
        return new CopyOnWriteArrayList<>(Addresses);
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'newUser.findAddressById(addressId) }'")
    public Address getAddressById(String addressId) {
        tryLoadAddresses();
        String addrId = addressId;
        if (TextUtils.isEmpty(addressId)) {
            addrId = DefaultAddress;
        }
        if (Addresses == null || Addresses.size() == 0) {
            Timber.e(SENDING_FAILED_REASON_TAG, "Addresses is null -> " + (Addresses == null) + "or empty");
            return null;
        }
        if (TextUtils.isEmpty(addrId)) {
            return Addresses.get(0);
        }
        for (Address address : Addresses) {
            if (address.getID().equals(addrId)) {
                return address;
            }
        }

        Timber.e(SENDING_FAILED_TAG, "addressID not found");
        return null;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public List<Keys> getKeys() {
        return (keys != null) ? keys : new ArrayList<>();
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.values.first { it.enabled && it.allowedToSend }?.id'")
    public int getAddressByIdFromOnlySendAddresses() {
        int result = 0;
        tryLoadAddresses();
        List<Address> senderOnlyAddresses = getSenderOnlyAddresses();
        for (int i = 0; i < senderOnlyAddresses.size(); i++) {
            Address address = senderOnlyAddresses.get(i);
            if (address.getSend() == 1) {
                result = i;
                break;
            }
        }
        return result;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.values\n" +
            "  .filter { it.enabled && it.allowedToReceive }\n" +
            "  .map { it.email }'")
    @NonNull
    public List<String> getSenderEmailAddresses() {
        List<String> result = new ArrayList<>();
        tryLoadAddresses();
        for (Address address : Addresses) {
            if (address.getStatus() == 1 && address.getReceive() == 1) {
                result.add(address.getEmail());
            }
        }
        return result;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.values.filter { it.enabled && it.allowedToReceive }")
    private List<Address> getSenderAddresses() {
        List<Address> result = new ArrayList<>();
        tryLoadAddresses();
        for (Address address : Addresses) {
            if (address.getStatus() == 1 && address.getReceive() == 1) {
                result.add(address);
            }
        }
        return result;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.values.filter { it.enabled }")
    private List<Address> getSenderOnlyAddresses() {
        List<Address> result = new ArrayList<>();
        tryLoadAddresses();
        for (Address address : Addresses) {
            if (address.getStatus() == 1) {
                result.add(address);
            }
        }
        return result;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.find { (_, v) -> v.enabled }.key")
    public int getPositionByAddressId(String addressId) {
        int result = 0;
        List<Address> senderAddresses = getSenderAddresses();
        for (int i = 0; i < senderAddresses.size(); i++) {
            Address address = senderAddresses.get(i);
            if (address.getStatus() == 1 && address.getReceive() == 1 && address.getID().equals(addressId)) {
                result = i;
                break;
            }
        }
        return result;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.values.find { it.email == email }.email")
    public String getSenderAddressIdByEmail(String email) {
        String result = null;
        tryLoadAddresses();
        for (Address address : Addresses) {
            if (address.getEmail().equals(email)) {
                result = address.getID();
                break;
            }
        }
        return result;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.values.find { it.email == email }.displayName")
    public String getSenderAddressNameByEmail(String email) {
        String result = null;
        tryLoadAddresses();
        for (Address address : Addresses) {
            if (address.getEmail().equals(email)) {
                result = address.getDisplayName();
                break;
            }
        }
        return result;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.find { (_, v) -> v.email == email }.key")
    public int getAddressOrderByAddress(Address address) {
        int result = 0;
        for (int i = 0; i < Addresses.size(); i++) {
            Address otherAddress = Addresses.get(i);
            if (address.getID().equals(otherAddress.getID())) {
                result = i;
                break;
            }
        }
        return result;
    }

    public String setAddressesOrder(int newDefault) {
        CopyOnWriteArrayList<Address> newAddresses = new CopyOnWriteArrayList<>();
        Address newDefaultAddress = Addresses.get(newDefault);
        Address currentDefaultAddress = getDefaultAddress();
        int currentDefaultAliasNewOrder = newDefaultAddress.getOrder();
        newDefaultAddress.setOrder(1);
        currentDefaultAddress.setOrder(currentDefaultAliasNewOrder);
        newAddresses.add(newDefaultAddress);

        for (int i = 0; i < Addresses.size(); i++) {
            if (i != newDefault) {
                newAddresses.add(Addresses.get(i));
            }
        }
        Addresses = newAddresses;
        AddressId = Addresses.get(0).getID();
        DefaultAddress = Addresses.get(0).getEmail();
        DisplayName = Addresses.get(0).getDisplayName();
        return AddressId;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getId() {
        return id;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getName() {
        return name;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getDisplayName() {
        return DisplayName;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.values.find { it.id == addressId }?.displayName'")
    @Nullable
    public String getDisplayNameForAddress(String addressId) {
        tryLoadAddresses();
        for (Address address : Addresses) {
            if (address.getID().equals(addressId) && !TextUtils.isEmpty(address.getDisplayName())) {
                return address.getDisplayName();
            }
        }
        return DisplayName;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public long getMaxSpace() {
        return maxSpace;
    }

    public void setAddressIdEmail() {
        if (Addresses != null && Addresses.size() > 0) {
            AddressId = Addresses.get(0).getID();
            DefaultAddress = Addresses.get(0).getEmail();
        }
    }

    private String serializeAddresses() {
        Gson gson = new Gson();
        return gson.toJson(Addresses);
    }

    private static CopyOnWriteArrayList<Address> deserializeAddresses(String serialized) {
        CopyOnWriteArrayList<Address> result = new CopyOnWriteArrayList<>();
        if (serialized.isEmpty()) {
            return result;
        }
        Gson gson = new Gson();
        Address[] out = gson.fromJson(serialized, Address[].class);
        if (out != null) {
            result = new CopyOnWriteArrayList<>(Arrays.asList(out));
        }
        return result;
    }

    private String serializeKeys() {
        Gson gson = new Gson();
        return gson.toJson(keys);
    }

    private static List<Keys> deserializeKeys(String serialized) {
        if (serialized.isEmpty())
            return new ArrayList<>();
        Gson gson = new Gson();
        Keys[] out = gson.fromJson(serialized, Keys[].class);
        return Arrays.asList(out);
    }

    public void setDisplayName(String displayName) {
        DisplayName = displayName;
    }

    public boolean isShowMobileSignature() {
        return ShowMobileSignature;
    }

    public void setShowMobileSignature(boolean showMobileSignature) {
        ShowMobileSignature = showMobileSignature;
        saveShowMobileSignatureSetting();
    }

    public boolean isShowSignature() {
        return ShowSignature;
    }

    public void setSignature(String signature) {
        Signature = signature;
    }

    public void setShowSignature(boolean showSignature) {
        ShowSignature = showSignature;
        saveShowSignatureSetting();
    }

    public boolean isBackgroundSync() {
        return BackgroundSync;
    }

    public void setBackgroundSync(boolean backgroundSync) {
        BackgroundSync = backgroundSync;
        saveBackgroundSyncSetting();
    }

    public int getAutoLockPINPeriod() {
        AutoLockPINPeriod = loadAutoLockPINPeriodFromBackup();
        return AutoLockPINPeriod;
    }

    public void setAutoLockPINPeriod(int autoLockPINPeriod) {
        AutoLockPINPeriod = autoLockPINPeriod;
        saveAutoLockPINPeriodBackup();
    }

    public boolean isUsePin() {
        UsePin = loadUsePinFromBackup();
        return UsePin;
    }

    public void setUsePin(boolean usePin) {
        UsePin = usePin;
        saveUsePinBackup();
    }

    public void setUseFingerprint(boolean useFingerprint) {
        UseFingerprint = useFingerprint;
        saveUseFingerprintBackup();
    }

    public boolean isUseFingerprint() {
        return loadUseFingerprintFromBackup();
    }

    public int getMaxAttachmentStorage() {
        return MaxAttachmentStorage;
    }

    public void setMaxAttachmentStorage(int maxAttachmentStorage) {
        MaxAttachmentStorage = maxAttachmentStorage;
    }

    public boolean shouldPINLockTheApp(long diff) {
        int option = AutoLockPINPeriod;
        int autoLockTimePeriod = AutoLockPINPeriod == -1 ? Integer.MAX_VALUE : ProtonMailApplication.getApplication().getResources().getIntArray(R.array.auto_logout_values)[option];
        return isUsePin() && (ManuallyLocked || (diff > autoLockTimePeriod)) && ProtonMailApplication.getApplication().getUserManager().getMailboxPin() != null;
    }

    public long getLastInteractionDiff() {
        if (LastInteraction == 0) {
            LastInteraction = loadLastInteractionFromBackup();
        }
        return SystemClock.elapsedRealtime() - LastInteraction;
    }

    public void setLastInteraction(long lastInteraction) {
        LastInteraction = lastInteraction;
        saveLastInteractionBackup();
    }

    public boolean isNotificationVisibilityLockScreen() {
        return loadNotificationVisibilityLockScreenSettingsFromBackup() == 1;
    }

    public void setNotificationVisibilityLockScreen(boolean notificationVisibilityLockScreen) {
        NotificationVisibilityLockScreen = notificationVisibilityLockScreen ? 1 : 0;
        saveNotificationVisibilityLockScreenSettingsBackup();
    }

    public boolean isPreventTakingScreenshots() {
        return PreventTakingScreenshots == 1;
    }

    public void setPreventTakingScreenshots(boolean preventTakingScreenshots) {
        PreventTakingScreenshots = preventTakingScreenshots ? 1 : 0;
        savePreventTakingScreenshotsSetting();
    }

    public boolean isBlockSwipingGestures() { return BlockSwipingGestures; }

    public void setBlockSwipingGestures(boolean blockSwipingGestures) {
        BlockSwipingGestures = blockSwipingGestures;
        saveBlockSwipingGestures();
    }

    public boolean isGcmDownloadMessageDetails() {
        return GcmDownloadMessageDetails;
    }

    public void setGcmDownloadMessageDetails(boolean gcmDownloadMessageDetails) {
        GcmDownloadMessageDetails = gcmDownloadMessageDetails;
        saveGcmDownloadMessageDetailsSetting();
    }

    public void setCombinedContacts(boolean combineContacts) {
        this.CombinedContacts = combineContacts;
        saveCombinedContactsBackup();
    }

    public boolean getCombinedContacts() {
        return loadCombinedContactsFromBackup();
    }

    @Nullable
    public Uri getRingtone() {

        // patch for FileUriExposedException
        if (ringtone != null && "file".equals(ringtone.getScheme())) {
            return null;
        }

        return ringtone;
    }

    public void setRingtone(Uri ringtone) {
        this.ringtone = ringtone;
        saveRingtoneBackup();
    }

    public void setManuallyLocked(boolean manuallyLocked) {
        ManuallyLocked = manuallyLocked;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getUsername() {
        return username;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nfrom:  'addresses.sorted()'")
    private void sortAddresses() {
        List<Address> addresses = new ArrayList<>(Addresses);
        Collections.sort(addresses, (o1, o2) -> Integer.compare(o1.getOrder(), o2.getOrder()));
        Addresses = new CopyOnWriteArrayList<>(addresses);
        if (Addresses.size() > 0) {
            Address firstAddress = Addresses.get(0);
            if (firstAddress != null) {
                DisplayName = firstAddress.getDisplayName();
            }
        }
    }

    public void setAddresses(List<Address> addresses) {
        this.Addresses = new CopyOnWriteArrayList<>(addresses);
        sortAddresses();
        save();
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public long getMaxAllowedAttachmentSpace() {
        return MaxAttachmentStorage; // return the value in bytes
    }

    public void setUsername(@NonNull String username) {
        if (!TextUtils.isEmpty(username)) {
            this.username = username;
        }
    }

    public void setLegacyAccount (boolean legacy) {
        isLegacyAccount = legacy;
        saveUserAccountLegacy(legacy);
    }

    public boolean getLegacyAccount() {
        return loadUserAccountLegacy();
    }

    private void saveUserAccountLegacy(boolean isUserLegacy) {
        SharedPreferences defaultSharedPreferences = ProtonMailApplication.getApplication().getDefaultSharedPreferences();
        defaultSharedPreferences.edit().putBoolean(PREF_USER_LEGACY_ACCOUNT, isUserLegacy).apply();
    }

    private boolean loadUserAccountLegacy() {
        SharedPreferences defaultSharedPreferences = ProtonMailApplication.getApplication().getDefaultSharedPreferences();
        return defaultSharedPreferences.getBoolean(PREF_USER_LEGACY_ACCOUNT, true);
    }

    /**
     * Convert this User to new User entity
     * @return {@link ch.protonmail.android.domain.entity.user.User}
     */
    public ch.protonmail.android.domain.entity.user.User toNewUser() {
        return UserBridgeMapper.buildDefault().toNewModel(this);
    }
}

