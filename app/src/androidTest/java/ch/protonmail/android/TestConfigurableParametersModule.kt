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

package ch.protonmail.android

import ch.protonmail.android.di.AlternativeApiPins
import ch.protonmail.android.di.BaseUrl
import ch.protonmail.android.di.ConfigurableParametersModule
import ch.protonmail.android.di.DefaultApiPins
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import me.proton.core.humanverification.presentation.HumanVerificationApiHost

private const val LOCAL_API_HOST = "localhost:8080"
private const val LOCAL_BASE_URL = "http://$LOCAL_API_HOST/"

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ConfigurableParametersModule::class]
)

/**
 * Replace hilt/dagger module for test, following
 * [https://dagger.dev/hilt/testing.html]
 */
object TestConfigurableParametersModule {

    @BaseUrl
    @Provides
    fun provideBaseUrl(): String = LOCAL_BASE_URL

    @Provides
    @HumanVerificationApiHost
    fun provideHumanVerificationApiHost(): String = LOCAL_API_HOST

    @Provides
    @AlternativeApiPins
    fun alternativeApiPins() = emptyList<String>()

    @Provides
    @DefaultApiPins
    fun defaultApiPins() = emptyArray<String>()
}
