/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.feature.account

import ch.protonmail.android.api.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getAccounts
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.domain.entity.user.User as NewUser

@Deprecated("Replaced by Core AccountManager", ReplaceWith("Core AccountManager"))
fun AccountManager.allLoggedInBlocking() = runBlocking { allLoggedIn() }

@Deprecated("Replaced by Core AccountManager", ReplaceWith("Core AccountManager"))
suspend fun AccountManager.allLoggedIn() =
    getAccounts(AccountState.Ready).firstOrNull()?.map { UserId(it.userId.id) }.orEmpty().toSet()

@Deprecated("Replaced by Core AccountManager", ReplaceWith("Core AccountManager"))
suspend fun AccountManager.allSaved() =
    getAccounts().firstOrNull()?.map { UserId(it.userId.id) }.orEmpty().toSet()

suspend fun AccountManager.primaryUserId(
    scope: CoroutineScope
): StateFlow<UserId?> = getPrimaryUserId().stateIn(scope)

suspend fun AccountManager.primaryId(
    scope: CoroutineScope
): StateFlow<UserId?> = primaryUserId(scope).mapLatest { it?.let { UserId(it.id) } }.stateIn(scope)

suspend fun AccountManager.primaryLegacyUser(
    scope: CoroutineScope,
    refresh: Flow<Unit>,
    getUser: suspend (UserId) -> User
): StateFlow<User?> = primaryId(scope).combine(refresh) { id: UserId?, _ -> id }
    .mapLatest { it?.let { getUser(it) } }.stateIn(scope)

suspend fun AccountManager.primaryUser(
    scope: CoroutineScope,
    refresh: Flow<Unit>,
    getNewUser: suspend (UserId) -> NewUser
): StateFlow<NewUser?> = primaryId(scope).combine(refresh) { id: UserId?, _ -> id }
    .mapLatest { it?.let { getNewUser(it) } }.stateIn(scope)
