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
import me.proton.core.network.data.ApiProvider
import me.proton.core.plan.data.repository.PlansRepositoryImpl
import me.proton.core.plan.domain.SupportedSignupPaidPlans
import me.proton.core.plan.domain.SupportedUpgradePaidPlans
import me.proton.core.plan.domain.repository.PlansRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CorePlanModule {

    @Provides
    @SupportedSignupPaidPlans
    fun provideClientSupportedPaidPlanIds(): List<String> = listOf("plus")

    @Provides
    @Singleton
    fun providePlansRepository(apiProvider: ApiProvider): PlansRepository =
        PlansRepositoryImpl(apiProvider)
}

@Module
@InstallIn(SingletonComponent::class)
interface PlansBindsModule {
    @Binds
    @SupportedUpgradePaidPlans
    fun bindClientSupportedUpgradePaidPlanNames(@SupportedSignupPaidPlans plans: List<String>): List<String>
}
