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
import ch.protonmail.android.feature.account.SetupAccountUserCheck
import ch.protonmail.android.prefs.SecureSharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.auth.data.repository.AuthRepositoryImpl
import me.proton.core.auth.domain.repository.AuthRepository
import me.proton.core.auth.domain.usecase.PostLoginAccountSetup
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.crypto.android.srp.GOpenPGPSrpCrypto
import me.proton.core.crypto.common.srp.SrpCrypto
import me.proton.core.network.data.ApiProvider
import me.proton.core.user.domain.UserManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreAuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(apiProvider: ApiProvider): AuthRepository =
        AuthRepositoryImpl(apiProvider)

    @Provides
    fun provideAuthOrchestrator(): AuthOrchestrator =
        AuthOrchestrator()

    @Provides
    @Singleton
    fun provideSrpCrypto(): SrpCrypto =
        GOpenPGPSrpCrypto()

    @Provides
    @Singleton
    fun provideUserCheck(
        @ApplicationContext context: Context,
        accountManager: AccountManager,
        userManager: UserManager,
        factory: SecureSharedPreferences.Factory
    ): PostLoginAccountSetup.UserCheck = SetupAccountUserCheck(context, accountManager, userManager, factory)
}
