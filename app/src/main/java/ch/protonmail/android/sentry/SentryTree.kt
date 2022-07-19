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
package ch.protonmail.android.sentry

import android.util.Log
import ch.protonmail.android.core.DetailedException
import ch.protonmail.android.data.remote.OfflineException
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.utils.extensions.isServerError
import ch.protonmail.android.utils.extensions.obfuscateEmail
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.protocol.Message
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Production variant of [Timber.Tree]
 * Logs only [Log.INFO]+ to Logcat
 * Logs only [Log.WARN]+ to Sentry
 *
 * Email addresses are obfuscated for Logcat
 */
internal class SentryTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (throwable.shouldBeIgnored()) return

        val event = SentryEvent().apply {
            tag?.let { setTag(TAG_LOG, it) }
            if (throwable is DetailedException) {
                setExtras(throwable.extras)
            }
            setMessage(obfuscatedMessage(message))
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

    private fun Throwable?.shouldBeIgnored() = when (this) {
        is CancellationException,
        is UnknownHostException,
        is SocketTimeoutException,
        is SSLException,
        is ConnectException,
        is SocketException,
        is OfflineException -> true
        is HttpException -> isServerError()
        else -> false
    }

    private companion object {

        const val TAG_LOG = "LOG_TAG"
    }
}
