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

import android.content.Context
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.api.segments.HEADER_APP_VERSION
import ch.protonmail.android.api.segments.HEADER_AUTH
import ch.protonmail.android.api.segments.HEADER_LOCALE
import ch.protonmail.android.api.segments.HEADER_UID
import ch.protonmail.android.api.segments.HEADER_USER_AGENT
import ch.protonmail.android.api.segments.REFRESH_PATH
import ch.protonmail.android.api.segments.RESPONSE_CODE_TOO_MANY_REQUESTS
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.extensions.app
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.TagConstraint
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtonMailAuthenticator @Inject constructor(
    private val userManager: UserManager,
    private val jobManager: JobManager,
    private val appContext: Context
) : Authenticator {

    private val appVersionName by lazy {
        val name = "Android_" + BuildConfig.VERSION_NAME
        if (name[name.length - 1] == '.') {
            name.substring(0, name.length - 1)
        } else {
            name
        }
    }

    // api instance cannot be injected, due to a circular dependency

    @Throws(IOException::class)
    override fun authenticate(route: Route?, response: Response): Request? = refreshAuthToken(response)

    @Synchronized
    private fun refreshAuthToken(response: Response): Request? {
        val userId = response.request().tag(UserIdTag::class.java)?.id ?: userManager.requireCurrentUserId()
        val tokenManager = userManager.getTokenManagerBlocking(userId)

        val originalRequest = response.request()
        if (originalRequest.header(HEADER_AUTH) != tokenManager.authAccessToken) {
            // update request with new token
            return updateRequestHeaders(tokenManager, originalRequest)
        } else {
            if (response.priorResponse() != null) { // NOT NULL -> triggered by automatic retry,
                // requests triggered by auto-retry will be discarded for refreshing tokens
                return null
            } else {
                Timber.i("tokens need refreshing")
            }
        }

        if (!originalRequest.url().encodedPath().contains(REFRESH_PATH)) {
            val refreshBody = tokenManager.createRefreshBody()
            val refreshResponse =
                appContext.app.api.refreshAuthBlocking(refreshBody, RetrofitTag(usernameAuth))
            if (refreshResponse.error.isNullOrEmpty() && refreshResponse.accessToken != null) {
                Timber.i(
                    "access token expired: got correct refresh response, handle refresh in token manager"
                )
                tokenManager.handleRefresh(refreshResponse)
            } else {
                return if (refreshResponse.code == RESPONSE_CODE_TOO_MANY_REQUESTS) {
                    Timber.i(
                        "access token expired: got 429 response trying to refresh it, quitting flow"
                    )
                    null
                } else {
                    Timber.i(
                        "access token expired: " +
                            "got error response (${refreshResponse.code}) trying to refresh it: " +
                            "(refresh token blank = ${tokenManager.isRefreshTokenBlank()}, " +
                            "uid blank = ${tokenManager.isUidBlank()}), logging out"
                    )
                    appContext.app.notifyLoggedOut(usernameAuth)
                    jobManager.stop()
                    jobManager.clear()
                    jobManager.cancelJobsInBackground(null, TagConstraint.ALL)
                    userManager.logoutOfflineBlocking(userId)
                    // return null since we're logging out
                    null
                }
            }

            // update request with new token
            if (tokenManager.authAccessToken != null) {
                Timber.d("access token expired, updating request with new token")
                return updateRequestHeaders(tokenManager, originalRequest)
            } else {
                Timber.tag("429").i(
                    "access token expired: " +
                        "updating request without the token (should not happen!) " +
                        "and uid blank? ${tokenManager.isUidBlank()}"
                )
                return originalRequest.newBuilder()
                    .header(HEADER_UID, tokenManager.uid)
                    .header(HEADER_APP_VERSION, appVersionName)
                    .header(HEADER_USER_AGENT, AppUtil.buildUserAgent())
                    .header(HEADER_LOCALE, appContext.app.currentLocale)
                    .build()
            }
        } else { // if received 401 error while refreshing access token, send event to logout user
            Timber.tag("429").i(
                "access token expired: got 401 while trying to refresh it " +
                    "(refresh token blank = ${tokenManager.isRefreshTokenBlank()}, " +
                    "uid blank = ${tokenManager.isUidBlank()})"
            )
            appContext.app.notifyLoggedOut(usernameAuth)
            userManager.logoutOffline(usernameAuth)
            return null
        }
    }

    private fun updateRequestHeaders(tokenManager: TokenManager, originalRequest: Request): Request {
        return originalRequest.newBuilder()
            .header(HEADER_AUTH, tokenManager.authAccessToken!!)
            .header(HEADER_UID, tokenManager.uid)
            .header(HEADER_APP_VERSION, appVersionName)
            .header(HEADER_USER_AGENT, AppUtil.buildUserAgent())
            .header(HEADER_LOCALE, appContext.app.currentLocale)
            .build()
    }
}
