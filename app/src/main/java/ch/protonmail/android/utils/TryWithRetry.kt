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

package ch.protonmail.android.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import javax.inject.Inject

private const val DEFAULT_RETRY_COUNT = 2

class TryWithRetry @Inject constructor() {

    suspend operator fun <T> invoke(
        numberOfRetries: Int = DEFAULT_RETRY_COUNT,
        shouldRetryOnError: (error: Exception) -> Boolean = { true },
        shouldRetryOnResult: (result: T) -> Boolean = { false },
        beforeRetry: suspend (retryCount: Int) -> Unit = { },
        block: suspend () -> T,
    ): Either<Exception, T> {
        var retryCount = 0
        var lastResult: T? = null
        var lastException: Exception? = null

        try {
            lastResult = block()
            if (!shouldRetryOnResult(lastResult)) return lastResult.right()
        } catch (exception: Exception) {
            lastException = exception
            if (!shouldRetryOnError(lastException)) return lastException.left()
        }

        while (retryCount < numberOfRetries) {
            try {
                beforeRetry(retryCount)
                lastResult = block()
                if (!shouldRetryOnResult(lastResult)) return lastResult.right()
            } catch (exception: Exception) {
                lastException = exception
                lastResult = null
                if (!shouldRetryOnError(lastException)) return lastException.left()
            }
            retryCount++
        }
        return lastResult?.right()
            ?: lastException?.left()
            ?: IllegalStateException("No result nor exception").left()
    }
}
