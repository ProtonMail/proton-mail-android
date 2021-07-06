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
import ch.protonmail.android.utils.notifier.UserNotifier
import com.birbit.android.jobqueue.JobManager
import me.proton.core.accountmanager.domain.SessionManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

/**
 * ProtonMailRequestInterceptor intercepts every request and check response.
 */
class ProtonMailRequestInterceptor private constructor(
    userManager: UserManager,
    jobManager: JobManager,
    networkUtil: QueueNetworkUtil,
    userNotifier: UserNotifier,
    sessionManager: SessionManager,
) : BaseRequestInterceptor(userManager, jobManager, networkUtil, userNotifier, sessionManager) {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = getRequestWithHeaders(chain.request())
        try {
            Timber.d("Intercept: advancing request with url: " + request.url)
            requestCount++
            val response: Response = chain.proceed(request)

            networkUtils.setCurrentlyHasConnectivity()

            // check validity of response (DoH expiration and error codes)
            return checkResponse(response, chain)

        } catch (exception: IOException) {
            Timber.d(exception, "Intercept: IOException with url: " + request.url)
            networkUtils.retryPingAsPreviousRequestWasInconclusive()
            throw exception
        } finally {
            requestCount--
        }
    }

    companion object {

        private var instance: ProtonMailRequestInterceptor? = null

        var requestCount = 0

        fun getInstance(
            userManager: UserManager,
            jobManager: JobManager,
            networkUtil: QueueNetworkUtil,
            userNotifier: UserNotifier,
            sessionManager: SessionManager
        ): ProtonMailRequestInterceptor = synchronized(this) {
            if (instance == null) {
                instance = ProtonMailRequestInterceptor(
                    userManager,
                    jobManager,
                    networkUtil,
                    userNotifier,
                    sessionManager
                )
            }
            instance!!
        }
    }
}
