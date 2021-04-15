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

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.util.orThrow
import ch.protonmail.android.feature.account.allSaved
import kotlinx.coroutines.runBlocking
import me.proton.core.accountmanager.domain.AccountManager
import javax.inject.Inject

/**
 * Find [Id] for a saved user associated with the given username
 */
class FindUserIdForUsername @Inject constructor(
    private val accountManager: AccountManager,
    private val loadUser: LoadUser
) {

    suspend operator fun invoke(username: Name): Either<Error, Id> {
        var failedToLoadUser = false

        val userId = accountManager.allSaved().find {
            val userEither = loadUser(it)
            if (userEither.isLeft()) {
                failedToLoadUser = true
                false
            } else {
                userEither.orThrow().name == username
            }
        }

        return when {
            userId != null -> Right(userId)
            failedToLoadUser -> Left(Error.CantLoadUser)
            else -> Left(Error.UserNotFound)
        }
    }

    @Deprecated(
        "Should not be used, necessary only for old and Java classes",
        ReplaceWith("invoke(username)")
    )
    fun blocking(username: Name): Either<Error, Id> =
        runBlocking { invoke(username) }

    sealed class Error {
        object UserNotFound : Error()
        object CantLoadUser : Error()
    }
}
