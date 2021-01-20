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
import ch.protonmail.android.api.models.LoginInfoResponse
import ch.protonmail.android.api.models.LoginResponse
import ch.protonmail.android.api.models.ModulusResponse
import ch.protonmail.android.api.models.RefreshBody
import ch.protonmail.android.api.models.RefreshResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.TwoFABody
import ch.protonmail.android.api.models.TwoFAResponse
import retrofit2.Call
import java.io.IOException

interface AuthenticationApiSpec {

    @Throws(IOException::class)
    fun revokeAccessBlocking(username: String): ResponseBody

    suspend fun revokeAccess(username: String): ResponseBody

    @Throws(IOException::class)
    fun loginInfo(username: String): LoginInfoResponse

    /**
     * This call strips out all user-specific headers so we get clean SRP session.
     */
    @Throws(IOException::class)
    fun loginInfoForAuthentication(username: String): LoginInfoResponse

    @Throws(IOException::class)
    fun login(username: String, srpSession: String, clientEphemeral: ByteArray, clientProof: ByteArray): LoginResponse

    @Throws(IOException::class)
    fun randomModulus(): ModulusResponse

    suspend fun refreshAuth(refreshBody: RefreshBody, retrofitTag: RetrofitTag?): RefreshResponse

    @Throws(IOException::class)
    fun refreshAuthBlocking(refreshBody: RefreshBody, retrofitTag: RetrofitTag?): RefreshResponse

    fun twoFactor(twoFABody: TwoFABody): TwoFAResponse
}
