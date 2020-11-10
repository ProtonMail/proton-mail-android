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

package ch.protonmail.android.usecase

import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.User
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

/**
 * Find [Id] for a saved user associated with the given username
 */
class FindUserIdForUsername @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val accountManager: AccountManager
    // private val userManager: UserManager // TODO fix circular dependency, by removing FindUserIdForUsername
    //                                          dependency from userManger
) {

    suspend operator fun invoke(username: Name): Id = withContext(dispatchers.Io) {
        accountManager.allSaved().first {
            val user: User = TODO("userManager.getUser(it)") // UserManager does not support Id yet
            user.name == username
        }
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("invoke(username)")
    )
    fun blocking(username: Name) = runBlocking { invoke(username) }
}
