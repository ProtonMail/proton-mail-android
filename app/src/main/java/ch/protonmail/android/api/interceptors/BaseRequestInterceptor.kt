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

import androidx.preference.PreferenceManager
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.api.models.doh.ProxyItem
import ch.protonmail.android.api.segments.HEADER_APP_VERSION
import ch.protonmail.android.api.segments.HEADER_LOCALE
import ch.protonmail.android.api.segments.HEADER_UID
import ch.protonmail.android.api.segments.HEADER_USER_AGENT
import ch.protonmail.android.api.segments.RESPONSE_CODE_AUTH_AUTH_ACCOUNT_DELETED
import ch.protonmail.android.api.segments.RESPONSE_CODE_AUTH_AUTH_ACCOUNT_DISABLED
import ch.protonmail.android.api.segments.RESPONSE_CODE_AUTH_AUTH_ACCOUNT_FAILED_GENERIC
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_VERIFICATION_NEEDED
import ch.protonmail.android.api.segments.RESPONSE_CODE_GATEWAY_TIMEOUT
import ch.protonmail.android.api.segments.RESPONSE_CODE_MESSAGE_READING_RESTRICTED
import ch.protonmail.android.api.segments.RESPONSE_CODE_NOT_ALLOWED
import ch.protonmail.android.api.segments.RESPONSE_CODE_OLD_PASSWORD_INCORRECT
import ch.protonmail.android.api.segments.RESPONSE_CODE_SERVICE_UNAVAILABLE
import ch.protonmail.android.api.segments.RESPONSE_CODE_TOO_MANY_REQUESTS
import ch.protonmail.android.api.segments.RESPONSE_CODE_UNAUTHORIZED
import ch.protonmail.android.api.segments.RESPONSE_CODE_UNPROCESSABLE_ENTITY
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.RequestTimeoutEvent
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.notifier.UserNotifier
import com.birbit.android.jobqueue.JobManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.runBlocking
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import timber.log.Timber
import java.io.IOException

// region constants
private const val TWENTY_FOUR_HOURS_IN_MILLIS = 24 * 60 * 60 * 1000L
// endregion

abstract class BaseRequestInterceptor(
    protected val userManager: UserManager,
    protected val jobManager: JobManager,
    protected val networkUtils: QueueNetworkUtil,
    protected val userNotifier: UserNotifier,
    protected val sessionManager: SessionManager
) : Interceptor {

    private val appVersionName by lazy {
        val name = "Android_" + BuildConfig.VERSION_NAME
        if (name[name.length - 1] == '.') {
            name.substring(0, name.length - 1)
        } else {
            name
        }
    }

    private fun check24hExpired(): Boolean {
        val user = userManager.currentLegacyUser
        val prefs = PreferenceManager.getDefaultSharedPreferences(ProtonMailApplication.getApplication())
        val proxies = Proxies.getInstance(null, prefs)
        if (user != null && user.allowSecureConnectionsViaThirdParties && !user.usingDefaultApi) {
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

    fun checkResponse(response: Response, chain: Interceptor.Chain): Response {
        if (check24hExpired()) {
            return response
        }

        when (response.code) {
            RESPONSE_CODE_UNAUTHORIZED -> {
                Timber.d("'unauthorized' when processing request")
                val uid = response.request.header(HEADER_UID)
                val sessionId = uid?.let { SessionId(it) }
                if (sessionId != null) {
                    val refresh = runCatching {
                        // Execute a request that is handled by Core Network (ApiProvider/SessionProvider).
                        // This will start a refresh session tokens procedure (mutual exclusive).
                        runBlocking { sessionManager.refreshScopes(sessionId) }
                    }
                    if (refresh.isSuccess) {
                        response.closeQuietly()
                        val newRequest: Request = getRequestWithHeaders(response.request)
                        return chain.proceed(newRequest)
                    }
                }
            }
            RESPONSE_CODE_GATEWAY_TIMEOUT -> {
                Timber.d("'gateway timeout' when processing request")
                AppUtil.postEventOnUi(RequestTimeoutEvent())
            }
            RESPONSE_CODE_TOO_MANY_REQUESTS -> {
                Timber.d("'too many requests' when processing request")
            }
            RESPONSE_CODE_SERVICE_UNAVAILABLE -> { // 503
                Timber.d("'service unavailable' when processing request")
                networkUtils.retryPingAsPreviousRequestWasInconclusive()
            }
            RESPONSE_CODE_UNPROCESSABLE_ENTITY -> {
                Timber.d("'unprocessable entity' when processing request")
                var responseBodyError: String? = response.message
                var responseBody: ResponseBody? = null
                try {
                    responseBody = Gson().fromJson(
                        response.peekBody(Long.MAX_VALUE).string(),
                        ResponseBody::class.java
                    )
                    responseBodyError = responseBody.error
                } catch (e: JsonSyntaxException) {
                    Timber.d(e, "response had more bytes than MAX_VALUE")
                } catch (e: IOException) {
                    Timber.d(e)
                }
                // when we introduced global handling of 422 http error code it resulted with toast
                // showing up in places where we have questionable behaviours that don't really cause
                // issues for the end user. And sadly the errors provided by the API weren't of any
                // particular use to the end user. So for now we show only the errors for this error codes.
                if (responseBody?.code in arrayOf(
                        RESPONSE_CODE_MESSAGE_READING_RESTRICTED,
                        RESPONSE_CODE_OLD_PASSWORD_INCORRECT,
                        RESPONSE_CODE_ERROR_VERIFICATION_NEEDED,
                        RESPONSE_CODE_NOT_ALLOWED,
                        RESPONSE_CODE_AUTH_AUTH_ACCOUNT_FAILED_GENERIC,
                        RESPONSE_CODE_AUTH_AUTH_ACCOUNT_DELETED,
                        RESPONSE_CODE_AUTH_AUTH_ACCOUNT_DISABLED
                    )
                ) {
                    responseBodyError?.let { userNotifier.showError(it) }
                }
            }
        }
        return response
    }

    fun getRequestWithHeaders(request: Request): Request {
        val requestBuilder = request.newBuilder()

        requestBuilder.setClientHeaders()

        val tagUserId = request.tag(UserIdTag::class.java)?.id
        val currentUserId = userManager.currentUserId

        when {
            tagUserId != null -> requestBuilder.setSessionHeadersFor(tagUserId)
            currentUserId != null -> requestBuilder.setSessionHeadersFor(currentUserId)
        }
        return requestBuilder.build()
    }

    private fun Request.Builder.setClientHeaders() {
        header(HEADER_APP_VERSION, appVersionName)
        header(HEADER_USER_AGENT, AppUtil.buildUserAgent())
        header(HEADER_LOCALE, ProtonMailApplication.getApplication().currentLocale)
    }

    private fun Request.Builder.setSessionHeadersFor(userId: UserId) {
        val session = runBlocking { sessionManager.getSessionId(userId)?.let { sessionManager.getSession(it) } }
        if (session != null) setSessionHeaders(session)
    }
}
