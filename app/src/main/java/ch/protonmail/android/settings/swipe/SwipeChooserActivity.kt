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
package ch.protonmail.android.settings.swipe

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.settings.data.toMailSwipeAction
import ch.protonmail.android.settings.swipe.viewmodel.SwipeChooserViewModel
import ch.protonmail.android.utils.extensions.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ch.protonmail.android.adapters.swipe.SwipeAction as MailSwipeAction
import me.proton.core.mailsettings.domain.entity.SwipeAction as CoreSwipeAction

// region constants
const val EXTRA_CURRENT_ACTION = "extra.current.action"
const val EXTRA_SWIPE_ID = "EXTRA_SWIPE_ID"
// endregion

enum class SwipeType {
    RIGHT,
    LEFT
}

@AndroidEntryPoint
class SwipeChooserActivity : BaseActivity() {

    private val swipeChooserViewModel: SwipeChooserViewModel by viewModels()

    private val swipeRadioGroup by lazy { findViewById<RadioGroup>(R.id.swipeRadioGroup) }

    override fun getLayoutId() = R.layout.activity_swipe_chooser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        actionBar?.elevation = elevation

        val titleRes = when (intent.getSerializableExtra(EXTRA_SWIPE_ID) as SwipeType) {
            SwipeType.LEFT -> R.string.settings_swipe_right_to_left
            SwipeType.RIGHT -> R.string.settings_swipe_left_to_right
        }
        actionBar?.title = getString(titleRes)

        val currentAction = intent.getSerializableExtra(EXTRA_CURRENT_ACTION) as CoreSwipeAction
        createActions(currentAction.toMailSwipeAction())

        lifecycleScope.launch {
            swipeChooserViewModel.state
                .flowWithLifecycle(lifecycle)
                .collect(::handleState)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.save_menu, menu)
        menu.findItem(R.id.save).isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                swipeChooserViewModel.onSaveClicked()
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    override fun onBackPressed() {
        swipeChooserViewModel.onSaveClicked()
    }

    private fun createActions(currentAction: MailSwipeAction) {

        val availableActions = arrayOf(
            MailSwipeAction.MARK_READ,
            MailSwipeAction.UPDATE_STAR,
            MailSwipeAction.TRASH,
            MailSwipeAction.ARCHIVE,
            MailSwipeAction.SPAM
        )

        for (index in availableActions.indices) {
            val swipeAction = availableActions[index]
            if (currentAction == swipeAction) {
                (swipeRadioGroup.getChildAt(index) as RadioButton).isChecked = true
            }
        }
        swipeRadioGroup?.setOnCheckedChangeListener { _, _ ->
            val action = when (swipeRadioGroup.checkedRadioButtonId) {
                R.id.read_unread -> CoreSwipeAction.MarkRead
                R.id.star_unstar -> CoreSwipeAction.Star
                R.id.trash -> CoreSwipeAction.Trash
                R.id.move_to_archive -> CoreSwipeAction.Archive
                R.id.move_to_spam -> CoreSwipeAction.Spam
                else -> throw IllegalArgumentException("Unknown button id")
            }
            swipeChooserViewModel.setAction(action)
        }
    }

    private fun handleState(state: SwipeChooserViewModel.State) {
        when (state) {
            SwipeChooserViewModel.State.Idle -> { /* noop */ }
            SwipeChooserViewModel.State.Saving -> showToast(R.string.settings_swipe_saving)
            SwipeChooserViewModel.State.Success -> saveAndFinish()
            SwipeChooserViewModel.State.GenericError -> {
                showToast(R.string.settings_swipe_generic_error)
                saveAndFinish()
            }
        }
    }

    private fun saveAndFinish() {
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
