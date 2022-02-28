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

package ch.protonmail.android.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.account.data.repository.AccountRepositoryImpl
import me.proton.core.account.domain.repository.AccountRepository
import me.proton.core.accountmanager.data.AccountManagerImpl
import me.proton.core.accountmanager.data.AccountMigratorImpl
import me.proton.core.accountmanager.data.AccountStateHandler
import me.proton.core.accountmanager.data.AccountStateHandlerCoroutineScope
import me.proton.core.accountmanager.data.SessionListenerImpl
import me.proton.core.accountmanager.data.SessionManagerImpl
import me.proton.core.accountmanager.data.SessionProviderImpl
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.accountmanager.domain.migrator.AccountMigrator
import me.proton.core.auth.domain.AccountWorkflowHandler
import me.proton.core.auth.domain.repository.AuthRepository
import me.proton.core.crypto.android.context.AndroidCryptoContext
import me.proton.core.crypto.android.keystore.AndroidKeyStoreCrypto
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.domain.entity.Product
import me.proton.core.network.domain.session.SessionListener
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.repository.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountManagerModule {

    @Provides
    @Singleton
    @AccountStateHandlerCoroutineScope
    fun provideAccountStateHandlerCoroutineScope(): CoroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())

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
        accountManagerDatabase: AccountDatabase,
        keyStoreCrypto: KeyStoreCrypto
    ): AccountRepository =
        AccountRepositoryImpl(product, accountManagerDatabase, keyStoreCrypto)

    @Provides
    @Singleton
    fun provideAccountManagerImpl(
        product: Product,
        accountRepository: AccountRepository,
        authRepository: AuthRepository,
        userManager: UserManager
    ): AccountManagerImpl =
        AccountManagerImpl(product, accountRepository, authRepository, userManager)

    @Provides
    @Singleton
    fun provideAccountMigrator(
        accountManager: AccountManager,
        accountRepository: AccountRepository,
        userRepository: UserRepository
    ): AccountMigrator =
        AccountMigratorImpl(accountManager, accountRepository, userRepository)

    @Suppress("LongParameterList")
    @Provides
    @Singleton
    fun provideAccountStateHandler(
        @AccountStateHandlerCoroutineScope
        scope: CoroutineScope,
        userManager: UserManager,
        accountManager: AccountManager,
        accountRepository: AccountRepository,
        accountMigrator: AccountMigrator,
        product: Product
    ): AccountStateHandler = AccountStateHandler(
        scope,
        userManager,
        accountManager,
        accountRepository,
        accountMigrator,
        product
    )

    @Provides
    @Singleton
    fun provideSessionProvider(
        accountRepository: AccountRepository
    ): SessionProvider =
        SessionProviderImpl(accountRepository)

    @Provides
    @Singleton
    fun provideSessionListener(
        accountRepository: AccountRepository
    ): SessionListener =
        SessionListenerImpl(accountRepository)

    @Provides
    @Singleton
    fun provideSessionManager(
        sessionListener: SessionListener,
        sessionProvider: SessionProvider,
        authRepository: AuthRepository
    ): SessionManager =
        SessionManagerImpl(sessionProvider, sessionListener, authRepository)
}

@Module
@InstallIn(SingletonComponent::class)
interface AccountManagerBindModule {

    @Binds
    fun bindAccountManager(accountManagerImpl: AccountManagerImpl): AccountManager

    @Binds
    fun bindAccountWorkflowHandler(accountManagerImpl: AccountManagerImpl): AccountWorkflowHandler
}
