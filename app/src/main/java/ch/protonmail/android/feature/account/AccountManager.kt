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

package ch.protonmail.android.feature.account

import ch.protonmail.android.domain.entity.Id
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getAccounts

@Deprecated("Replaced by Core AccountManager", ReplaceWith("Core AccountManager"))
fun AccountManager.allLoggedInBlocking() = runBlocking { allLoggedIn() }

@Deprecated("Replaced by Core AccountManager", ReplaceWith("Core AccountManager"))
suspend fun AccountManager.allLoggedIn() =
    getAccounts(AccountState.Ready).firstOrNull()?.map { Id(it.userId.id) }.orEmpty().toSet()

@Deprecated("Replaced by Core AccountManager", ReplaceWith("Core AccountManager"))
suspend fun AccountManager.allSaved() =
    getAccounts(AccountState.Disabled).firstOrNull()?.map { Id(it.userId.id) }.orEmpty().toSet()
