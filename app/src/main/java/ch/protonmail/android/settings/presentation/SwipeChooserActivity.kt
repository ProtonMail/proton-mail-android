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
import ch.protonmail.android.settings.data.toMailSwipeAction
import ch.protonmail.android.utils.extensions.app
import dagger.hilt.android.AndroidEntryPoint
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

        if (swipeChooserViewModel.swipeId == SwipeType.LEFT) {
            actionBar?.title = getString(R.string.settings_swipe_right_to_left)
        } else if (swipeChooserViewModel.swipeId == SwipeType.RIGHT) {
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
        menu.findItem(R.id.save).isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                swipeChooserViewModel.onSaveClicked()
                saveAndFinish()
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    override fun onBackPressed() {
        swipeChooserViewModel.onSaveClicked()
        saveAndFinish()
    }

    private fun createActions() {

        val availableActions = arrayOf(
            MailSwipeAction.MARK_READ,
            MailSwipeAction.UPDATE_STAR,
            MailSwipeAction.TRASH,
            MailSwipeAction.ARCHIVE,
            MailSwipeAction.SPAM
        )

        for (index in availableActions.indices) {
            val swipeAction = availableActions[index]
            if (swipeChooserViewModel.currentAction.toMailSwipeAction() == swipeAction) {
                (swipeRadioGroup.getChildAt(index) as RadioButton).isChecked = true
            }
        }
        swipeRadioGroup?.setOnCheckedChangeListener { _, _ ->
            swipeChooserViewModel.currentAction = when (swipeRadioGroup.checkedRadioButtonId) {
                R.id.read_unread -> CoreSwipeAction.MarkRead
                R.id.star_unstar -> CoreSwipeAction.Star
                R.id.trash -> CoreSwipeAction.Trash
                R.id.move_to_archive -> CoreSwipeAction.Archive
                R.id.move_to_spam -> CoreSwipeAction.Spam
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
