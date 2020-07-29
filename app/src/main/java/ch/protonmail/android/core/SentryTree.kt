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
package ch.protonmail.android.core

import android.os.Build
import android.util.Log
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.utils.AppUtil
import io.sentry.Sentry
import io.sentry.event.EventBuilder
import timber.log.Timber

internal class SentryTree : Timber.Tree() {

    /**
     * This method is called by all other logging methods. It ignores all levels up to and including DEBUG.
     *
     * @param message documentation says this can be null, yet annotations say otherwise, so to be
     * on the safe side, we set it here as nullable
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority <= Log.DEBUG) {
            return
        }
        val eventBuilder = EventBuilder()
        eventBuilder.withMessage(message)
        eventBuilder.withTag(TAG_ANDROID_TAG, tag ?: NO_TAG)
        eventBuilder.withTag(TAG_APP_VERSION, AppUtil.getAppVersion())
        eventBuilder.withTag(TAG_SDK_VERSION, "${Build.VERSION.SDK_INT}")
        eventBuilder.withTag(TAG_DEVICE_MODEL, Build.MODEL)
        Sentry.capture(eventBuilder.build())
    }

    private companion object {
        private const val NO_TAG = "NO_TIMBER_TAG"
        private const val TAG_ANDROID_TAG = "TAG_ANDROID_TAG"
        private const val TAG_APP_VERSION = "APP_VERSION"
        private const val TAG_SDK_VERSION = "SDK_VERSION"
        private const val TAG_DEVICE_MODEL = "DEVICE_MODEL"
    }

    fun obfuscateEmails(string: String): String =
        string.replace(EmailAddress.VALIDATION_REGEX.toRegex()) {
            val (id, host) = it.value.split("@")
            val limit = id.length - 3
            val replacement = id.mapIndexed { i: Int, c: Char ->
                if (i < limit) "*" else c
            }.joinToString(separator = "")
            "$replacement@$host"
        }
}
