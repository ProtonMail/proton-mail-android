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

import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.ConnectivityEvent
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.Logger
import com.birbit.android.jobqueue.JobManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException

/**
 * ProtonMailRequestInterceptor intercepts every request and if HTTP response status is 401
 * It try to refresh token and make original request again with refreshed access token
 */

// region constants
private const val FIVE_SECONDS_IN_MILLIS = 5000L
private const val TAG = "ProtonMailRequestInterceptor"
// endregion
class ProtonMailRequestInterceptor private constructor(
        userManager: UserManager,
        jobManager: JobManager,
        networkUtil: QueueNetworkUtil
): BaseRequestInterceptor(userManager, jobManager, networkUtil) {

    var previousInterceptTime = System.currentTimeMillis() - FIVE_SECONDS_IN_MILLIS
    val prefs = ProtonMailApplication.getApplication().defaultSharedPreferences

    companion object {
        @Volatile private var INSTANCE: ProtonMailRequestInterceptor? = null

        fun getInstance(userManager: UserManager, jobManager: JobManager, networkUtil: QueueNetworkUtil):
                ProtonMailRequestInterceptor =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildInstance(userManager, jobManager, networkUtil).also { INSTANCE = it }
                }

        private fun buildInstance(userManager: UserManager, jobManager: JobManager, networkUtil: QueueNetworkUtil) =
                ProtonMailRequestInterceptor(userManager, jobManager, networkUtil)

    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {

        val request: Request = applyHeadersToRequest(chain.request())

        // try the request
        var response: Response? = null
        try {

            Logger.doLog(TAG,  "Intercept: advancing request with url: " + request.url())
            // val requestUrl = request.url().toString()
            // response = if (requestUrl.contains(Constants.ENDPOINT_URI) && prefs.getBoolean("pref_doh_ongoing", false)) {
            //     Logger.doLog(TAG, "Intercept: stifling request because it's using old api and doh is ongoing")
            //     null
            // } else {
                response = chain.proceed(request)
            // }

        } catch (exception: IOException) {
            // checkForProxy()
            Logger.doLog(TAG,  "Intercept: IOException with url: " + request.url())
            // val currentInterceptTime = System.currentTimeMillis()
            // if(currentInterceptTime - previousInterceptTime >= FIVE_SECONDS_IN_MILLIS) {
            //     Logger.doLog(TAG, "Time difference between caught requests is greater than 5 seconds. Updating times and posting on UI...")
                AppUtil.postEventOnUi(ConnectivityEvent(false))
            // }
            // previousInterceptTime = currentInterceptTime
            networkUtils.setCurrentlyHasConnectivity(false)
            // exception.printStackTrace() // TODO: should we pollute the logcat?
        } catch (exception: ConnectException) {
            Logger.doLog(TAG, "Intercept: ConnectException")
            exception.printStackTrace()
        } catch (exception: Exception) {
            Logger.doLog(TAG, "Intercept: Exception")
            exception.printStackTrace()
        }

        if (response == null) {
            return chain.proceed(request)
        } else {
            networkUtils.setCurrentlyHasConnectivity(true)
            AppUtil.postEventOnUi(ConnectivityEvent(true))
        }

        // check if expired token, otherwise just pass the original response on
        val reAuthResponse = checkIfTokenExpired(chain, request, response)

        return reAuthResponse ?: response
    }
}
