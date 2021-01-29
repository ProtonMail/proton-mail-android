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

import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name

/**
 * Objects of this class can be attached to OkHttp's requests and be read by Interceptors.
 */
data class UserIdTag(
    val id: Id
)

/**
 * This will be used only during login, when user Id is still unknown,
 * for all the other calls [UserIdTag] must be used instead
 */
data class UsernameTag(
    val username: Name
) {
    constructor(username: String): this(Name(username))
}

@Deprecated("Use 'UserIdTag' instead", ReplaceWith("UserIdTag(userId)"), DeprecationLevel.ERROR)
data class RetrofitTag(
    val usernameAuth: String? // username which we want to authorize request for, remove auth if null
)
