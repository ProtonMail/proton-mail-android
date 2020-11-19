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
package ch.protonmail.android.fcm;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import ch.protonmail.android.BuildConfig;
import ch.protonmail.android.api.AccountManager;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import timber.log.Timber;

public class FcmUtil {

    /**
     * Gets the current token for application on FCM service.
     * <p/>
     * If result is empty, the app needs to register.
     *
     * @return Firebase token, or empty string if there is no existing
     * Firebase token.
     */
    @NonNull
    public static String getFirebaseToken() {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final SharedPreferences prefs = app.getDefaultSharedPreferences();

        String registrationId = prefs.getString(Constants.Prefs.PREF_REGISTRATION_ID, "");
        if (registrationId.isEmpty()) {
            Timber.v("Token not found");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing registration ID is not guaranteed to work with
        // the new app version.
        int registeredVersion = prefs.getInt(Constants.Prefs.PREF_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = BuildConfig.VERSION_CODE;
        if (registeredVersion != currentVersion) {
            Timber.v("App version changed");
            return "";
        }
        return registrationId;
    }

    /**
     * Stores the Firebase tokenD and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * Note: Do not call this method from the UI thread.
     *
     * @param token Firebase token
     */
    @SuppressLint("CommitPrefEdits")
    public static void setFirebaseToken(String token) {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final SharedPreferences prefs = app.getDefaultSharedPreferences();
        int appVersion = BuildConfig.VERSION_CODE;
        Timber.v("Saving version:%s registration ID: %s", appVersion, token);
        // the commit is because the saving of registration id is considered always to be called from a background thread
        prefs.edit()
                .putString(Constants.Prefs.PREF_REGISTRATION_ID, token)
                .putInt(Constants.Prefs.PREF_APP_VERSION, appVersion)
                .commit();
    }

    public static void setTokenSent(String username, boolean sent) {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final SharedPreferences prefs = app.getSecureSharedPreferences(username);
        prefs.edit().putBoolean(Constants.Prefs.PREF_SENT_TOKEN_TO_SERVER, sent).apply();
    }

    public static void setTokenSent(boolean sent) {
        for (String username : AccountManager.Companion.getInstance(ProtonMailApplication.getApplication()).getLoggedInUsers()) {
            setTokenSent(username, sent);
        }
    }

    public static boolean isTokenSent(String username) {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final SharedPreferences prefs = app.getSecureSharedPreferences(username);
        return prefs.getBoolean(Constants.Prefs.PREF_SENT_TOKEN_TO_SERVER, false);
    }

    public static boolean isTokenSent() {
        for (String username : AccountManager.Companion.getInstance(ProtonMailApplication.getApplication()).getLoggedInUsers()) {
            if (!isTokenSent(username)) {
                return false;
            }
        }
        return true;
    }
}