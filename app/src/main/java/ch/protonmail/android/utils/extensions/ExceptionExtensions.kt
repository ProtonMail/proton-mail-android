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

package ch.protonmail.android.utils.extensions

import ch.protonmail.android.api.segments.RESPONSE_CODE_REQUEST_TIMEOUT
import ch.protonmail.android.api.segments.RESPONSE_CODE_TOO_MANY_REQUESTS
import retrofit2.HttpException
import java.io.IOException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

fun Exception.isRetryableNetworkError() = when (this) {
    is HttpException ->
        isServerError() || code() == RESPONSE_CODE_TOO_MANY_REQUESTS || code() == RESPONSE_CODE_REQUEST_TIMEOUT
    is SSLPeerUnverifiedException,
    is SSLHandshakeException -> false
    is IOException -> true
    else -> false
}
