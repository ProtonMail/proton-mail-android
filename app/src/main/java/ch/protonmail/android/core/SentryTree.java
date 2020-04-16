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
package ch.protonmail.android.core;

import android.os.Build;
import android.util.Log;

import org.jetbrains.annotations.Nullable;

import ch.protonmail.android.utils.AppUtil;
import io.sentry.Sentry;
import io.sentry.event.EventBuilder;
import timber.log.Timber;

class SentryTree extends Timber.Tree {

    private final String NO_MESSAGE = "NO_TIMBER_MESSAGE";
    private final String NO_TAG = "NO_TIMBER_TAG";

    private final String TAG_ANDROID_TAG = "TAG_ANDROID_TAG";
    private final String TAG_APP_VERSION = "APP_VERSION";
    private final String TAG_SDK_VERSION = "SDK_VERSION";
    private final String TAG_DEVICE_MODEL = "DEVICE_MODEL";

    /**
     * This method is called by all other logging methods. It ignores all levels up to and including DEBUG.
     *
     * @param message documentation says this can be null, yet annotations say otherwise, so to be
     *                on the safe side, we set it here as nullable
     */
    @Override
    protected void log(int priority, @Nullable String tag, @Nullable String message, @Nullable Throwable t) {
        if (priority <= Log.DEBUG) {
            return;
        }

        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.withMessage(message != null ? message : NO_MESSAGE);
        eventBuilder.withTag(TAG_ANDROID_TAG, tag != null ? tag : NO_TAG);
        eventBuilder.withTag(TAG_APP_VERSION, AppUtil.getAppVersion());
        eventBuilder.withTag(TAG_SDK_VERSION, "" + Build.VERSION.SDK_INT);
        eventBuilder.withTag(TAG_DEVICE_MODEL, Build.MODEL);

        Sentry.capture(eventBuilder.build());
    }
}
