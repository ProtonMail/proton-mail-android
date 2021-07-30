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
package ch.protonmail.android.settings.presentation

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.viewModels
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.settings.data.toLocalSwipeActionUiModel
import ch.protonmail.android.utils.extensions.app
import dagger.hilt.android.AndroidEntryPoint
import me.proton.core.mailsettings.domain.entity.SwipeAction
import ch.protonmail.android.adapters.swipe.SwipeAction as SwipeActionLocal

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

    private val swipeActionsViewModel: SwipeActionsViewModel by viewModels()

    private val swipeRadioGroup by lazy { findViewById<RadioGroup>(R.id.swipeRadioGroup) }

    override fun getLayoutId() = R.layout.activity_swipe_chooser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        actionBar?.elevation = elevation

        if (swipeActionsViewModel.swipeId == SwipeType.LEFT) {
            actionBar?.title = getString(R.string.settings_swipe_right_to_left)
        } else if (swipeActionsViewModel.swipeId == SwipeType.RIGHT) {
            actionBar?.title = getString(R.string.settings_swipe_left_to_right)
        }
        createActions()
    }

    override fun onStart() {
        super.onStart()
        app.bus.register(this)
    }

    override fun onStop() {
        super.onStop()
        app.bus.unregister(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.save_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                saveAndFinish()
                true
            }
            R.id.save -> {
                swipeActionsViewModel.onSaveClicked()
                saveAndFinish()
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    override fun onBackPressed() {
        saveAndFinish()
    }

    private fun createActions() {

        val availableActions = arrayOf(
            getString(SwipeActionLocal.MARK_READ.actionDescription), getString(SwipeActionLocal.STAR.actionDescription),
            getString(SwipeActionLocal.TRASH.actionDescription), getString(SwipeActionLocal.ARCHIVE.actionDescription),
            getString(SwipeActionLocal.SPAM.actionDescription)
        )

        for (index in availableActions.indices) {
            val swipeAction = availableActions[index]
            if (getString(swipeActionsViewModel.currentAction.toLocalSwipeActionUiModel().actionDescription)
                == swipeAction
            ) {
                (swipeRadioGroup.getChildAt(index) as RadioButton).isChecked = true
            }
        }
        swipeRadioGroup?.setOnCheckedChangeListener { _, _ ->
            swipeActionsViewModel.currentAction = when (swipeRadioGroup.checkedRadioButtonId) {
                R.id.read_unread -> SwipeAction.MarkRead
                R.id.star_unstar -> SwipeAction.Star
                R.id.trash -> SwipeAction.Trash
                R.id.move_to_archive -> SwipeAction.Archive
                R.id.move_to_spam -> SwipeAction.Spam
                else -> throw IllegalArgumentException("Unknown button id")
            }
        }
    }

    private fun saveAndFinish() {
        setResult(Activity.RESULT_OK, intent)
        saveLastInteraction()
        finish()
    }
}
