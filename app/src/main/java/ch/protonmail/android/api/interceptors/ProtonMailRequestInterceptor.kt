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
package ch.protonmail.android.api.interceptors

import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.ConnectivityEvent
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.notifier.UserNotifier
import com.birbit.android.jobqueue.JobManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

/**
 * ProtonMailRequestInterceptor intercepts every request and if HTTP response status is 401
 * It try to refresh token and make original request again with refreshed access token
 */
class ProtonMailRequestInterceptor private constructor(
    userManager: UserManager,
    jobManager: JobManager,
    networkUtil: QueueNetworkUtil,
    userNotifier: UserNotifier
) : BaseRequestInterceptor(userManager, jobManager, networkUtil, userNotifier) {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {

        val request: Request = applyHeadersToRequest(chain.request())

        // try the request
        var response: Response? = null
        try {
            requestCount++
            Timber.d("Intercept: advancing request with url: " + request.url())
            response = chain.proceed(request)

        } catch (exception: IOException) {
            Timber.d(exception, "Intercept: IOException with url: " + request.url())
            networkUtils.retryPingAsPreviousRequestWasInconclusive()
        }

        requestCount--
        if (response == null) {
            return chain.proceed(request)
        } else {
            networkUtils.setCurrentlyHasConnectivity()
            AppUtil.postEventOnUi(ConnectivityEvent(true))
        }

        // check validity of response (DoH expiration and error codes)
        checkResponse(response)
        return response
    }

    companion object {

        var requestCount = 0

        @Volatile
        private var INSTANCE: ProtonMailRequestInterceptor? = null

        fun getInstance(
            userManager: UserManager,
            jobManager: JobManager,
            networkUtil: QueueNetworkUtil,
            userNotifier: UserNotifier
        ): ProtonMailRequestInterceptor =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: buildInstance(userManager, jobManager, networkUtil, userNotifier).also { INSTANCE = it }
            }

        private fun buildInstance(
            userManager: UserManager,
            jobManager: JobManager,
            networkUtil: QueueNetworkUtil,
            userNotifier: UserNotifier
        ) = ProtonMailRequestInterceptor(userManager, jobManager, networkUtil, userNotifier)

    }
}
