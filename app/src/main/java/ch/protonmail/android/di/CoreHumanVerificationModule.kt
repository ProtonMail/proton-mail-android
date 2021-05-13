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

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.accountmanager.data.db.AccountManagerDatabase
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.humanverification.data.HumanVerificationListenerImpl
import me.proton.core.humanverification.data.HumanVerificationManagerImpl
import me.proton.core.humanverification.data.repository.HumanVerificationRepositoryImpl
import me.proton.core.humanverification.domain.HumanVerificationManager
import me.proton.core.humanverification.domain.HumanVerificationWorkflowHandler
import me.proton.core.humanverification.domain.repository.HumanVerificationRepository
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.session.HumanVerificationListener
import me.proton.core.network.domain.session.HumanVerificationProvider
import me.proton.core.user.data.repository.UserValidationRepositoryImpl
import me.proton.core.user.domain.repository.UserValidationRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HumanVerificationModule {
    @Provides
    @Singleton
    fun provideHumanVerificationListener(
        humanVerificationRepository: HumanVerificationRepository
    ): HumanVerificationListener =
        HumanVerificationListenerImpl(humanVerificationRepository)

    @Provides
    @Singleton
    fun provideHumanVerificationRepository(
        db: AccountManagerDatabase,
        keyStoreCrypto: KeyStoreCrypto
    ): HumanVerificationRepository =
        HumanVerificationRepositoryImpl(db, keyStoreCrypto)

    @Provides
    @Singleton
    fun provideHumanVerificationManager(
        humanVerificationRepository: HumanVerificationRepository
    ): HumanVerificationManagerImpl = HumanVerificationManagerImpl(humanVerificationRepository)

    @Provides
    @Singleton
    fun provideUserValidationRepositoryImpl(
        provider: ApiProvider,
        humanVerificationListener: HumanVerificationListener
    ): UserValidationRepository = UserValidationRepositoryImpl(provider, humanVerificationListener)
}

@Module
@InstallIn(SingletonComponent::class)
interface HumanVerificationBindModule {

    @Binds
    fun bindHumanVerificationManager(
        humanVerificationManagerImpl: HumanVerificationManagerImpl
    ): HumanVerificationManager

    @Binds
    fun bindHumanVerificationWorkflowHandler(
        humanVerificationManagerImpl: HumanVerificationManagerImpl
    ): HumanVerificationWorkflowHandler

    @Binds
    fun bindHumanVerificationProvider(
        humanVerificationManagerImpl: HumanVerificationManagerImpl
    ): HumanVerificationProvider
}
