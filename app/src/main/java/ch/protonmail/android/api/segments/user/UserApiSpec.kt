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

import ch.protonmail.android.api.models.DirectEnabledResponse
import ch.protonmail.android.api.models.KeySalts
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.UserInfo
import ch.protonmail.android.domain.entity.Id
import java.io.IOException

interface UserApiSpec {

    @Throws(IOException::class)
    fun fetchUserInfoBlocking(): UserInfo

    suspend fun fetchUserInfo(): UserInfo

    @Throws(IOException::class)
    fun fetchUserInfoBlocking(userId: Id): UserInfo

    fun fetchKeySalts(userId: Id): KeySalts

    @Deprecated("Use with user Id", ReplaceWith("fetchKeySalts(userId)"))
    fun fetchKeySalts(): KeySalts

    @Throws(IOException::class)
    fun isUsernameAvailable(username: String): ResponseBody

    @Throws(IOException::class)
    fun fetchDirectEnabled(): DirectEnabledResponse
}
