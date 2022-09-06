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
package ch.protonmail.android.api.interceptors

import ch.protonmail.android.utils.TryWithExponentialBackoff
import ch.protonmail.android.utils.extensions.isRetryableError
import ch.protonmail.android.utils.extensions.isRetryableNetworkError
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class RetryRequestInterceptor @Inject constructor(
    val tryWithExponentialBackoff: TryWithExponentialBackoff
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()

        return runBlocking {
            tryWithExponentialBackoff(
                numberOfRetries = NUMBER_OF_RETRIES,
                backoffDuration = BACKOFF_DURATION,
                block = { chain.proceed(request) },
                shouldRetryOnError = { error -> error.isRetryableNetworkError() },
                shouldRetryOnResult = { response -> response.isRetryableError() },
            ).fold(
                ifLeft = { throw it },
                ifRight = { result -> result }
            )
        }
    }

    companion object {

        val BACKOFF_DURATION = 500.milliseconds
        const val NUMBER_OF_RETRIES = 2
    }
}
