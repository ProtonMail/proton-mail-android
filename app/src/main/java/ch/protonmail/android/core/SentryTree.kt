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
import ch.protonmail.android.utils.extensions.obfuscateEmail
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.protocol.Message
import timber.log.Timber

/**
 * Production variant of [Timber.Tree]
 * Logs only [Log.INFO]+ to Logcat
 * Logs only [Log.WARN]+ to Sentry
 *
 * Email addresses are obfuscated for Logcat
 */
internal class SentryTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val event = SentryEvent().apply {
            tag?.let { setTag(TAG_LOG, it) }
            if (t is DetailedException) {
                setExtras(t.extras)
            }
            setMessage(obfuscatedMessage(message))
            setTag(TAG_APP_VERSION, AppUtil.getAppVersion())
            setTag(TAG_SDK_VERSION, "${Build.VERSION.SDK_INT}")
            setTag(TAG_DEVICE_MODEL, Build.MODEL)
        }
        Sentry.captureEvent(event)
    }

    private fun obfuscatedMessage(string: String): Message = Message().apply {
        message = obfuscateEmails(string)
    }

    private fun obfuscateEmails(string: String): String =
        string.replace(EmailAddress.VALIDATION_REGEX) {
            it.value.obfuscateEmail()
        }

    private companion object {
        private const val TAG_LOG = "LOG_TAG"
        private const val TAG_APP_VERSION = "APP_VERSION"
        private const val TAG_SDK_VERSION = "SDK_VERSION"
        private const val TAG_DEVICE_MODEL = "DEVICE_MODEL"
    }
}
