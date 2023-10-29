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
package ch.protonmail.android.api.models;

import static ch.protonmail.android.core.Constants.Prefs.PREF_ADDRESS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_ADDRESS_ID;
import static ch.protonmail.android.core.Constants.Prefs.PREF_ALIASES;
import static ch.protonmail.android.core.Constants.Prefs.PREF_ALLOW_SECURE_CONNECTIONS_VIA_THIRD_PARTIES;
import static ch.protonmail.android.core.Constants.Prefs.PREF_AUTO_LOCK_PIN_PERIOD;
import static ch.protonmail.android.core.Constants.Prefs.PREF_BACKGROUND_SYNC;
import static ch.protonmail.android.core.Constants.Prefs.PREF_COMBINED_CONTACTS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_DELINQUENT;
import static ch.protonmail.android.core.Constants.Prefs.PREF_DISPLAY_MOBILE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_DISPLAY_NAME;
import static ch.protonmail.android.core.Constants.Prefs.PREF_DISPLAY_SIGNATURE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_GCM_DOWNLOAD_MESSAGE_DETAILS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_KEYS;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MAX_ATTACHMENT_STORAGE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MAX_SPACE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MAX_UPLOAD_FILE_SIZE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_MOBILE_FOOTER;
import static ch.protonmail.android.core.Constants.Prefs.PREF_NOTIFICATION;
import static ch.protonmail.android.core.Constants.Prefs.PREF_NOTIFICATION_VISIBILITY_LOCK_SCREEN;
import static ch.protonmail.android.core.Constants.Prefs.PREF_RINGTONE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_ROLE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_SUBSCRIBED;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USED_SPACE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_CREDIT;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_CURRENCY;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_NAME;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_PRIVATE;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USER_SERVICES;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USE_FINGERPRINT;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USE_PIN;
import static ch.protonmail.android.core.Constants.Prefs.PREF_USING_REGULAR_API;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import arrow.core.Either;
import arrow.core.Either.Right;
import ch.protonmail.android.R;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.feature.user.UserManagerKt;
import ch.protonmail.android.mapper.bridge.UserBridgeMapper;
import ch.protonmail.android.prefs.SecureSharedPreferences;
import ch.protonmail.android.usecase.LoadUser;
import me.proton.core.crypto.common.keystore.EncryptedByteArray;
import me.proton.core.crypto.common.keystore.KeyStoreCrypto;
import me.proton.core.domain.entity.UserId;
import me.proton.core.network.domain.ApiException;
import me.proton.core.user.domain.UserManager;
import timber.log.Timber;

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
    private EncryptedByteArray passphrase;

    private List<Keys> keys;
    private List<Address> addresses;
    private String defaultAddressId;
    private String defaultAddressEmail;

    private String mobileFooter;
    private boolean ShowMobileFooter = true;
    private boolean ShowSignature = true;

    // new

    // region these are local only - do not touch them
    private int AutoLockPINPeriod = -1; // this can remain here, local only setting

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
    private boolean GcmDownloadMessageDetails; // this can remain here, local only setting
    private boolean UsePin; // this can remain here, local only setting
    private boolean UseFingerprint; // this can remain here, local only setting
    private Uri ringtone; // this can remain here, local only setting
    private int MaxAttachmentStorage; // this can remain here, local only setting
    private String username; // this can remain here, local only setting
    private boolean CombinedContacts; // this can remain here, local only setting
    // endregion

    @NonNull
    @Deprecated
    @kotlin.Deprecated(message = "Use usecase/LoadLegacyUser")
    public static Either<LoadUser.Error, User> load(UserId userId, Context context, UserManager userManager, KeyStoreCrypto keyStoreCrypto) {
        final SharedPreferences securePrefs = SecureSharedPreferences.Companion.getPrefsForUser(context, userId);

        User user;

        try {
            user = loadFromCore(userId, securePrefs, userManager, keyStoreCrypto);
        } catch (ApiException exception) {
            Timber.e(exception);
            user = loadFromPrefs(userId, securePrefs);
        }

        return new Right(user);
    }

    private static User loadFromCore(UserId userId, SharedPreferences securePrefs, UserManager userManager, KeyStoreCrypto keyStoreCrypto) throws ApiException {
        // Core UserManager (UserRepository & UserAddressRepository) have a memory cache.
        // Core UserManager will only do a network call if no data exist.

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

        user.passphrase = UserManagerKt.getUserPassphrase(coreUser);

        loadLocalSettings(user, securePrefs);

        return user;
    }

    public static User loadFromPrefs(UserId userId, Context context) {
        final SharedPreferences securePrefs = SecureSharedPreferences.Companion.getPrefsForUser(context, userId);
        return loadFromPrefs(userId, securePrefs);
    }

    private static User loadFromPrefs(UserId userId, SharedPreferences securePrefs) {
        final User user = new User();
        user.id = userId.getId();
        user.name = securePrefs.getString(PREF_USER_NAME, "");
        user.displayName = securePrefs.getString(PREF_DISPLAY_NAME, "");
        user.username = user.name;
        user.usedSpace = securePrefs.getLong(PREF_USED_SPACE, 0L);
        user.role = securePrefs.getInt(PREF_ROLE, 0);
        user.subscribed = securePrefs.getInt(PREF_SUBSCRIBED, 0);
        user.currency = securePrefs.getString(PREF_USER_CURRENCY, "eur");
        user.credit = securePrefs.getInt(PREF_USER_CREDIT, 0);
        user.delinquent = securePrefs.getInt(PREF_DELINQUENT, 0);
        user.isPrivate = securePrefs.getInt(PREF_USER_PRIVATE, 0);
        user.services = securePrefs.getInt(PREF_USER_SERVICES, 0);
        user.maxSpace = securePrefs.getLong(PREF_MAX_SPACE, 0L);
        user.maxUpload = securePrefs.getInt(PREF_MAX_UPLOAD_FILE_SIZE, 0);

        user.keys = deserializeKeys(securePrefs.getString(PREF_KEYS, ""));
        user.addresses = deserializeAddresses(securePrefs.getString(PREF_ALIASES, ""));

        if (user.addresses.size() >= 1) {
            user.defaultAddressId = user.addresses.get(0).getID();
            user.defaultAddressEmail = user.addresses.get(0).getEmail();
        } else {
            user.defaultAddressId = securePrefs.getString(PREF_ADDRESS_ID, "");
            user.defaultAddressEmail = securePrefs.getString(PREF_ADDRESS, "");
        }

        loadLocalSettings(user, securePrefs);

        return user;
    }

    private static void loadLocalSettings(User user, SharedPreferences securePrefs) {
        if (!user.isPaidUserSignatureEdit()) {
            user.mobileFooter = ProtonMailApplication.getApplication().getString(R.string.default_mobile_footer);
        } else {
            user.mobileFooter = securePrefs.getString(PREF_MOBILE_FOOTER, ProtonMailApplication.getApplication().getString(R.string.default_mobile_footer));
        }
        user.ShowMobileFooter = securePrefs.getBoolean(PREF_DISPLAY_MOBILE, false);
        if (!user.ShowMobileFooter && !user.isPaidUserSignatureEdit()) {
            user.ShowMobileFooter = true;
            user.setShowMobileFooter(true);
        }
        user.ShowSignature = securePrefs.getBoolean(PREF_DISPLAY_SIGNATURE, true);
        user.NotificationSetting = user.loadNotificationSettingsFromBackup();
        user.BackgroundSync = securePrefs.getBoolean(PREF_BACKGROUND_SYNC, true);
        user.GcmDownloadMessageDetails = securePrefs.getBoolean(PREF_GCM_DOWNLOAD_MESSAGE_DETAILS, false);
        int maxAttachmentStorage = securePrefs.getInt(PREF_MAX_ATTACHMENT_STORAGE, Constants.DEFAULT_ATTACHMENT_STORAGE_IN_MB);
        if (maxAttachmentStorage == 0) {
            maxAttachmentStorage = Constants.DEFAULT_ATTACHMENT_STORAGE_IN_MB;
            securePrefs.edit().putInt(PREF_MAX_ATTACHMENT_STORAGE, Constants.DEFAULT_ATTACHMENT_STORAGE_IN_MB).apply();
        }
        user.MaxAttachmentStorage = maxAttachmentStorage;

        user.NotificationVisibilityLockScreen = user.loadNotificationVisibilityLockScreenSettingsFromBackup();
        user.AutoLockPINPeriod = user.loadAutoLockPINPeriodFromBackup();
        user.UsePin = user.loadUsePinFromBackup();
        user.UseFingerprint = user.loadUseFingerprintFromBackup();
        user.CombinedContacts = user.loadCombinedContactsFromBackup();
        String notificationRingtone = user.loadRingtoneBackup();
        if (!TextUtils.isEmpty(notificationRingtone)) {
            user.ringtone = Uri.parse(notificationRingtone);
        }
    }

    // region SecureSharePref

    private SharedPreferences getPreferences() {
        ProtonMailApplication application = ProtonMailApplication.getApplication();
        return SecureSharedPreferences.Companion.getPrefsForUser(application, new UserId(id));
    }

    private void saveShowSignatureSetting() {
        getPreferences().edit().putBoolean(PREF_DISPLAY_SIGNATURE, ShowSignature).apply();
    }

    private void saveShowMobileFooterSetting() {
        getPreferences().edit().putBoolean(PREF_DISPLAY_MOBILE, ShowMobileFooter).apply();
    }

    private void saveMobileFooterSetting() {
        getPreferences().edit().putString(PREF_MOBILE_FOOTER, mobileFooter).apply();
    }

    private void saveGcmDownloadMessageDetailsSetting() {
        getPreferences().edit().putBoolean(PREF_GCM_DOWNLOAD_MESSAGE_DETAILS, GcmDownloadMessageDetails).apply();
    }

    private void saveBackgroundSyncSetting() {
        getPreferences().edit().putBoolean(PREF_BACKGROUND_SYNC, BackgroundSync).apply();
    }

    public void saveMaxAttachmentStorageSetting() {
        getPreferences().edit().putInt(PREF_MAX_ATTACHMENT_STORAGE, MaxAttachmentStorage).apply();
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
     *
     * @param useDefaultApi boolean
     */
    public void setUsingDefaultApi(boolean useDefaultApi) {
        SharedPreferences secureSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ProtonMailApplication.getApplication());
        secureSharedPreferences.edit().putBoolean(PREF_USING_REGULAR_API, useDefaultApi).apply();
    }

    public boolean getUsingDefaultApi() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ProtonMailApplication.getApplication());
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
        return isPaidUser() || role > 0;
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

    @Nullable
    public EncryptedByteArray getPassphrase() {
        return passphrase;
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

    public String getMobileFooter() {
        return mobileFooter == null ? "" : mobileFooter;
    }

    public void setMobileFooter(String mobileFooter) {
        this.mobileFooter = mobileFooter;
        saveMobileFooterSetting();
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

    private static List<Keys> deserializeKeys(String serialized) {
        if (serialized.isEmpty())
            return new ArrayList<>();
        Gson gson = new Gson();
        Keys[] out = gson.fromJson(serialized, Keys[].class);
        return Arrays.asList(out);
    }

    public boolean isShowMobileFooter() {
        return ShowMobileFooter;
    }

    public void setShowMobileFooter(boolean showMobileFooter) {
        ShowMobileFooter = showMobileFooter;
        saveShowMobileFooterSetting();
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

    @Deprecated // Use getPinLockTimer use case
    public int getAutoLockPINPeriod() {
        AutoLockPINPeriod = loadAutoLockPINPeriodFromBackup();
        return AutoLockPINPeriod;
    }

    public void setAutoLockPINPeriod(int autoLockPINPeriod) {
        AutoLockPINPeriod = autoLockPINPeriod;
        saveAutoLockPINPeriodBackup();
    }

    @Deprecated // Use IsPinLockEnabled use case
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

    public boolean isNotificationVisibilityLockScreen() {
        return loadNotificationVisibilityLockScreenSettingsFromBackup() == 1;
    }

    public void setNotificationVisibilityLockScreen(boolean notificationVisibilityLockScreen) {
        NotificationVisibilityLockScreen = notificationVisibilityLockScreen ? 1 : 0;
        saveNotificationVisibilityLockScreenSettingsBackup();
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

    public String getUsername() {
        return username;
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

