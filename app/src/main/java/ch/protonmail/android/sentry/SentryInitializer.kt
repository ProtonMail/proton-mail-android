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

package ch.protonmail.android.sentry

import android.content.Context
import android.os.Build
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.AppUtil
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.sentry.Sentry
import io.sentry.SentryOptions

class SentryInitializer : Initializer<SentryUserObserver> {

    override fun create(context: Context): SentryUserObserver {
        Sentry.init { options: SentryOptions ->
            with(options) {
                dsn = BuildConfig.SENTRY_DSN
                release = BuildConfig.VERSION_NAME
                environment = Constants.HOST
                setTag(APP_VERSION, AppUtil.getAppVersion())
                setTag(SDK_VERSION, "${Build.VERSION.SDK_INT}")
                setTag(DEVICE_MODEL, Build.MODEL)
            }
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

    private companion object Tag {

        const val APP_VERSION = "APP_VERSION"
        const val SDK_VERSION = "SDK_VERSION"
        const val DEVICE_MODEL = "DEVICE_MODEL"
    }
}
