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

import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.utils.FileUtils;
import kotlin.jvm.Transient;

import static ch.protonmail.android.core.Constants.Prefs.PREF_NOTIFICATION_EMAIL;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_EMAIL;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_INVOICE_TEXT;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_LOCALE;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_LOG_AUTH;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_NEWS;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_PASSWORD_MODE;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_TWO_FACTOR;

public class UserSettings {

    @SerializedName(Fields.User.PASSWORD_MODE)
    private int passwordMode;

    @SerializedName(Fields.User.NEWS)
    private int news;

    @SerializedName(Fields.User.LOCALE)
    private String locale;

    @SerializedName(Fields.User.LOG_AUTH)
    private int logAuth;

    @SerializedName(Fields.User.INVOICE_TEXT)
    private String invoiceText;

    @SerializedName(Fields.User.TWO_FACTOR)
    private int twoFactor;

    @SerializedName(Fields.User.EMAIL)
    private UserSettingsEmail email;

    @Nullable
    @Transient
    private String notificationEmail;

    public Constants.PasswordMode getPasswordMode() {
        return Constants.PasswordMode.Companion.fromInt(passwordMode);
    }

    public int getTwoFactor() {
        return twoFactor;
    }

    private void setPasswordMode(int passwordMode) {
        this.passwordMode = passwordMode;
    }

    private void setTwoFactor(int twoFactor) {
        this.twoFactor = twoFactor;
    }

    public void setEmail(UserSettingsEmail email) {
        this.email = email;
    }

    @Nullable
    public String getNotificationEmail() {
        if (TextUtils.isEmpty(notificationEmail) && email != null) {
            notificationEmail = email.getValue();
        }
        return notificationEmail;
    }

    public void setNotificationEmail(@Nullable String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public static UserSettings load(SharedPreferences userPreferences) {

        int passwordMode = userPreferences.getInt(PREF_PASSWORD_MODE, -1); //-1 means we need to reload
        int twoFactor = userPreferences.getInt(PREF_TWO_FACTOR, -1); // -1 means we need to reload
        String email = userPreferences.getString(PREF_EMAIL, null);
        String notificationEmail = userPreferences.getString(PREF_NOTIFICATION_EMAIL, "");
        UserSettingsEmail userSettingsEmail = FileUtils.deserializeStringToObject(email);

        UserSettings userSettings = new UserSettings();
        userSettings.setPasswordMode(passwordMode);
        userSettings.setTwoFactor(twoFactor);
        userSettings.setEmail(userSettingsEmail);
        userSettings.setNotificationEmail(notificationEmail);

        return userSettings;
    }

    @Deprecated
    @kotlin.Deprecated(message = "Load with Preferences directly")
    public static UserSettings load(String username) {
        throw new UnsupportedOperationException("Load with Preferences directly");
    }

    public void save(SharedPreferences userPreferences) {

        userPreferences.edit()
                .putInt(PREF_PASSWORD_MODE, passwordMode)
                .putInt(PREF_NEWS, news)
                .putString(PREF_LOCALE, locale)
                .putInt(PREF_LOG_AUTH, logAuth)
                .putString(PREF_INVOICE_TEXT, invoiceText)
                .putInt(PREF_TWO_FACTOR, twoFactor)
                .putString(PREF_EMAIL, FileUtils.toString(email))
                .putString(PREF_NOTIFICATION_EMAIL, notificationEmail)
                .apply();
        // TODO handle email
    }

    @Deprecated
    @kotlin.Deprecated(message = "Save with Preferences directly")
    public void save() {
        throw new UnsupportedOperationException("Save with Preferences directly");
    }

    @Deprecated
    @kotlin.Deprecated(message = "This is not needed, was used only for save")
    public String getUsername() {
        throw new UnsupportedOperationException("This is not needed, was used only for save");
    }

    @Deprecated
    @kotlin.Deprecated(message = "This is not needed, was used only for save")
    public void setUsername(String username) {
        throw new UnsupportedOperationException("This is not needed, was used only for save");
    }
}
