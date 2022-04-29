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

package ch.protonmail.android.onboarding.base.presentation

import android.content.SharedPreferences
import androidx.startup.AppInitializer
import ch.protonmail.android.core.Constants.Prefs.PREF_EXISTING_USER_ONBOARDING_SHOWN
import ch.protonmail.android.testdata.UserIdTestData
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.proton.core.domain.entity.UserId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class AddStartOnboardingObserverIfNeededTest(
    private val userId: UserId?,
    private val onboardingAlreadyShown: OnboardingAlreadyShown,
    private val shouldAddObserver: ShouldAddObserver
) {

    private val sharedPrefsMock = mockk<SharedPreferences> {
        every { getBoolean(PREF_EXISTING_USER_ONBOARDING_SHOWN, false) } returns onboardingAlreadyShown.value
    }
    private val appInitializerMock = mockk<AppInitializer> {
        every { initializeComponent(StartOnboardingObserverInitializer::class.java) } returns mockk()
    }
    private val addStartOnboardingObserverIfNeeded = AddStartOnboardingObserverIfNeeded(sharedPrefsMock)

    @Test
    fun `should add the observer`() {
        // when
        addStartOnboardingObserverIfNeeded(appInitializerMock, userId)

        // then
        if (shouldAddObserver.value) {
            verify { appInitializerMock.initializeComponent(StartOnboardingObserverInitializer::class.java) }
        } else {
            verify { appInitializerMock wasNot called }
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf(null,                  OnboardingAlreadyShown(true),   ShouldAddObserver(false)),
                arrayOf(null,                  OnboardingAlreadyShown(false),  ShouldAddObserver(false)),
                arrayOf(UserIdTestData.userId, OnboardingAlreadyShown(true),   ShouldAddObserver(false)),
                arrayOf(UserIdTestData.userId, OnboardingAlreadyShown(false),  ShouldAddObserver(true)),
            )
        }
    }

    data class OnboardingAlreadyShown(val value: Boolean)
    data class ShouldAddObserver(val value: Boolean)
}
