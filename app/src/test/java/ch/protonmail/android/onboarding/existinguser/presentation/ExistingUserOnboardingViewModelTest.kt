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

package ch.protonmail.android.onboarding.existinguser.presentation

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

internal class ExistingUserOnboardingViewModelTest :
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private val prefsEditorMock = mockk<SharedPreferences.Editor>(relaxUnitFun = true) {
        every { putBoolean(any(), any()) } returns this@mockk
    }
    private val defaultSharedPreferencesMock = mockk<SharedPreferences> {
        every { edit() } returns prefsEditorMock
    }
    private val existingUserOnboardingViewModel
        get() = runBlocking(dispatchers.Main) {
            ExistingUserOnboardingViewModel(
                defaultSharedPreferencesMock,
                dispatchers
            )
        }

    @Test
    fun `should emit the expected state`() = runBlockingTest {
        // when
        existingUserOnboardingViewModel.onboardingState.test {
            val actualState = awaitItem()

            // then
            assertEquals(EXPECTED_STATE, actualState)
        }
    }

    @Test
    fun `should save the onboarding shown in prefs`() = runBlockingTest {
        // when
        existingUserOnboardingViewModel.saveOnboardingShown()

        // then
        verify { prefsEditorMock.putBoolean(Constants.Prefs.PREF_EXISTING_USER_ONBOARDING_SHOWN, true) }
        verify { prefsEditorMock.apply() }
    }

    private companion object TestData {

        val EXPECTED_STATE = OnboardingViewModel.OnboardingState(
            listOf(
                OnboardingItemUiModel(
                    R.drawable.img_onboarding_encryption,
                    R.string.existing_user_onboarding_new_look_headline,
                    R.string.existing_user_onboarding_new_look_description
                ),
                OnboardingItemUiModel(
                    R.drawable.img_onboarding_new_features,
                    R.string.existing_user_onboarding_new_features_headline,
                    R.string.existing_user_onboarding_new_features_description
                ),
                OnboardingItemUiModel(
                    R.drawable.ic_logo_mail,
                    R.string.existing_user_onboarding_updated_proton_headline,
                    R.string.existing_user_onboarding_updated_proton_description,
                    R.drawable.welcome_header_mail
                )
            )
        )
    }
}
