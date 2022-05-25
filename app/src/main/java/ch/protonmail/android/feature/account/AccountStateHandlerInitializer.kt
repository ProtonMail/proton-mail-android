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

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import me.proton.core.accountmanager.data.AccountStateHandler

class AccountStateHandlerInitializer : Initializer<AccountStateHandler> {

    override fun create(context: Context): AccountStateHandler {
        val accountStateHandler = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AccountStateHandlerInitializerEntryPoint::class.java
        ).accountStateHandler()

        accountStateHandler.start()

        return accountStateHandler
    }

    override fun dependencies(): List<Class<out Initializer<*>?>> = emptyList()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AccountStateHandlerInitializerEntryPoint {
        fun accountStateHandler(): AccountStateHandler
    }
}
