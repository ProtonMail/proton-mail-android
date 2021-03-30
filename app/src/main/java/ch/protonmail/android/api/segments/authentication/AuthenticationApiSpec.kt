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

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.ModulusResponse
import ch.protonmail.android.api.models.RefreshBody
import ch.protonmail.android.api.models.RefreshResponse
import java.io.IOException

interface AuthenticationApiSpec {

    @Throws(IOException::class)
    fun randomModulus(): ModulusResponse

    suspend fun refreshAuth(refreshBody: RefreshBody, userIdTag: UserIdTag?): RefreshResponse

    fun refreshAuthBlocking(refreshBody: RefreshBody, userIdTag: UserIdTag): RefreshResponse
}
