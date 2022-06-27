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
package ch.protonmail.android.settings.presentation.showmoved

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.settings.presentation.showmoved.viewmodel.ShowMovedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.settings_show_moved_activity.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.presentation.utils.showToast

@AndroidEntryPoint
class ShowMovedActivity : BaseActivity() {

    private val showMovedViewModel: ShowMovedViewModel by viewModels()

    override fun getLayoutId() = R.layout.settings_show_moved_activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        actionBar?.elevation = elevation

        actionBar?.title = getString(R.string.settings_show_moved_setting_title)

        lifecycleScope.launch {
            showMovedViewModel.state
                .flowWithLifecycle(lifecycle)
                .collect(::handleState)
        }

        showMovedViewModel.setSettingCurrentValue()
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return if (menuItem.itemId == android.R.id.home) {
            saveAndFinish()
            true
        } else super.onOptionsItemSelected(menuItem)
    }

    override fun onBackPressed() {
        saveAndFinish()
    }

    private fun handleState(state: ShowMovedViewModel.State) {
        when (state) {
            is ShowMovedViewModel.State.Idle -> { /* noop */
            }
            is ShowMovedViewModel.State.Fetched -> {
                actionSwitch.isChecked = when (state.showMoved) {
                    ShowMoved.None -> false
                    ShowMoved.Both -> true
                    else -> true
                }
                actionSwitch.setOnCheckedChangeListener { _, isChecked ->
                    val valueToSave = when (isChecked) {
                        true -> ShowMoved.Both
                        false -> ShowMoved.None
                    }
                    showMovedViewModel.onToggle(valueToSave)
                }
            }
            is ShowMovedViewModel.State.Saving -> showToast("Saving")
            is ShowMovedViewModel.State.Success -> showToast("Saved")
            is ShowMovedViewModel.State.GenericError -> {
                showToast("Cannot save")
                showMovedViewModel.setSettingCurrentValue()
            }
        }
    }

    private fun saveAndFinish() {
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    class Launcher : ActivityResultContract<Unit, Unit>() {

        override fun createIntent(context: Context, input: Unit) = Intent(context, ShowMovedActivity::class.java)
        override fun parseResult(resultCode: Int, intent: Intent?) {}
    }
}
