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

import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;

public class FcmUtil {
    private static final String TAG_FCM_UTIL = "FcmUtil";

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    public static final String SENDER_ID = "75309174866";

    private FcmUtil() {
    }

    /**
     * Gets the current registration ID for application on FCM service.
     * <p/>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    public static String getRegistrationId() {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final SharedPreferences prefs = app.getDefaultSharedPreferences();

        String registrationId = prefs.getString(Constants.Prefs.PREF_REGISTRATION_ID, "");
        if (registrationId.isEmpty()) {
            Logger.doLog(TAG_FCM_UTIL, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing registration ID is not guaranteed to work with
        // the new app version.
        int registeredVersion = prefs.getInt(Constants.Prefs.PREF_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = AppUtil.getAppVersionCode(app);
        if (registeredVersion != currentVersion) {
            Logger.doLog(TAG_FCM_UTIL, "App version changed");
            return "";
        }
        return registrationId;
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * Note: Do not call this method from the UI thread.
     *
     * @param registrationId registration ID
     */
    @SuppressLint("CommitPrefEdits")
    public static void setRegistrationId(String registrationId) {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final SharedPreferences prefs = app.getDefaultSharedPreferences();
        int appVersion = AppUtil.getAppVersionCode(app);
        // the commit is because the saving of registration id is considered always to be called from a background thread
        prefs.edit()
                .putString(Constants.Prefs.PREF_REGISTRATION_ID, registrationId)
                .putInt(Constants.Prefs.PREF_APP_VERSION, appVersion)
                .commit();
    }

    public static void setTokenSent(boolean sent) {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final SharedPreferences prefs = app.getDefaultSharedPreferences();
        prefs.edit().putBoolean(Constants.Prefs.PREF_SENT_TOKEN_TO_SERVER, sent).apply();
    }

    public static boolean isTokenSent() {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final SharedPreferences prefs = app.getDefaultSharedPreferences();
        return prefs.getBoolean(Constants.Prefs.PREF_SENT_TOKEN_TO_SERVER, false);
    }
}