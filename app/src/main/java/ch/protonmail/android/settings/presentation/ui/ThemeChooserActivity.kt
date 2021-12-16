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

package ch.protonmail.android.settings.presentation.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.databinding.ActivityThemeChooserBinding
import ch.protonmail.android.settings.domain.model.AppThemeSettings
import ch.protonmail.android.settings.presentation.viewmodel.ThemeChooserViewModel
import ch.protonmail.android.settings.presentation.viewmodel.ThemeChooserViewModel.Action
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.util.kotlin.unsupported

@AndroidEntryPoint
class ThemeChooserActivity : AppCompatActivity() {

    private val viewModel: ThemeChooserViewModel by viewModels()
    private lateinit var binding: ActivityThemeChooserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeChooserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appThemeToolbar)

        viewModel.state
            .onEach(::updateThemeSelection)
            .launchIn(lifecycleScope)

        binding.appThemeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val action = when (checkedId) {
                R.id.app_theme_light_radio_button -> Action.SetLightTheme
                R.id.app_theme_dark_radio_button -> Action.SetDarkTheme
                R.id.app_theme_system_radio_button -> Action.SetSystemTheme
                else -> unsupported
            }
            viewModel.process(action)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) onBackPressed()
        else return super.onOptionsItemSelected(item)
        return true
    }

    private fun updateThemeSelection(state: ThemeChooserViewModel.State) {
        val theme = when (state) {
            ThemeChooserViewModel.State.Loading -> return
            is ThemeChooserViewModel.State.Data -> state.settings
        }
        val radioButton = when (theme) {
            AppThemeSettings.LIGHT -> binding.appThemeLightRadioButton
            AppThemeSettings.DARK -> binding.appThemeDarkRadioButton
            AppThemeSettings.FOLLOW_SYSTEM -> binding.appThemeSystemRadioButton
        }
        binding.appThemeRadioGroup.check(radioButton.id)
    }

    class Launcher : ActivityResultContract<Unit, Unit>() {

        override fun createIntent(context: Context, input: Unit) = Intent(context, ThemeChooserActivity::class.java)
        override fun parseResult(resultCode: Int, intent: Intent?) {}
    }
}
