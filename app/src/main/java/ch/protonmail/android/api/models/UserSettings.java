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

import static ch.protonmail.android.core.Constants.Prefs.PREF_NOTIFICATION_EMAIL;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_EMAIL;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_INVOICE_TEXT;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_LOCALE;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_LOG_AUTH;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_NEWS;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_PASSWORD_MODE;
import static ch.protonmail.android.core.Constants.Prefs.UserSettings.PREF_TWO_FACTOR;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.utils.FileUtils;
import kotlin.jvm.Transient;

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

    @Transient
    private String username;
    @Transient
    private String NotificationEmail;

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

    public String getNotificationEmail() {
        if (TextUtils.isEmpty(NotificationEmail)) {
            if (email != null) {
                NotificationEmail = email.getValue();
            }
        }
        return NotificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.NotificationEmail = notificationEmail;
    }

    public static UserSettings load(String username) {
        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(username);

        UserSettings userSettings = new UserSettings();
        int passwordMode = pref.getInt(PREF_PASSWORD_MODE, -1); //-1 means we need to reload
        int twoFactor = pref.getInt(PREF_TWO_FACTOR, -1); // -1 means we need to reload
        String email = pref.getString(PREF_EMAIL, null);
        String notificationEmail = pref.getString(PREF_NOTIFICATION_EMAIL, "");
        UserSettingsEmail userSettingsEmail = FileUtils.deserializeStringToObject(email);
        if (!TextUtils.isEmpty(username)) {
            userSettings.username = username;
        }
        userSettings.setPasswordMode(passwordMode);
        userSettings.setTwoFactor(twoFactor);
        userSettings.setEmail(userSettingsEmail);
        userSettings.setNotificationEmail(notificationEmail);
        return userSettings;
    }

    public void save() {

        final SharedPreferences pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(this.username);

        pref.edit()
                .putInt(PREF_PASSWORD_MODE, passwordMode)
                .putInt(PREF_NEWS, news)
                .putString(PREF_LOCALE, locale)
                .putInt(PREF_LOG_AUTH, logAuth)
                .putString(PREF_INVOICE_TEXT, invoiceText)
                .putInt(PREF_TWO_FACTOR, twoFactor)
                .putString(PREF_EMAIL, FileUtils.toString(email))
                .putString(PREF_NOTIFICATION_EMAIL, NotificationEmail)
                .apply();
        //todo handle email
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
