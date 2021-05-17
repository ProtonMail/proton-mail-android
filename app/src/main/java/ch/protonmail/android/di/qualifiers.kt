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

package ch.protonmail.android.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class AlternativeApiPins

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class AppCoroutineScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class AppProcessLifecycleOwner

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class AttachmentsDirectory

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class BackupSharedPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class CurrentUserCrypto

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class CurrentUserId

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class CurrentUserMailSettings

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class DefaultSharedPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class DohProviders

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION])
annotation class MessageDaoQualifier
