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

package ch.protonmail.android.onboarding.existinguser.presentation

import android.content.SharedPreferences
import ch.protonmail.android.core.Constants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test

internal class ExistingUserOnboardingViewModelTest : CoroutinesTest {

    private val prefsEditorMock = mockk<SharedPreferences.Editor>(relaxUnitFun = true) {
        every { putBoolean(any(), any()) } returns this@mockk
    }
    private val defaultSharedPreferencesMock = mockk<SharedPreferences> {
        every { edit() } returns prefsEditorMock
    }
    private val existingUserOnboardingViewModel
        get() = runBlocking(TestDispatcherProvider.Main) {
            ExistingUserOnboardingViewModel(
                defaultSharedPreferencesMock,
                TestDispatcherProvider
            )
        }

    @Test
    fun `should save the onboarding shown in prefs`() = runBlockingTest {
        // when
        existingUserOnboardingViewModel.saveOnboardingShown()

        // then
        verify { prefsEditorMock.putBoolean(Constants.Prefs.PREF_EXISTING_USER_ONBOARDING_SHOWN, true) }
        verify { prefsEditorMock.apply() }
    }
}
