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
package ch.protonmail.android

import android.text.TextUtils
import androidx.test.filters.LargeTest
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.api.models.*
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.crypto.Crypto
import ch.protonmail.android.utils.crypto.OpenPGP
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@LargeTest
internal class TokenManagerTest {

    private val openPgp = OpenPGP()

    val username = "username"
    val accessToken = "9c196c6e621ddc7bd55609f274e6174ef7fe2e00"
    val accessTokenRefreshed = "9c196c6e621ddc7bd55609f274e6174ef7fe2e00_refreshed"
    val refreshToken = "7790e2067c2a9701745805672eb34fb94c67ac83"
    val refreshTokenRefreshed = "7790e2067c2a9701745805672eb34fb94c67ac83_refreshed"
    val tokenType = "Bearer"
    val loginResponse: LoginResponse
    val refreshResponse: RefreshResponse

    init {
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } returns false
        every { TextUtils.isEmpty(null) } returns true
        every { TextUtils.isEmpty("") } returns true

        loginResponse = mockk()
        every { loginResponse.accessToken } returns accessToken
        every { loginResponse.passwordMode } returns Constants.PasswordMode.SINGLE
        every { loginResponse.refreshToken } returns refreshToken
        every { loginResponse.tokenType } returns tokenType
        every { loginResponse.uid } returns "02db5fe7b95506438326ea6c036f1b24b2914eba"
        every { loginResponse.scope } returns "full self organization payments keys paid nondelinquent mail"
        every { loginResponse.keySalt } returns null
        every { loginResponse.isAccessTokenArmored } returns false
        every { loginResponse.privateKey } returns null

        refreshResponse = mockk()
        every { refreshResponse.accessToken } returns accessTokenRefreshed
        every { refreshResponse.refreshToken } returns refreshTokenRefreshed
        every { refreshResponse.scope } returns "full self organization payments keys paid nondelinquent mail"
        every { refreshResponse.isAccessTokenArmored } returns false
        every { refreshResponse.privateKey } returns null
    }

    @Test
    fun handle_login() {
        val tokenManager = TokenManager.getInstance("user for access token", openPgp)

        tokenManager!!.handleLogin(loginResponse)

        assertEquals("$tokenType $accessToken", tokenManager.authAccessToken)
        assertEquals(refreshToken, tokenManager.createRefreshBody().refreshToken)
    }

    @Test
    fun clear_access_token() {
        val tokenManager = TokenManager.getInstance(username, openPgp)

        tokenManager!!.handleLogin(loginResponse)

        tokenManager.clearAccessToken()
        assertNull(tokenManager.authAccessToken)
        assertFalse(tokenManager.createRefreshBody().refreshToken.isNullOrBlank())
    }

    @Test
    fun clear_token_manager_for_user() {
        var tokenManager = TokenManager.getInstance(username, openPgp)

        tokenManager!!.handleLogin(loginResponse)

        tokenManager.clear()
        assertNull(tokenManager.authAccessToken)
        assert(tokenManager.createRefreshBody().refreshToken.isNullOrBlank())

        // obtain TokenManager instance again after clearing
        tokenManager = TokenManager.getInstance(username, openPgp)

        assertNull(tokenManager!!.authAccessToken)
        assert(tokenManager.createRefreshBody().refreshToken.isNullOrBlank())
    }

    @Test
    fun handle_refresh() {
        val tokenManager = TokenManager.getInstance(username, openPgp)

        tokenManager!!.handleLogin(loginResponse)
        tokenManager!!.handleRefresh(refreshResponse)

        assertEquals("$tokenType $accessTokenRefreshed", tokenManager.authAccessToken)
        assertEquals(refreshTokenRefreshed, tokenManager.createRefreshBody().refreshToken)
    }

}
