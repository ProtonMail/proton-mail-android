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

import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.api.models.RefreshBody
import ch.protonmail.android.api.segments.RESPONSE_CODE_GATEWAY_TIMEOUT
import ch.protonmail.android.api.segments.RESPONSE_CODE_TOO_MANY_REQUESTS
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.AppUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class BaseRequestInterceptorTest {

    private val tokenManagerMock = mockk<TokenManager> {
        every { createRefreshBody() } returns RefreshBody("refresh_token")
        every { handleRefresh(any()) } returns mockk<Unit>()
        every { authAccessToken } returns "auth_access_token"
        every { uid } returns "uid"
    }

    private val userManagerMock = mockk<UserManager> {
        every { username } answers {"testuser"}
        every { getTokenManager("testuser") } returns tokenManagerMock
        every { getMailboxPassword("testuser") } returns "mailbox password".toByteArray()
    }

    val interceptor = ProtonMailRequestInterceptor.getInstance(userManagerMock, mockk(), mockk())

    @Test
    fun gateway_timeout() {

        val responseMock = mockk<Response> {
            every { code() } returns RESPONSE_CODE_GATEWAY_TIMEOUT
        }

        assertNull(interceptor.checkIfTokenExpired(mockk(), mockk(), responseMock))
    }

    @Test
    fun too_many_requests() {

        val responseMock = mockk<Response> {
            every { code() } returns RESPONSE_CODE_TOO_MANY_REQUESTS
        }

        assertNull(interceptor.checkIfTokenExpired(mockk(), mockk(), responseMock))
    }

    @Test
    fun ok_response() {

        val responseMock = mockk<Response> {
            every { code() } returns 200
        }

        assertNull(interceptor.checkIfTokenExpired(mockk(), mockk(), responseMock))
    }

    @Test
    fun unauthorized_then_too_many_requests() {

        val chainMock = mockk<Interceptor.Chain> {
            every { request() } returns mockk {
                every { tag(RetrofitTag::class.java) } returns null
            }
        }
        val requestMock = mockk<Request> {
            every { url() } returns mockk {
                every { encodedPath() } returns "https://legit_url"
            }
        }
        val responseMock = mockk<Response> {
            every { code() } answers { 401 } // andThen RESPONSE_CODE_TOO_MANY_REQUESTS
        }

        interceptor.publicService = mockk {
            every { refreshSync(any(), any()) } returns mockk {
                every { execute() } returns mockk {
                    every { body() } answers { null } // refresh failed
                    every { code() } answers { RESPONSE_CODE_TOO_MANY_REQUESTS } // because it got 429
                }
            }
        }

        assertNull(interceptor.checkIfTokenExpired(chainMock, requestMock, responseMock))
    }

    @Test
    fun unauthorized_then_auth_okay_and_retry() {

        val chainMock = mockk<Interceptor.Chain> {
            every { request() } returns mockk {
                every { tag(RetrofitTag::class.java) } returns null
            }
            every { proceed(any()) } answers { mockk() }
        }
        val requestMock = mockk<Request> {
            every { url() } returns mockk {
                every { encodedPath() } returns "https://legit_url"
            }
            every { newBuilder() } returns mockk(relaxed = true)
        }
        val responseMock = mockk<Response> {
            every { code() } answers { 401 }
        }

        interceptor.publicService = mockk {
            every { refreshSync(any(), any()) } returns mockk {
                every { execute() } returns mockk {
                    every { body() } returns mockk {
                        // successful token refresh response
                        every { accessToken } returns "correct_access_token"
                    }
                    every { code() } answers { 200 }
                }
            }
        }

        assertNotNull(interceptor.checkIfTokenExpired(chainMock, requestMock, responseMock))
    }

    companion object {

        @BeforeClass
        @JvmStatic
        fun setup() {
            mockkStatic(AppUtil::class)
            every { AppUtil.postEventOnUi(any()) } answers { mockk<Void>() }
            every { AppUtil.getAppVersionName(any()) } answers { "app version name" }
            every { AppUtil.buildUserAgent() } answers { "user agent" }

            mockkStatic(ProtonMailApplication::class)
            every { ProtonMailApplication.getApplication().currentLocale } returns "current locale"
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            unmockkStatic(AppUtil::class)
            unmockkStatic(ProtonMailApplication::class)
        }
    }
}
