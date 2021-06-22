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
package ch.protonmail.android.utils.crypto

import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.utils.ServerTime
import me.proton.core.network.domain.server.ServerTimeListener
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class ServerTimeInterceptor(
    var serverTimeListener: ServerTimeListener,
    var queueNetworkUtil: QueueNetworkUtil
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        try {
            response = chain.proceed(request)
            handleResponse(response)
        } catch (exception: IOException) {
            Timber.d(exception, "IOException ${request.url}")
            queueNetworkUtil.setConnectivityHasFailed(exception)
        }

        if (response == null) {
            return chain.proceed(request)
        }
        return response
    }

    private fun handleResponse(response: Response) {
        val date = response.headers.getDate("date") ?: return
        serverTimeListener.onServerTimeUpdated(date.time / 1000)
        ServerTime.updateServerTime(date.time)
    }
}
