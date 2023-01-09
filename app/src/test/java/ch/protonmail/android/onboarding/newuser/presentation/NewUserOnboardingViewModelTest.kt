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

package ch.protonmail.android.onboarding.newuser.presentation

import android.content.SharedPreferences
import app.cash.turbine.test
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.onboarding.base.presentation.OnboardingViewModel
import ch.protonmail.android.onboarding.base.presentation.model.OnboardingItemUiModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test
import kotlin.test.assertEquals

internal class NewUserOnboardingViewModelTest :
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private val prefsEditorMock = mockk<SharedPreferences.Editor>(relaxUnitFun = true) {
        every { putBoolean(any(), any()) } returns this@mockk
    }
    private val defaultSharedPreferencesMock = mockk<SharedPreferences> {
        every { edit() } returns prefsEditorMock
    }
    private val newUserOnboardingViewModel
        get() = runBlocking(dispatchers.Main) {
            NewUserOnboardingViewModel(
                defaultSharedPreferencesMock,
                dispatchers
            )
        }

    @Test
    fun `should emit the expected state`() = runBlockingTest {
        // when
        newUserOnboardingViewModel.onboardingState.test {
            val actualState = awaitItem()

            // then
            assertEquals(EXPECTED_STATE, actualState)
        }
    }

    @Test
    fun `should save the onboarding shown in prefs`() = runBlockingTest {
        // when
        newUserOnboardingViewModel.saveOnboardingShown()

        // then
        verify { prefsEditorMock.putBoolean(Constants.Prefs.PREF_NEW_USER_ONBOARDING_SHOWN, true) }
        verify { prefsEditorMock.apply() }
    }

    private companion object TestData {

        val EXPECTED_STATE = OnboardingViewModel.OnboardingState(
            listOf(
                OnboardingItemUiModel(
                    R.drawable.img_onboarding_encryption,
                    R.string.new_user_onboarding_privacy_for_all_headline,
                    R.string.new_user_onboarding_privacy_for_all_description
                ),
                OnboardingItemUiModel(
                    R.drawable.img_onboarding_labels_folders,
                    R.string.new_user_onboarding_neat_and_tidy_headline,
                    R.string.new_user_onboarding_neat_and_tidy_description
                )
            )
        )
    }
}
