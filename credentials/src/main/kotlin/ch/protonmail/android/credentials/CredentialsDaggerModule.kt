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

package ch.protonmail.android.credentials

import android.content.Context
import android.content.SharedPreferences
import ch.protonmail.android.domain.repository.CredentialRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
internal object CredentialsDaggerModule {

    @Provides
    @CredentialsSharedPreferences
    fun credentialsSharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences("credentials", Context.MODE_PRIVATE)
}

@Module
@InstallIn(SingletonComponent::class)
internal interface CredentialsDaggerBindsModule {

    @Binds
    fun SharedPreferencesCredentialRepository.repo(): CredentialRepository
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class CredentialsSharedPreferences
