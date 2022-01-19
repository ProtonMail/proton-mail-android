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

package ch.protonmail.android.pinlock.presentation

import android.content.Context
import androidx.startup.Initializer
import ch.protonmail.android.pinlock.domain.usecase.ShouldShowPinLockScreen
import ch.protonmail.android.usecase.GetElapsedRealTimeMillis
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class PinLockManagerInitializer : Initializer<PinLockManager> {

    override fun create(context: Context): PinLockManager {
        val entryPoint = EntryPointAccessors.fromApplication(context, PinLockManagerEntryPoint::class.java)
        return PinLockManager(
            context = context,
            getElapsedRealTimeMillis = entryPoint.getElapsedRealTimeMillis(),
            shouldShowPinLockScreen = entryPoint.shouldShowPinLockScreen()
        )
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PinLockManagerEntryPoint {

        fun getElapsedRealTimeMillis(): GetElapsedRealTimeMillis
        fun shouldShowPinLockScreen(): ShouldShowPinLockScreen
    }
}
