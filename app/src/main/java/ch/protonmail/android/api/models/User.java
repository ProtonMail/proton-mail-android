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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import arrow.core.Either;
import arrow.core.Either.Left;
import arrow.core.Either.Right;

import java.util.ArrayList;
import java.util.List;

import ch.protonmail.android.R;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.domain.entity.Id;
import ch.protonmail.android.feature.user.UserManagerKt;
import ch.protonmail.android.mapper.bridge.UserBridgeMapper;
import ch.protonmail.android.prefs.SecureSharedPreferences;
import ch.protonmail.android.usecase.LoadUser;
import me.proton.core.user.domain.UserManager;
import timber.log.Timber;

import static ch.protonmail.android.core.Constants.Prefs.PREF_ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES;
import static ch.protonmail.android.core.Constants.Prefs.PREF_AUTO_LOCK_PIN_PERIOD;
import static ch.protonmail.android.core.Constants.Prefs.PREF_BACKGROUND_SYNC;
import static ch.protonmail.android.core.Constants.Prefs.PREF_COMBINED_CONTACTS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_DISPLAY_MOBILE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_DISPLAY_SIGNATURE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_GCM_DOWNLOAD_MESSAGE_DETAILS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_LAST_INTERACTION;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MANUALLY_LOCKED;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MAX_ATTACHMENT_STORAGE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MOBILE_SIGNATURE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_NOTIFICATION;
import static ch.protonmail.android.core.Constants.Prefs.PREF_NOTIFICATION_VISIBILITY_LOCK_SCREEN;
import static ch.protonmail.android.core.Constants.Prefs.PREF_PREVENT_TAKING_SCREENSHOTS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_RINGTONE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USED_SPACE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USE_FINGERPRINT;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USE_PIN;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USING_REGULAR_API;

public class User {
    private String id;
    private String name;
    private String displayName;
    private long usedSpace;
    private long maxSpace;
    private int maxUpload;
    private int role;
    private int subscribed;
    private int delinquent;
    private String currency;
    private int credit;
    private int isPrivate;
    private int services;

    private List<Keys> keys;
    private List<Address> addresses;
    private String defaultAddressId;
    private String defaultAddressEmail;

    private String MobileSignature;
    private boolean ShowMobileSignature = true;
    private boolean ShowSignature = false;

    // new

    // region these are local only - do not touch them
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
    // endregion

    @NonNull
    @Deprecated
    @kotlin.Deprecated(message = "Use usecase/LoadLegacyUser")
    public static Either<LoadUser.Error, User> load(Id userId, Context context, UserManager userManager) {
        final SharedPreferences securePrefs = SecureSharedPreferences.Companion.getPrefsForUser(context, userId);

        if (securePrefs.getAll().isEmpty()) {
            return new Left(LoadUser.Error.NoPreferencesStored.INSTANCE);
        }

        // Note: Core UserManager (UserRepository & UserAddressRepository) have a memory cache.
        // Get User/Keys from Core.
        me.proton.core.user.domain.entity.User coreUser = UserManagerKt.getUserBlocking(userManager, userId);
        List<Keys> keys = UserManagerKt.getLegacyKeysBlocking(userManager, userId);

        // Get Primary/Addresses from Core.
        me.proton.core.user.domain.entity.UserAddress primaryAddress = UserManagerKt.getPrimaryAddressBlocking(userManager, userId);
        List<Address> addresses = UserManagerKt.getLegacyAddressesBlocking(userManager, userId);

        final User user = new User();
        user.id = coreUser.getUserId().getId();
        user.name = coreUser.getName();
        user.displayName = coreUser.getDisplayName();
        user.username = user.name;
        user.usedSpace = coreUser.getUsedSpace();
        user.role = coreUser.getRole().getValue();
        user.subscribed = coreUser.getSubscribed();
        user.currency = coreUser.getCurrency();
        user.credit = coreUser.getCredit();
        user.delinquent = coreUser.getDelinquent().getValue();
        user.isPrivate = coreUser.getPrivate() ? 1 : 0;
        user.services = coreUser.getServices();
        user.maxSpace = coreUser.getMaxSpace();
        user.maxUpload = Long.valueOf(coreUser.getMaxUpload()).intValue();

        user.keys = keys;

        user.defaultAddressId = primaryAddress.getAddressId().getId();
        user.defaultAddressEmail = primaryAddress.getEmail();
        user.addresses = addresses;

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
        user.NotificationSetting = user.loadNotificationSettingsFromBackup();
        user.BackgroundSync = securePrefs.getBoolean(PREF_BACKGROUND_SYNC, true);
        user.PreventTakingScreenshots = securePrefs.getInt(PREF_PREVENT_TAKING_SCREENSHOTS, 0);
        user.GcmDownloadMessageDetails = securePrefs.getBoolean(PREF_GCM_DOWNLOAD_MESSAGE_DETAILS, false);
        int maxAttachmentStorage = securePrefs.getInt(PREF_MAX_ATTACHMENT_STORAGE, Constants.MIN_ATTACHMENT_STORAGE_IN_MB);
        if (maxAttachmentStorage <= 0) {
            maxAttachmentStorage = Constants.MIN_ATTACHMENT_STORAGE_IN_MB; // defaulting to the min allowed att storage
        }
        user.MaxAttachmentStorage = maxAttachmentStorage;
        user.ManuallyLocked = securePrefs.getBoolean(PREF_MANUALLY_LOCKED, false);

        user.NotificationVisibilityLockScreen = user.loadNotificationVisibilityLockScreenSettingsFromBackup();
        user.AutoLockPINPeriod = user.loadAutoLockPINPeriodFromBackup();
        user.UsePin = user.loadUsePinFromBackup();
        user.UseFingerprint = user.loadUseFingerprintFromBackup();
        user.CombinedContacts = user.loadCombinedContactsFromBackup();
        user.LastInteraction = user.loadLastInteractionFromBackup();
        String notificationRingtone = user.loadRingtoneBackup();
        if (!TextUtils.isEmpty(notificationRingtone)) {
            user.ringtone = Uri.parse(notificationRingtone);
        }

        return new Right(user);
    }

    // region SecureSharePref

    private SharedPreferences getPreferences() {
        ProtonMailApplication application = ProtonMailApplication.getApplication();
        return SecureSharedPreferences.Companion.getPrefsForUser(application, new Id(id));
    }

    private void saveShowSignatureSetting() {
        getPreferences().edit().putBoolean(PREF_DISPLAY_SIGNATURE, ShowSignature).apply();
    }

    private boolean loadShowSignatureSetting() {
        return getPreferences().getBoolean(PREF_DISPLAY_SIGNATURE, false);
    }

    private void saveShowMobileSignatureSetting() {
        getPreferences().edit().putBoolean(PREF_DISPLAY_MOBILE, ShowMobileSignature).apply();
    }

    private boolean loadShowMobileSignatureSetting() {
        return getPreferences().getBoolean(PREF_DISPLAY_MOBILE, true);
    }

    private void saveGcmDownloadMessageDetailsSetting() {
        getPreferences().edit().putBoolean(PREF_GCM_DOWNLOAD_MESSAGE_DETAILS, GcmDownloadMessageDetails).apply();
    }

    private boolean loadGcmDownloadMessageDetailsSetting() {
        return getPreferences().getBoolean(PREF_GCM_DOWNLOAD_MESSAGE_DETAILS, false);
    }

    private void saveBackgroundSyncSetting() {
        getPreferences().edit().putBoolean(PREF_BACKGROUND_SYNC, BackgroundSync).apply();
    }

    private boolean loadBackgroundSyncSetting() {
        return getPreferences().getBoolean(PREF_BACKGROUND_SYNC, true);
    }

    private void savePreventTakingScreenshotsSetting() {
        getPreferences().edit().putInt(PREF_PREVENT_TAKING_SCREENSHOTS, 0).apply();
    }

    public void saveMaxAttachmentStorageSetting() {
        getPreferences().edit().putInt(PREF_MAX_ATTACHMENT_STORAGE, MaxAttachmentStorage).apply();
    }

    public void saveManuallyLockedSetting() {
        getPreferences().edit().putBoolean(PREF_MANUALLY_LOCKED, ManuallyLocked).apply();
    }

    private int loadPreventTakingScreenshotsSetting() {
        return getPreferences().getInt(PREF_PREVENT_TAKING_SCREENSHOTS, 0);
    }

    public void setAllowSecureConnectionsViaThirdParties(boolean allowSecureConnectionsViaThirdParties) {
        getPreferences().edit().putBoolean(PREF_ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES, allowSecureConnectionsViaThirdParties).apply();
    }

    public boolean getAllowSecureConnectionsViaThirdParties() {
        SharedPreferences secureSharedPreferences = ProtonMailApplication.getApplication().getSecureSharedPreferences();
        return secureSharedPreferences.getBoolean(PREF_ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES, true); // automatic opt-in for users
    }

    /**
     * Using default proton api (not proxy).
     *
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

    public void saveNotificationVisibilityLockScreenSettingsBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putInt(PREF_NOTIFICATION_VISIBILITY_LOCK_SCREEN, NotificationVisibilityLockScreen).apply();
    }

    private int loadNotificationVisibilityLockScreenSettingsFromBackup() {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getInt(PREF_NOTIFICATION_VISIBILITY_LOCK_SCREEN, -1);
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

    public long getUsedSpace() {
        return usedSpace;
    }

    public int getRole() {
        return role;
    }

    public boolean isPaidUserSignatureEdit() {
        boolean allowMobileSignatureEdit = ProtonMailApplication.getApplication().getResources().getBoolean(R.bool.allow_mobile_signature_edit);
        return allowMobileSignatureEdit || role > 0;
    }

    public int getPrivate() {
        return isPrivate;
    }

    public int getSubscribed() {
        return subscribed;
    }

    public int getServices() {
        return services;
    }

    public boolean isPaidUser() {
        return subscribed > 0;
    }

    public int getCredit() {
        return credit;
    }

    public String getCurrency() {
        return currency;
    }

    public int getDelinquentValue() {
        return delinquent;
    }

    public boolean getDelinquent() {
        return delinquent >= 3;
    }

    public int getMaxUpload() {
        return maxUpload;
    }

    public void setAndSaveUsedSpace(long usedSpace) {
        if (this.usedSpace != usedSpace) {
            this.usedSpace = usedSpace;
            Timber.v("SetAndSaveUsedSpace for username: `" + username + "`");
            getPreferences().edit().putLong(PREF_USED_SPACE, this.usedSpace).apply();
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

    public String getSignatureForAddress(String addressId) {
        for (Address address : addresses) {
            if (address.getID().equals(addressId)) {
                return address.getSignature();
            }
        }
        return "";
    }

    public String getMobileSignature() {
        return MobileSignature == null ? "" : MobileSignature;
    }

    public void setMobileSignature(String mobileSignature) {
        MobileSignature = mobileSignature;
    }

    public String getDefaultAddressId() {
        return defaultAddressId;
    }

    public String getDefaultAddressEmail() {
        return defaultAddressEmail;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public Address getAddressById(String addressId) {
        for (Address address : addresses) {
            if (address.getID().equals(addressId)) {
                return address;
            }
        }
        return null;
    }

    public List<Keys> getKeys() {
        return (keys != null) ? keys : new ArrayList<>();
    }

    public int getAddressByIdFromOnlySendAddresses() {
        int result = 0;
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

    public List<String> getSenderEmailAddresses() {
        List<String> result = new ArrayList<>();
        for (Address address : addresses) {
            if (address.getStatus() == 1 && address.getReceive() == 1) {
                result.add(address.getEmail());
            }
        }
        return result;
    }

    private List<Address> getSenderAddresses() {
        List<Address> result = new ArrayList<>();
        for (Address address : addresses) {
            if (address.getStatus() == 1 && address.getReceive() == 1) {
                result.add(address);
            }
        }
        return result;
    }

    private List<Address> getSenderOnlyAddresses() {
        List<Address> result = new ArrayList<>();
        for (Address address : addresses) {
            if (address.getStatus() == 1) {
                result.add(address);
            }
        }
        return result;
    }

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

    public String getSenderAddressIdByEmail(String email) {
        String result = null;
        for (Address address : addresses) {
            if (address.getEmail().equals(email)) {
                result = address.getID();
                break;
            }
        }
        return result;
    }

    public String getSenderAddressNameByEmail(String email) {
        String result = null;
        for (Address address : addresses) {
            if (address.getEmail().equals(email)) {
                result = address.getDisplayName();
                break;
            }
        }
        return result;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayNameForAddress(String addressId) {
        for (Address address : addresses) {
            if (address.getID().equals(addressId) && !TextUtils.isEmpty(address.getDisplayName())) {
                return address.getDisplayName();
            }
        }
        return displayName;
    }

    public long getMaxSpace() {
        return maxSpace;
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
        saveMaxAttachmentStorageSetting();
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
        saveManuallyLockedSetting();
    }

    public String getUsername() {
        return username;
    }

    public long getMaxAllowedAttachmentSpace() {
        return MaxAttachmentStorage; // return the value in bytes
    }

    /**
     * Convert this User to new User entity
     *
     * @return {@link ch.protonmail.android.domain.entity.user.User}
     */
    public ch.protonmail.android.domain.entity.user.User toNewUser() {
        return UserBridgeMapper.buildDefault().toNewModel(this);
    }
}

