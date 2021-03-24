/*
 * Copyright (c) 2020 Proton Technologies AG
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
import me.proton.core.account.data.repository.AccountRepositoryImpl
import me.proton.core.account.domain.repository.AccountRepository
import me.proton.core.accountmanager.data.AccountManagerImpl
import me.proton.core.accountmanager.data.SessionManagerImpl
import me.proton.core.accountmanager.data.db.AccountManagerDatabase
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.auth.domain.AccountWorkflowHandler
import me.proton.core.auth.domain.repository.AuthRepository
import me.proton.core.crypto.android.context.AndroidCryptoContext
import me.proton.core.crypto.android.keystore.AndroidKeyStoreCrypto
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.domain.entity.Product
import me.proton.core.network.domain.session.SessionListener
import me.proton.core.network.domain.session.SessionProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountManagerModule {

    @Provides
    @Singleton
    fun provideAccountManagerDatabase(@ApplicationContext context: Context): AccountManagerDatabase =
        AccountManagerDatabase.buildDatabase(context)

    @Provides
    @Singleton
    fun provideKeyStoreCrypto(): KeyStoreCrypto =
        AndroidKeyStoreCrypto.default

    @Provides
    @Singleton
    fun provideCryptoContext(
        keyStoreCrypto: KeyStoreCrypto
    ): CryptoContext =
        AndroidCryptoContext(keyStoreCrypto)

    @Provides
    @Singleton
    fun provideAccountRepository(
        product: Product,
        accountManagerDatabase: AccountManagerDatabase,
        keyStoreCrypto: KeyStoreCrypto
    ): AccountRepository =
        AccountRepositoryImpl(product, accountManagerDatabase, keyStoreCrypto)

    @Provides
    @Singleton
    fun provideAccountManagerImpl(
        product: Product,
        accountRepository: AccountRepository,
        authRepository: AuthRepository
    ): AccountManagerImpl =
        AccountManagerImpl(product, accountRepository, authRepository)

    @Provides
    @Singleton
    fun provideSessionManagerImpl(
        accountRepository: AccountRepository
    ): SessionManagerImpl =
        SessionManagerImpl(accountRepository)
}

@Module
@InstallIn(SingletonComponent::class)
interface AccountManagerBindModule {

    @Binds
    fun bindAccountManager(accountManagerImpl: AccountManagerImpl): AccountManager

    @Binds
    fun bindAccountWorkflowHandler(accountManagerImpl: AccountManagerImpl): AccountWorkflowHandler

    @Binds
    fun bindSessionProvider(sessionManagerImpl: SessionManagerImpl): SessionProvider

    @Binds
    fun bindSessionListener(sessionManagerImpl: SessionManagerImpl): SessionListener
}
