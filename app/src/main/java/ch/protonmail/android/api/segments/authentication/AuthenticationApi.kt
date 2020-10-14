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
package ch.protonmail.android.api.segments.authentication

import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.LoginBody
import ch.protonmail.android.api.models.LoginInfoBody
import ch.protonmail.android.api.models.LoginInfoResponse
import ch.protonmail.android.api.models.LoginResponse
import ch.protonmail.android.api.models.ModulusResponse
import ch.protonmail.android.api.models.RefreshBody
import ch.protonmail.android.api.models.RefreshResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.TwoFABody
import ch.protonmail.android.api.models.TwoFAResponse
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import ch.protonmail.android.utils.ConstantTime
import java.io.IOException

class AuthenticationApi(
    private val service: AuthenticationService,
    private val pubService: AuthenticationPubService
) : BaseApi(), AuthenticationApiSpec {

    @Throws(IOException::class)
    override fun twoFactor(twoFABody: TwoFABody): TwoFAResponse =
        ParseUtils.parse(pubService.post2fa(twoFABody).execute())

    @Throws(IOException::class)
    override fun revokeAccessBlocking(username: String): ResponseBody =
        ParseUtils.parse(service.revoke(RetrofitTag(username)).execute())

    override suspend fun revokeAccess(username: String): ResponseBody =
        ParseUtils.parse(service.revoke(RetrofitTag(username)).execute())

    @Throws(IOException::class)
    override fun loginInfo(username: String): LoginInfoResponse {
        val infoBody = LoginInfoBody(username)
        return ParseUtils.parse(pubService.loginInfo(infoBody).execute())
    }

    @Throws(IOException::class)
    override fun loginInfoForAuthentication(username: String): LoginInfoResponse {
        val infoBody = LoginInfoBody(username)
        return ParseUtils.parse(pubService.loginInfo(infoBody, RetrofitTag(usernameAuth = null)).execute())
    }

    @Throws(IOException::class)
    override fun login(username: String, srpSession: String, clientEphemeral: ByteArray, clientProof: ByteArray): LoginResponse {
        // We don't actually need constant time encoding here, assuming that SRP is secure. However,
        // given that the data has information about passwords in it, it is better to be safe than
        // sorry.
        val loginBody = LoginBody(username, srpSession, ConstantTime.encodeBase64(clientEphemeral, true), ConstantTime.encodeBase64(clientProof, true), null)
        return ParseUtils.parse(pubService.login(loginBody).execute())
    }

    @Throws(IOException::class)
    override fun randomModulus(): ModulusResponse = ParseUtils.parse(pubService.randomModulus().execute())

    @Throws(IOException::class)
    override fun refreshSync(refreshBody: RefreshBody): RefreshResponse = ParseUtils.parse(pubService.refreshSync(refreshBody).execute())
}
