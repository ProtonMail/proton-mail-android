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
package ch.protonmail.android.featureflags

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class FeatureFlagsInitializer : Initializer<RefreshFeatureFlags> {

   override fun create(context: Context): RefreshFeatureFlags {
      val refreshFeatureFlags = EntryPointAccessors.fromApplication(
          context.applicationContext,
          FeatureFlagsEntryPoint::class.java
      ).refreshFeatureFlags()
      refreshFeatureFlags.refresh()
      return refreshFeatureFlags
   }

   override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

   @EntryPoint
   @InstallIn(SingletonComponent::class)
   interface FeatureFlagsEntryPoint {

      fun refreshFeatureFlags(): RefreshFeatureFlags
   }
}
