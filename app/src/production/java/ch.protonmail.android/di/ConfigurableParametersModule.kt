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

import ch.protonmail.android.core.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.humanverification.presentation.HumanVerificationApiHost
import me.proton.core.network.data.di.AlternativeApiPins
import me.proton.core.network.data.di.CertificatePins
import me.proton.core.network.data.di.Constants as CoreConstants

@Module
@InstallIn(SingletonComponent::class)
object ConfigurableParametersModule {

    @BaseUrl
    @Provides
    fun provideBaseUrl(): String = Constants.BASE_URL

    @Provides
    @HumanVerificationApiHost
    fun provideHumanVerificationApiHost(): String = Constants.HUMAN_VERIFICATION_URL

    @Provides
    @AlternativeApiPins
    fun alternativeApiPins() = CoreConstants.ALTERNATIVE_API_SPKI_PINS

    @Provides
    @CertificatePins
    fun defaultApiPins() = CoreConstants.DEFAULT_SPKI_PINS
}
