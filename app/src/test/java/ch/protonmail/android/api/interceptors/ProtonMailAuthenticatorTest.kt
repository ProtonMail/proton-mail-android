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
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.JobManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import junit.framework.Assert
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProtonMailAuthenticatorTest {

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
        every { isRefreshTokenBlank() } returns false
        every { isUidBlank() } returns false
    }

    private val userManagerMock = mockk<UserManager> {
        every { username } answers { "testuser" }
        every { getTokenManager("testuser") } returns tokenManagerMock
        every { getMailboxPassword("testuser") } returns "mailbox password".toByteArray()
        every { user } returns userMock
        every { logoutOffline("testuser")} just runs
    }

    private val jobManagerMock = mockk<JobManager> (relaxed = true)

    private val authenticator =
        ProtonMailAuthenticator.getInstance(userManagerMock, jobManagerMock, mockk())



    @Before
    fun setup() {
        mockkStatic(AppUtil::class)
        mockkStatic(ProtonMailApplication::class)
        MockKAnnotations.init(this)
        every { AppUtil.postEventOnUi(any()) } answers { mockk<Void>() }
        every { AppUtil.getAppVersionName(any()) } answers { "app version name" }
        every { AppUtil.buildUserAgent() } answers { "user agent" }

        every { ProtonMailApplication.getApplication().currentLocale } returns "current locale"
        every {
            ProtonMailApplication.getApplication().defaultSharedPreferences
        } returns prefsMock

        every { ProtonMailApplication.getApplication().notifyLoggedOut("testuser") } just runs
    }

    @After
    fun teardown() {
        unmockkStatic(AppUtil::class)
        unmockkStatic(ProtonMailApplication::class)
    }

    @Test
    fun verifyThatUnauthorisedWithAutoRetryCancelsRequest() {
        // given
        val requestMock = mockk<Request> {
            every { tag(RetrofitTag::class.java) } returns null
            every { header("Authorization") } returns "auth_access_token"
            every { url() } returns mockk {
                every { encodedPath() } returns "https://legit_url"
            }
        }
        val priorResponse = mockk<Response>(relaxed = true)
        val responseMock = mockk<Response> {
            every { request() } answers { requestMock }
            every { priorResponse() } answers { priorResponse }
        }

        // when
        val unauthorizedWithAutoRetryResultRequest = authenticator.refreshAuthToken(mockk(), responseMock)

        // then
        Assert.assertNull(unauthorizedWithAutoRetryResultRequest)
    }

    @Test
    fun verifyThatUnauthorisedFollowedByTooManyRequestsErrorCancelsRequest() {
        // given
        val requestMock = mockk<Request> {
            every { tag(RetrofitTag::class.java) } returns null
            every { header("Authorization") } returns "auth_access_token"
            every { url() } returns mockk {
                every { encodedPath() } returns "https://legit_url"
            }
        }
        val responseMock = mockk<Response> {
            every { request() } answers { requestMock }
            every { priorResponse() } answers { null }
        }

        authenticator.publicService = mockk {
            every { refreshSyncBlocking(any(), any()) } returns mockk {
                every { execute() } returns mockk {
                    every { body() } answers { null } // refresh failed
                    every { code() } answers { ch.protonmail.android.api.segments.RESPONSE_CODE_TOO_MANY_REQUESTS }
                }
            }
        }

        // when
        val checkIfTokenExpiredResponse = authenticator.refreshAuthToken(mockk(), responseMock)

        // then
        Assert.assertNull(checkIfTokenExpiredResponse)
    }

    @Test
    fun verifyThatUnauthorisedFollowedByUnauthorisedErrorCancelsRequest() {
        // given
        val requestMock = mockk<Request> {
            every { tag(RetrofitTag::class.java) } returns null
            every { header("Authorization") } returns "auth_access_token"
            every { url() } returns mockk {
                every { encodedPath() } returns "https://legit_url/auth/refresh"
            }
        }
        val responseMock = mockk<Response> {
            every { request() } answers { requestMock }
            every { priorResponse() } answers { null }
        }

        authenticator.publicService = mockk {
            every { refreshSyncBlocking(any(), any()) } returns mockk {
                every { execute() } returns mockk {
                    every { body() } answers { null } // refresh failed
                    every { code() } answers { ch.protonmail.android.api.segments.RESPONSE_CODE_UNAUTHORIZED }
                }
            }
        }

        // when
        val checkIfTokenExpiredResponse = authenticator.refreshAuthToken(mockk(), responseMock)

        // then
        Assert.assertNull(checkIfTokenExpiredResponse)
    }

    @Test
    fun verifyThatUnauthorisedFollowedByGatewayTimeoutErrorCancelsRequest() {
        // given
        val requestMock = mockk<Request> {
            every { tag(RetrofitTag::class.java) } returns null
            every { header("Authorization") } returns "auth_access_token"
            every { url() } returns mockk {
                every { encodedPath() } returns "https://legit_url"
            }
        }
        val responseMock = mockk<Response> {
            every { request() } answers { requestMock }
            every { priorResponse() } answers { null }
        }

        authenticator.publicService = mockk {
            every { refreshSyncBlocking(any(), any()) } returns mockk {
                every { execute() } returns mockk {
                    every { body() } answers { null } // refresh failed
                    every { code() } answers { ch.protonmail.android.api.segments.RESPONSE_CODE_GATEWAY_TIMEOUT }
                }
            }
        }

        // when
        val checkIfTokenExpiredResponse = authenticator.refreshAuthToken(mockk(), responseMock)

        // then
        Assert.assertNull(checkIfTokenExpiredResponse)
    }

    @Test
    fun verifyThatAfterReceivingUnauthorisedTokenRetryReturnsNewResponse() {

        // given
        val requestMock = mockk<Request> {
            every { tag(RetrofitTag::class.java) } returns null
            every { header("Authorization") } returns "auth_access_token"
            every { url() } returns mockk {
                every { encodedPath() } returns "https://legit_url"
            }
            every { newBuilder() } returns mockk(relaxed = true)
        }

        val responseMock = mockk<Response> {
            every { request() } answers { requestMock }
            every { priorResponse() } answers { null }
        }

        authenticator.publicService = mockk {
            every { refreshSyncBlocking(any(), any()) } returns mockk {
                every { execute() } returns mockk {
                    every { body() } returns mockk {
                        // successful token refresh response
                        every { accessToken } returns "correct_access_token"
                    }
                    every { code() } answers { 200 }
                }
            }
        }

        // when
        val updatedRequest = authenticator.refreshAuthToken(mockk(), responseMock)

        // then
        Assert.assertNotNull(updatedRequest)
    }
}
