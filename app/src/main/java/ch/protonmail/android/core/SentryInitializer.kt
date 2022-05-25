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

package ch.protonmail.android.core

import android.content.Context
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.sentry.Sentry
import io.sentry.SentryOptions

class SentryInitializer : Initializer<SentryUserObserver> {

    override fun create(context: Context): SentryUserObserver {
        Sentry.init { options: SentryOptions ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.release = BuildConfig.VERSION_NAME
            options.environment = Constants.HOST
        }

        val observer = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SentryInitializerEntryPoint::class.java
        ).observer()

        observer.start()

        return observer
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SentryInitializerEntryPoint {
        fun observer(): SentryUserObserver
    }
}
