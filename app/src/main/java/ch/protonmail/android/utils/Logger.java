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

import android.text.TextUtils;
import android.util.Log;

import ch.protonmail.android.BuildConfig;

/**
 * Created by dkadrikj on 16.7.15.
 */
public class Logger {
    private static final String TAG_PROTON_MAIL = "ProtonMail";

    public static void doLog(String tag, String text) {
        if (BuildConfig.DEBUG && !TextUtils.isEmpty(text)) {
            Log.i(tag, text);
        }
    }

    public static void doLog(String text) {
        if (BuildConfig.DEBUG && !TextUtils.isEmpty(text)) {
            Log.i(TAG_PROTON_MAIL, text);
        }
    }

    public static void doLogException(Exception e) {
        if (BuildConfig.DEBUG && e != null) {
            Log.e(TAG_PROTON_MAIL, "ProtonMail Exception", e);
        }
    }

    public static void doLogException(String tag, Exception e) {
        if (BuildConfig.DEBUG && e != null) {
            Log.e(tag, "ProtonMail Exception", e);
        }
    }

    public static void doLogException(String tag, String message, Throwable e) {
        if (BuildConfig.DEBUG && e != null) {
            Log.e(tag, message, e);
        }
    }
}
