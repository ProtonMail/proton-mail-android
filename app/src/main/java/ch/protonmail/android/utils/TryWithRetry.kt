/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.utils

import arrow.core.Either
import javax.inject.Inject

private const val DEFAULT_RETRY_COUNT = 3

class TryWithRetry @Inject constructor() {

    suspend operator fun <T> invoke(
        numberOfRetries: Int = DEFAULT_RETRY_COUNT,
        block: suspend () -> T,
    ): Either<List<Exception>, T> {
        var retryCount = 0
        val exceptions = mutableListOf<Exception>()
        while (retryCount < numberOfRetries) {
            try {
                retryCount++
                return Either.right(block())
            } catch (exception: Exception) {
                exceptions.add(exception)
            }
        }
        return Either.left(exceptions)
    }
}
