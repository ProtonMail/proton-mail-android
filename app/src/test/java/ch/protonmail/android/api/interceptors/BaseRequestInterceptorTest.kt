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
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.doh.PREF_DNS_OVER_HTTPS_API_URL_LIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_GATEWAY_TIMEOUT
import ch.protonmail.android.api.segments.RESPONSE_CODE_TOO_MANY_REQUESTS
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.notifier.UserNotifier
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.Assert.assertNull
import okhttp3.Response
import org.junit.After
import kotlin.test.BeforeTest
import kotlin.test.Test

class BaseRequestInterceptorTest {

    private val userMock = mockk<User> {
        every { username } returns "testuser"
        every { allowSecureConnectionsViaThirdParties } returns true
        every { usingDefaultApi } returns true
    }

    private val prefsMock = mockk<SharedPreferences> {
        every { getString(PREF_DNS_OVER_HTTPS_API_URL_LIST, null) } returns null
    }

    private val tokenManagerMock = mockk<TokenManager> {
        every { createRefreshBody() } returns RefreshBody("refresh_token")
        every { handleRefresh(any()) } returns mockk<Unit>()
        every { authAccessToken } returns "auth_access_token"
        every { uid } returns "uid"
    }

    private val userManagerMock = mockk<UserManager> {
        every { username } answers { "testuser" }
        every { getTokenManager("testuser") } returns tokenManagerMock
        every { getMailboxPassword("testuser") } returns "mailbox password".toByteArray()
        every { user } returns userMock
    }

    private val interceptor =
        ProtonMailRequestInterceptor.getInstance(userManagerMock, mockk(), mockk(), userNotifier)

    @BeforeTest
    fun setup() {
        mockkStatic(AppUtil::class)
        mockkStatic(ProtonMailApplication::class)
        every { AppUtil.postEventOnUi(any()) } answers { mockk<Void>() }
        every { AppUtil.getAppVersionName(any()) } answers { "app version name" }
        every { AppUtil.buildUserAgent() } answers { "user agent" }

        every { ProtonMailApplication.getApplication().currentLocale } returns "current locale"
        every {
            ProtonMailApplication.getApplication().defaultSharedPreferences
        } returns prefsMock
    }

    @After
    fun teardown() {
        unmockkStatic(AppUtil::class)
        unmockkStatic(ProtonMailApplication::class)
    }

    @Test
    fun verifyThatGatewayTimeoutResponseReturnsNullOutput() {
        // given
        val responseMock = mockk<Response> {
            every { code() } returns RESPONSE_CODE_GATEWAY_TIMEOUT
        }

        // when
        val checkIfTokenExpiredResponse = interceptor.checkResponse(responseMock)

        // then
        assertNull(checkIfTokenExpiredResponse)
    }

    @Test
    fun verifyThatTooManyRequestsResponseReturnsNullOutput() {
        // given
        val responseMock = mockk<Response> {
            every { code() } returns RESPONSE_CODE_TOO_MANY_REQUESTS
        }

        // when
        val checkIfTokenExpired = interceptor.checkResponse(responseMock)

        // then
        assertNull(checkIfTokenExpired)
    }

    @Test
    fun verifyThatOkResponseReturnsNullOutput() {
        // given
        every {
            ProtonMailApplication.getApplication().defaultSharedPreferences
        } returns prefsMock

        val responseMock = mockk<Response> {
            every { code() } returns 200
        }

        // when
        val checkIfTokenExpiredResponse = interceptor.checkResponse(responseMock)

        // then
        assertNull(checkIfTokenExpiredResponse)
    }

    @Test
    fun verifyThatUnprocessableEntityResponseShowsErrorToTheUser() {
        // given
        every {
            ProtonMailApplication.getApplication().defaultSharedPreferences
        } returns prefsMock

        val errorMessage = "Error - Reason for 422 response"
        val responseMock = mockk<Response> {
            every { code() } returns 422
            every { peekBody(any()).string() } returns "{ Error: \"$errorMessage\" }"
            every { message() } returns "HTTP status message"
        }

        // when
        interceptor.checkResponse(responseMock)

        // then
        verify { userNotifier.showError(errorMessage) }
    }

    companion object {
        // Since the SUT is a Singleton, we need to have a single instance of userNotifier mock
        // in order to perform assertions on it, otherwise verifications will fail as performed on the wrong instance.
        // That happens because the actual instance that will be in the SUT will always be the one passed when the
        // SUT was first created, which - unless this test runs first - may differ from the one that we're asserting on
        private val userNotifier = mockk<UserNotifier>(relaxed = true)
    }
}
