/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.onboarding.base.presentation

import android.content.SharedPreferences
import androidx.startup.AppInitializer
import ch.protonmail.android.core.Constants.Prefs.PREF_EXISTING_USER_ONBOARDING_SHOWN
import ch.protonmail.android.core.Constants.Prefs.PREF_PREVIOUS_APP_VERSION
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
    private val previousAppVersion: PreviousAppVersion,
    private val userId: UserId?,
    private val onboardingAlreadyShown: OnboardingAlreadyShown,
    private val shouldAddObserver: ShouldAddObserver
) {

    private val sharedPrefsMock = mockk<SharedPreferences> {
        every { getBoolean(PREF_EXISTING_USER_ONBOARDING_SHOWN, false) } returns onboardingAlreadyShown.value
        every { getInt(PREF_PREVIOUS_APP_VERSION, Int.MAX_VALUE) } returns previousAppVersion.value
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
                arrayOf(PreviousAppVersion(823), null,                  OnboardingAlreadyShown(true),   ShouldAddObserver(false)),
                arrayOf(PreviousAppVersion(823), null,                  OnboardingAlreadyShown(false),  ShouldAddObserver(false)),
                arrayOf(PreviousAppVersion(823), UserIdTestData.userId, OnboardingAlreadyShown(true),   ShouldAddObserver(false)),
                arrayOf(PreviousAppVersion(823), UserIdTestData.userId, OnboardingAlreadyShown(false),  ShouldAddObserver(false)),
                arrayOf(PreviousAppVersion(822), null,                  OnboardingAlreadyShown(true),   ShouldAddObserver(false)),
                arrayOf(PreviousAppVersion(822), null,                  OnboardingAlreadyShown(false),  ShouldAddObserver(false)),
                arrayOf(PreviousAppVersion(822), UserIdTestData.userId, OnboardingAlreadyShown(true),   ShouldAddObserver(false)),
                arrayOf(PreviousAppVersion(822), UserIdTestData.userId, OnboardingAlreadyShown(false),  ShouldAddObserver(true)),
            )
        }
    }

    data class PreviousAppVersion(val value: Int)
    data class OnboardingAlreadyShown(val value: Boolean)
    data class ShouldAddObserver(val value: Boolean)
}
