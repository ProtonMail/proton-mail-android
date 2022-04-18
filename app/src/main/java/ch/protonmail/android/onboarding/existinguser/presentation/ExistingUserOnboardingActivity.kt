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

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ch.protonmail.android.databinding.LayoutOnboardingToNewBrandItemBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExistingUserOnboardingActivity : AppCompatActivity() {

    val viewModel by viewModels<ExistingUserOnboardingViewModel>()

    private val binding by lazy {
        LayoutOnboardingToNewBrandItemBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            finishOnboardingActivity()
        }
    }

    override fun onBackPressed() {
        // back navigation is disabled
    }

    private fun finishOnboardingActivity() {
        viewModel.saveOnboardingShown()
        finish()
    }
}
