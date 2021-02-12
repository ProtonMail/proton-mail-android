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

import android.content.SharedPreferences
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.api.models.RefreshBody
import ch.protonmail.android.api.models.RefreshResponse
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.doh.PREF_DNS_OVER_HTTPS_API_URL_LIST
import ch.protonmail.android.api.segments.HEADER_AUTH
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.JobManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

const val AUTH_ACCESS_TOKEN = "auth_access_token"
const val TEST_URL = "https://legit_url"
const val TEST_USERNAME = "testuser"
const val NON_NULL_ERROR_MESSAGE = "non null error message"

class ProtonMailAuthenticatorTest {

    private val userMock = mockk<User> {
        every { username } returns TEST_USERNAME
        every { allowSecureConnectionsViaThirdParties } returns true
        every { usingDefaultApi } returns true
    }

    private val prefsMock = mockk<SharedPreferences> {
        every { getString(PREF_DNS_OVER_HTTPS_API_URL_LIST, null) } returns null
    }

    private val tokenManagerMock = mockk<TokenManager> {
        every { createRefreshBody() } returns RefreshBody("refresh_token")
        every { handleRefresh(any()) } returns mockk()
        every { authAccessToken } returns AUTH_ACCESS_TOKEN
        every { uid } returns "uid"
        every { isRefreshTokenBlank() } returns false
        every { isUidBlank() } returns false
    }

    private val userManagerMock = mockk<UserManager> {
        every { username } answers { TEST_USERNAME }
        every { getTokenManager(TEST_USERNAME) } returns tokenManagerMock
        every { getMailboxPassword(TEST_USERNAME) } returns "mailbox password".toByteArray()
        every { user } returns userMock
        every { logoutOffline(TEST_USERNAME) } just runs
    }

    private val jobManagerMock = mockk<JobManager> (relaxed = true)

    private val authenticator =
        ProtonMailAuthenticator(userManagerMock, jobManagerMock)

    @Before
    fun setup() {
        mockkStatic(AppUtil::class)
        mockkStatic(ProtonMailApplication::class)
        MockKAnnotations.init(this)
        every { AppUtil.postEventOnUi(any()) } answers { mockk<Void>() }
        every { AppUtil.buildUserAgent() } answers { "user agent" }

        every { ProtonMailApplication.getApplication().currentLocale } returns "current locale"
        every {
            ProtonMailApplication.getApplication().defaultSharedPreferences
        } returns prefsMock

        every { ProtonMailApplication.getApplication().notifyLoggedOut(TEST_USERNAME) } just runs
    }

    @After
    fun teardown() {
        unmockkStatic(AppUtil::class)
        unmockkStatic(ProtonMailApplication::class)
    }

    @Test
    fun verifyThatUnauthorisedWithAutoRetryCancelsRequest() {
        // given
        val request = Request.Builder()
            .tag(null)
            .header(HEADER_AUTH, AUTH_ACCESS_TOKEN)
            .url(TEST_URL)
            .build()
        val priorResponse = mockk<Response>(relaxed = true)
        val responseMock = mockk<Response> {
            every { request() } answers { request }
            every { priorResponse() } answers { priorResponse }
        }

        // when
        val unauthorizedWithAutoRetryResultRequest = authenticator.authenticate(mockk(), responseMock)

        // then
        assertNull(unauthorizedWithAutoRetryResultRequest)
    }

    @Test
    fun verifyThatUnauthorisedFollowedByTooManyRequestsErrorCancelsRequest() {
        // given
        val request = Request.Builder()
            .tag(null)
            .header(HEADER_AUTH, AUTH_ACCESS_TOKEN)
            .url(TEST_URL)
            .build()
        val responseMock = mockk<Response> {
            every { request() } answers { request }
            every { priorResponse() } answers { null }
        }

        val authResponseMock = mockk<RefreshResponse> {
            every { code } answers { ch.protonmail.android.api.segments.RESPONSE_CODE_TOO_MANY_REQUESTS }
            every { error } answers { NON_NULL_ERROR_MESSAGE }
        }

        every {
            ProtonMailApplication.getApplication().api.refreshAuthBlocking(any(), any())
        } returns authResponseMock

        // when
        val tooManyErrorsRequest = authenticator.authenticate(mockk(), responseMock)

        // then
        assertNull(tooManyErrorsRequest)
    }

    @Test
    fun verifyThatUnauthorisedFollowedByUnauthorisedErrorCancelsRequest() {
        // given
        val request = Request.Builder()
            .tag(null)
            .header(HEADER_AUTH, AUTH_ACCESS_TOKEN)
            .url("$TEST_URL/auth/refresh")
            .build()

        val responseMock = mockk<Response> {
            every { request() } answers { request }
            every { priorResponse() } answers { null }
        }

        val authResponseMock = mockk<RefreshResponse> {
            every { code } answers { ch.protonmail.android.api.segments.RESPONSE_CODE_UNAUTHORIZED }
            every { error } answers { NON_NULL_ERROR_MESSAGE }
        }

        every {
            ProtonMailApplication.getApplication().api.refreshAuthBlocking(any(), any())
        } returns authResponseMock

        // when
        val subsequentUnauthorisedRequest = authenticator.authenticate(mockk(), responseMock)

        // then
        assertNull(subsequentUnauthorisedRequest)
    }

    @Test
    fun verifyThatUnauthorisedFollowedByGatewayTimeoutErrorCancelsRequest() {
        // given
        val request = Request.Builder()
            .tag(null)
            .header(HEADER_AUTH, "auth_access_token")
            .url("https://legit_url")
            .build()

        val responseMock = mockk<Response> {
            every { request() } answers { request }
            every { priorResponse() } answers { null }
        }

        val authResponseMock = mockk<RefreshResponse> {
            every { code } answers { ch.protonmail.android.api.segments.RESPONSE_CODE_GATEWAY_TIMEOUT }
            every { error } answers { NON_NULL_ERROR_MESSAGE }
        }

        every {
            ProtonMailApplication.getApplication().api.refreshAuthBlocking(any(), any())
        } returns authResponseMock

        // when
        val gatewayTimeoutRequest = authenticator.authenticate(mockk(), responseMock)

        // then
        assertNull(gatewayTimeoutRequest)
    }

    @Test
    fun verifyThatAfterReceivingUnauthorisedTokenRetryReturnsNewResponse() {

        // given
        val request = Request.Builder()
            .tag(null)
            .header(HEADER_AUTH, AUTH_ACCESS_TOKEN)
            .url(TEST_URL)
            .build()

        val responseMock = mockk<Response> {
            every { request() } answers { request }
            every { priorResponse() } answers { null }
        }

        val authResponseMock = mockk<RefreshResponse> {
            every { code } answers { 200 }
            every { error } answers { "" }
            every { accessToken } answers { "correct_access_token" }
        }

        every {
            ProtonMailApplication.getApplication().api.refreshAuthBlocking(any(), any())
        } returns authResponseMock

        // when
        val updatedRequest = authenticator.authenticate(mockk(), responseMock)

        // then
        assertNotNull(updatedRequest)
    }
}
