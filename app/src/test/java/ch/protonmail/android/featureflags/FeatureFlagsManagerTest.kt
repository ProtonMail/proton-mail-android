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

package ch.protonmail.android.featureflags

import ch.protonmail.android.utils.AppUtil
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureFlagsManagerTest {

    lateinit var featureFlags: FeatureFlagsManager

    @BeforeTest
    fun setUp() {
        featureFlags = FeatureFlagsManager()
    }

    @Test
    fun changeViewModeFeatureFlagIsDisabledForNonDebugBuilds() {
        mockkStatic(AppUtil::class)
        every { AppUtil.isDebug() } returns false

        assertEquals(false, featureFlags.isChangeViewModeFeatureEnabled())

        unmockkStatic(AppUtil::class)
    }

    @Test
    fun changeViewModeFeatureFlagIsEnabledForDebugBuilds() {
        mockkStatic(AppUtil::class)
        every { AppUtil.isDebug() } returns true

        assertEquals(true, featureFlags.isChangeViewModeFeatureEnabled())

        unmockkStatic(AppUtil::class)
    }
}
