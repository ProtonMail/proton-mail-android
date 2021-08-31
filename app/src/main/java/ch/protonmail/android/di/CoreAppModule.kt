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

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import ch.protonmail.android.core.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.domain.entity.Product
import me.proton.core.user.data.DefaultDomainHost
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreAppModule {

    @Provides
    @Singleton
    fun provideProduct(): Product = Product.Mail

    @Provides
    @Singleton
    fun provideRequiredAccountType(): AccountType = AccountType.Internal

    @Provides
    @DefaultDomainHost
    fun provideDefaultDomainHost() = Constants.MAIL_DOMAIN_COM

    @Provides
    @Singleton
    @AppProcessLifecycleOwner
    fun provideAppProcessLifecycleOwner(): LifecycleOwner = ProcessLifecycleOwner.get()
}
