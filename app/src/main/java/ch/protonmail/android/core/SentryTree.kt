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

/**
 * Production variant of [Timber.Tree]
 * Logs only [Log.INFO]+ to Logcat
 * Logs only [Log.WARN]+ to Sentry
 *
 * Email addresses are obfuscated for Logcat
 */
internal class SentryTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        if (priority >= Log.WARN) {
            val event = EventBuilder().apply {
                tag?.let { withTag(TAG_LOG, it) }
                if (t is DetailedException) {
                    for (extra in t.extras) {
                        withExtra(extra.key, extra.value)
                    }
                }
                withMessage(obfuscateEmails(message))
                withTag(TAG_APP_VERSION, AppUtil.getAppVersion())
                withTag(TAG_SDK_VERSION, "${Build.VERSION.SDK_INT}")
                withTag(TAG_DEVICE_MODEL, Build.MODEL)
            }.build()
            Sentry.capture(event)
        }
    }

    private companion object {
        private const val TAG_LOG = "LOG_TAG"
        private const val TAG_APP_VERSION = "APP_VERSION"
        private const val TAG_SDK_VERSION = "SDK_VERSION"
        private const val TAG_DEVICE_MODEL = "DEVICE_MODEL"
    }

    fun obfuscateEmails(string: String): String =
        string.replace(EmailAddress.VALIDATION_REGEX) {
            val (id, host) = it.value.split("@")
            val limit = id.length - 3
            val replacement = id.mapIndexed { i: Int, c: Char ->
                if (i < limit) "*" else c
            }.joinToString(separator = "")
            "$replacement@$host"
        }
}
