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

import ch.protonmail.android.api.ProgressListener
import ch.protonmail.android.api.ProgressResponseBody
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.ConnectivityEvent
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.JobManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.Semaphore

class ProtonMailAttachmentRequestInterceptor private constructor(
    userManager: UserManager,
    jobManager: JobManager,
    networkUtil: QueueNetworkUtil
) : BaseRequestInterceptor(userManager, jobManager, networkUtil) {

    private var progressListener: ProgressListener? = null
    private var semaphore: Semaphore? = null

    fun nextProgressListener(progressListener: ProgressListener) {
        // If there already is a previous semaphore acquire so we know progressListener has been attached
        this.semaphore?.acquire()
        this.progressListener = progressListener
        this.semaphore = Semaphore(0)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {

        val request: Request = applyHeadersToRequest(chain.request())

        // try the request
        val response: Response?
        try {
            response = chain.proceed(request)
        } catch (exception: IOException) {
            AppUtil.postEventOnUi(ConnectivityEvent(false))
            networkUtils.setConnectivityHasFailed(exception)
            throw exception
        }

        networkUtils.setCurrentlyHasConnectivity()
        AppUtil.postEventOnUi(ConnectivityEvent(true))

        // check validity of response (DoH expiration and error codes)
        checkResponse(response)

        // for concurrency
        val progressListener = progressListener
        // otherwise just pass the original response on
        return if (progressListener != null) {
            val responseWithListener =
                response.newBuilder().body(ProgressResponseBody(response.body()!!, progressListener)).build()
            this.semaphore!!.release()
            this.progressListener = null
            this.semaphore = null
            responseWithListener
        } else {
            response
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ProtonMailAttachmentRequestInterceptor? = null

        val prefs = ProtonMailApplication.getApplication().defaultSharedPreferences

        fun getInstance(
            userManager: UserManager,
            jobManager: JobManager,
            networkUtil: QueueNetworkUtil
        ): ProtonMailAttachmentRequestInterceptor =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: buildInstance(/* publicService, */ userManager, jobManager, networkUtil).also { INSTANCE = it }
            }

        private fun buildInstance(
            userManager: UserManager,
            jobManager: JobManager,
            networkUtil: QueueNetworkUtil
        ) = ProtonMailAttachmentRequestInterceptor(userManager, jobManager, networkUtil)
    }
}
