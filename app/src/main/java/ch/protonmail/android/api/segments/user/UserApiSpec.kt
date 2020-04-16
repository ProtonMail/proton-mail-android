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
package ch.protonmail.android.api.segments.user

import ch.protonmail.android.api.models.*
import ch.protonmail.android.api.models.requests.PostHumanVerificationBody
import retrofit2.Call
import java.io.IOException

interface UserApiSpec {

    @Throws(IOException::class)
    fun fetchUserInfo() : UserInfo

    @Throws(IOException::class)
    fun fetchUserInfo(username: String) : UserInfo

    @Throws(IOException::class)
    fun fetchKeySalts() : KeySalts

    @Throws(IOException::class)
    fun fetchHumanVerificationOptions() : HumanVerifyOptionsResponse

    @Throws(IOException::class)
    fun postHumanVerification(body: PostHumanVerificationBody): ResponseBody?

    @Throws(IOException::class)
    fun createUser(username: String, password: PasswordVerifier, updateMe: Boolean, tokenType: String, token: String, timestamp:String, payload:String): UserInfo

    @Throws(IOException::class)
    fun sendVerificationCode(verificationCodeBody: VerificationCodeBody): ResponseBody

    @Throws(IOException::class)
    fun isUsernameAvailable(username: String): ResponseBody

    @Throws(IOException::class)
    fun fetchDirectEnabled(): DirectEnabledResponse
}
