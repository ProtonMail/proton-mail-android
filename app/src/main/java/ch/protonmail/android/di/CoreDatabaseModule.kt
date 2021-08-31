/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.accountmanager.data.db.AccountManagerDatabase
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.key.data.db.KeySaltDatabase
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.mailsettings.data.db.MailSettingsDatabase
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.usersettings.data.db.UserSettingsDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppDatabaseModule {
    @Provides
    @Singleton
    fun provideAccountManagerDatabase(@ApplicationContext context: Context): AccountManagerDatabase =
        AccountManagerDatabase.buildDatabase(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppDatabaseBindsModule {

    @Binds
    abstract fun provideAccountDatabase(db: AccountManagerDatabase): AccountDatabase

    @Binds
    abstract fun provideUserDatabase(db: AccountManagerDatabase): UserDatabase

    @Binds
    abstract fun provideAddressDatabase(db: AccountManagerDatabase): AddressDatabase

    @Binds
    abstract fun provideKeySaltDatabase(db: AccountManagerDatabase): KeySaltDatabase

    @Binds
    abstract fun providePublicAddressDatabase(db: AccountManagerDatabase): PublicAddressDatabase

    @Binds
    abstract fun provideHumanVerificationDatabase(db: AccountManagerDatabase): HumanVerificationDatabase

    @Binds
    abstract fun provideMailSettingsDatabase(db: AccountManagerDatabase): MailSettingsDatabase

    @Binds
    abstract fun provideUserSettingsDatabase(db: AccountManagerDatabase): UserSettingsDatabase
}
