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

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import ch.protonmail.android.R
import ch.protonmail.android.databinding.ActivityNewUserOnboardingBinding
import ch.protonmail.android.onboarding.base.presentation.OnboardingAdapter
import ch.protonmail.android.onboarding.base.presentation.model.OnboardingItemUiModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class NewUserOnboardingActivity : AppCompatActivity() {

    private val viewModel: NewUserOnboardingViewModel by viewModels()

    private val binding by lazy {
        ActivityNewUserOnboardingBinding.inflate(layoutInflater)
    }
    private val onboardingAdapter = OnboardingAdapter()
    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            if (position == onboardingAdapter.itemCount - 1) {
                binding.onboardingButton.setText(R.string.onboarding_get_started)
                binding.onboardingToolbar.menu.findItem(R.id.skip)?.isVisible = false
            } else {
                binding.onboardingButton.setText(R.string.onboarding_next)
                binding.onboardingToolbar.menu.findItem(R.id.skip)?.isVisible = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.onboardingToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        window.statusBarColor = ContextCompat.getColor(this, R.color.background_secondary)

        viewModel.onboardingState
            .onEach {
                setOnboardingAdapterItems(it.onboardingItemsList)
            }
            .launchIn(lifecycleScope)

        binding.onboardingViewPager.adapter = onboardingAdapter
        binding.onboardingViewPager.registerOnPageChangeCallback(onPageChangeCallback)
        binding.onboardingButton.setOnClickListener {
            if (binding.onboardingViewPager.currentItem == onboardingAdapter.itemCount - 1) {
                finishOnboardingActivity()
            } else {
                binding.onboardingViewPager.currentItem = binding.onboardingViewPager.currentItem + 1
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_onboarding, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.skip) finishOnboardingActivity()
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        // The user shouldn't be able to exit the onboarding with the back button
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.onboardingViewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    private fun setOnboardingAdapterItems(onboardingItemsList: List<OnboardingItemUiModel>) {
        onboardingAdapter.submitList(onboardingItemsList)
    }

    private fun finishOnboardingActivity() {
        viewModel.saveOnboardingShown()
        finish()
    }
}
