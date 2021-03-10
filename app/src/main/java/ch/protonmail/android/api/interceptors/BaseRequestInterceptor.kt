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

import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.api.models.doh.ProxyItem
import ch.protonmail.android.api.segments.AUTH_INFO_PATH
import ch.protonmail.android.api.segments.AUTH_PATH
import ch.protonmail.android.api.segments.HEADER_APP_VERSION
import ch.protonmail.android.api.segments.HEADER_AUTH
import ch.protonmail.android.api.segments.HEADER_LOCALE
import ch.protonmail.android.api.segments.HEADER_UID
import ch.protonmail.android.api.segments.HEADER_USER_AGENT
import ch.protonmail.android.api.segments.RESPONSE_CODE_GATEWAY_TIMEOUT
import ch.protonmail.android.api.segments.RESPONSE_CODE_SERVICE_UNAVAILABLE
import ch.protonmail.android.api.segments.RESPONSE_CODE_TOO_MANY_REQUESTS
import ch.protonmail.android.api.segments.RESPONSE_CODE_UNPROCESSABLE_ENTITY
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.RequestTimeoutEvent
import ch.protonmail.android.events.UnprocessableEntityEvent
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.JobManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber

// region constants
private const val TWENTY_FOUR_HOURS_IN_MILLIS = 24 * 60 * 60 * 1000L
// endregion

abstract class BaseRequestInterceptor(
    protected val userManager: UserManager,
    protected val jobManager: JobManager,
    protected val networkUtils: QueueNetworkUtil
) : Interceptor {

    private val appVersionName by lazy {
        val name = "Android_" + AppUtil.getAppVersionName(ProtonMailApplication.getApplication())
        if (name[name.length - 1] == '.') {
            name.substring(0, name.length - 1)
        } else {
            name
        }
    }

    private fun check24hExpired(): Boolean {
        val user = userManager.user
        val prefs = ProtonMailApplication.getApplication().defaultSharedPreferences
        val proxies = Proxies.getInstance(null, prefs)
        if (user.allowSecureConnectionsViaThirdParties && !user.usingDefaultApi) {
            if (proxies.proxyList.proxies.isNotEmpty()) {
                Timber.d("ProxyList is not empty")
                val proxy: ProxyItem? = try {
                    proxies.getCurrentActiveProxy()
                } catch (e: Exception) {
                    Timber.i(e, "getCurrentActiveProxy exception")
                    null
                }

                return if (proxy != null) { // if the proxy is not null, it's the current active proxy
                    Timber.d("Proxy active")
                    revertToOldApiIfNeeded(user, proxy.lastTrialTimestamp, false)
                } else { // if there aren't any active proxies, find the proxy with the most recent timestamp
                    Timber.d("ProxyList is null")
                    val latestProxy = proxies.proxyList.proxies.maxBy { it.lastTrialTimestamp }
                    revertToOldApiIfNeeded(user, latestProxy?.lastTrialTimestamp, true)
                }
            } else {
                // TODO: DoH: proxy list is empty, what to do here?
                Timber.d("DoH: proxy list is empty")
            }
        } else {
            // if user has not enabled third party, do nothing
        }
        return false
    }

    private fun revertToOldApiIfNeeded(user: User, timeStamp: Long?, force: Boolean): Boolean {
        if (force) {
            Timber.d("force switching to old api, since the new api is not available")
            networkUtils.networkConfigurator.networkSwitcher.reconfigureProxy(null) // force switch to old proxy
            user.usingDefaultApi = true
            return true
        }

        val currentTime = System.currentTimeMillis()
        val lastApiAttempt = timeStamp ?: currentTime // Proxies.getLastApiAttemptTimeStamp(prefs)
        val switchInterval = TWENTY_FOUR_HOURS_IN_MILLIS
        val timeDiff = kotlin.math.abs(currentTime - lastApiAttempt)
        if (timeDiff >= switchInterval) {
            Timber.d("time difference is greater than switch interval, switching to old API")
            networkUtils.networkConfigurator.networkSwitcher.reconfigureProxy(null) // force switch to old proxy
            user.usingDefaultApi = true
            return true
        }
        return false
    }

    /**
     * @return if returned null, no need to re-authorize or HTTP 504/429 happened (there's nothing we can do)
     */
    fun checkResponse(response: Response?): Response? {
        if (response == null) {
            return null
        }

        if (check24hExpired()) {
            return null
        }

        when {
            response.code() == RESPONSE_CODE_GATEWAY_TIMEOUT -> {
                Timber.d("'gateway timeout' when processing request")
                AppUtil.postEventOnUi(RequestTimeoutEvent())
            }
            response.code() == RESPONSE_CODE_TOO_MANY_REQUESTS -> {
                Timber.d("'too many requests' when processing request")
            }
            response.code() == RESPONSE_CODE_SERVICE_UNAVAILABLE -> { // 503
                Timber.d("'service unavailable' when processing request")
            }
            response.code() == RESPONSE_CODE_UNPROCESSABLE_ENTITY -> {
                Timber.d("'unprocessable entity' when processing request")
                var responseBodyError = response.message()
                try {
                    val responseBody = Gson().fromJson(response.peekBody(Long.MAX_VALUE).string(), ResponseBody::class.java)
                    responseBodyError = responseBody.error
                } catch (e: JsonSyntaxException) {
                    Timber.d(e)
                }
                AppUtil.postEventOnUi(UnprocessableEntityEvent(responseBodyError))
            }
        }
        return null
    }

    fun applyHeadersToRequest(request: Request): Request {

        val requestBuilder = request.newBuilder()
        val tokenManager = userManager.tokenManager
        // by default, we authorize requests using default user from UserManager
        if (tokenManager != null) {
            requestBuilder.header(HEADER_UID, tokenManager.uid)
            if (tokenManager.authAccessToken != null) {
                requestBuilder.header(HEADER_AUTH, tokenManager.authAccessToken!!)
            }
        }
        requestBuilder
            .header(HEADER_APP_VERSION, appVersionName)
            .header(HEADER_USER_AGENT, AppUtil.buildUserAgent())
            .header(HEADER_LOCALE, ProtonMailApplication.getApplication().currentLocale)

        // we have to remove UID from those requests, because they mess up with server recognizing affected user
        if (request.url().toString().endsWith(AUTH_PATH) || request.url().toString().contains(AUTH_INFO_PATH)) {
            requestBuilder.removeHeader(HEADER_AUTH).removeHeader(HEADER_UID)
        }

        // we customize auth headers if different than default user has to be authorized
        request.tag(RetrofitTag::class.java)?.also {
            if (it.usernameAuth == null) { // clear out default auth and unique session headers
                requestBuilder.removeHeader(HEADER_AUTH)
                requestBuilder.removeHeader(HEADER_UID)
            } else if (it.usernameAuth != tokenManager?.username) {
                // if it's the default user, credentials are already there
                val userTokenManager = userManager.getTokenManager(it.usernameAuth)
                userTokenManager?.let { manager ->
                    if (manager.authAccessToken != null) {
                        Timber.d("setting non-default auth headers for ${it.usernameAuth}")
                        requestBuilder.header(HEADER_AUTH, manager.authAccessToken!!)
                        requestBuilder.header(HEADER_UID, manager.uid)
                    }
                }
            }
        }

        return requestBuilder.build()
    }
}
