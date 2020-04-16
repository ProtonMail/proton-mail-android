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

import ch.protonmail.android.api.models.*
import java.io.IOException

interface AuthenticationApiSpec {

    @Throws(IOException::class)
    fun revokeAccess(username: String): ResponseBody

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

    @Throws(IOException::class)
    fun refreshSync(refreshBody: RefreshBody): RefreshResponse

    fun twoFactor(twoFABody: TwoFABody): TwoFAResponse
}
