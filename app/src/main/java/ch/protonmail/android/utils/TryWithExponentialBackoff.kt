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
import ch.protonmail.android.utils.extensions.exponentialDelay
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration

private const val DEFAULT_RETRY_COUNT = 2

class TryWithExponentialBackoff @Inject constructor(
    private val tryWithRetry: TryWithRetry
) {

    suspend operator fun <T> invoke(
        numberOfRetries: Int = DEFAULT_RETRY_COUNT,
        backoffDuration: Duration,
        exponentialBase: Double = 1.2,
        shouldRetryOnError: (error: Exception) -> Boolean = { true },
        shouldRetryOnResult: (result: T) -> Boolean = { false },
        block: suspend () -> T,
    ): Either<Exception, T> {
        return tryWithRetry(
            numberOfRetries = numberOfRetries,
            shouldRetryOnError = shouldRetryOnError,
            shouldRetryOnResult = shouldRetryOnResult,
            beforeRetry = { retryCount ->
                delay(
                    backoffDuration.exponentialDelay(retryCount, exponentialBase)
                )
            },
            block = block
        )
    }
}
