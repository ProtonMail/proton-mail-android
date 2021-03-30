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

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DirectEnabledResponse
import ch.protonmail.android.api.models.KeySalts
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.UserInfo
import ch.protonmail.android.api.utils.ParseUtils
import ch.protonmail.android.domain.entity.Id
import java.io.IOException

// region constants
const val MAIL_TYPE = 1
// endregion

class UserApi(
    private val service: UserService,
    private val pubService: UserPubService
) : UserApiSpec {

    @Throws(IOException::class)
    override fun fetchUserInfoBlocking(): UserInfo =
        ParseUtils.parse(service.fetchUserInfoCall().execute())

    override suspend fun fetchUserInfo(): UserInfo =
        service.fetchUserInfo()

    @Throws(IOException::class)
    override fun fetchUserInfoBlocking(userId: Id): UserInfo =
        ParseUtils.parse(service.fetchUserInfoCall(UserIdTag(userId)).execute())

    override fun fetchKeySalts(userId: Id): KeySalts =
        ParseUtils.parse(service.fetchKeySalts(UserIdTag(userId)).execute())

    @Deprecated("Use with user Id", ReplaceWith("fetchKeySalts(userId)"))
    override fun fetchKeySalts(): KeySalts =
        ParseUtils.parse(service.fetchKeySalts().execute())

    @Throws(IOException::class)
    override fun isUsernameAvailable(username: String): ResponseBody =
        ParseUtils.parse(pubService.isUsernameAvailable(username).execute())

    @Throws(IOException::class)
    override fun fetchDirectEnabled(): DirectEnabledResponse =
        ParseUtils.parse(pubService.fetchDirectEnabled(MAIL_TYPE).execute())
}
