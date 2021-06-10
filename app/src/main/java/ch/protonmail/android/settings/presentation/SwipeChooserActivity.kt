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
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.adapters.swipe.SwipeAction
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.jobs.UpdateSettingsJob
import ch.protonmail.android.prefs.SecureSharedPreferences

// region constants
const val EXTRA_CURRENT_ACTION = "extra.current.action"
const val EXTRA_SWIPE_ID = "EXTRA_SWIPE_ID"
// endregion

enum class SwipeType {

    RIGHT,
    LEFT
}

class SwipeChooserActivity : BaseActivity() {

    private var currentAction: Int = 0
    private var swipeId: SwipeType = SwipeType.RIGHT
    private var mailSettings: MailSettings? = null
    private var actionLeftSwipeChanged = false
    private var actionRightSwipeChanged = false

    private val swipeRadioGroup by lazy { findViewById<RadioGroup>(R.id.swipeRadioGroup) }

    override fun getLayoutId(): Int {
        return R.layout.activity_swipe_chooser
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        actionBar?.elevation = elevation

        currentAction = intent.getIntExtra(EXTRA_CURRENT_ACTION, 0)
        swipeId = intent.getSerializableExtra(EXTRA_SWIPE_ID) as SwipeType
        mailSettings = checkNotNull(mUserManager.getCurrentUserMailSettingsBlocking())

        if (swipeId == SwipeType.LEFT) {
            actionBar?.title = getString(R.string.settings_swipe_right_to_left)
        } else if (swipeId == SwipeType.RIGHT) {
            actionBar?.title = getString(R.string.settings_swipe_left_to_right)
        }
        createActions()
    }

    override fun onStart() {
        super.onStart()
        ProtonMailApplication.getApplication().bus.register(this)
    }

    override fun onStop() {
        super.onStop()
        ProtonMailApplication.getApplication().bus.unregister(this)
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
                val userPreferences =
                    SecureSharedPreferences.getPrefsForUser(this, mUserManager.requireCurrentUserId())
                mailSettings?.saveBlocking(userPreferences)
                val job = UpdateSettingsJob(
                    actionLeftSwipeChanged = actionLeftSwipeChanged,
                    actionRightSwipeChanged = actionRightSwipeChanged
                )
                mJobManager.addJobInBackground(job)
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
            getString(SwipeAction.MARK_READ.actionDescription), getString(SwipeAction.STAR.actionDescription),
            getString(SwipeAction.TRASH.actionDescription), getString(SwipeAction.ARCHIVE.actionDescription),
            getString(SwipeAction.SPAM.actionDescription)
        )

        for (index in availableActions.indices) {
            val swipeAction = availableActions[index]
            if (getString(SwipeAction.values()[currentAction].actionDescription) == swipeAction) {
                (swipeRadioGroup.getChildAt(index) as RadioButton).isChecked = true
            }
        }

        swipeRadioGroup!!.setOnCheckedChangeListener { _, _ ->
            when (swipeRadioGroup!!.checkedRadioButtonId) {
                R.id.read_unread -> {
                    currentAction = SwipeAction.MARK_READ.ordinal
                }
                R.id.star_unstar -> {
                    currentAction = SwipeAction.STAR.ordinal
                }
                R.id.trash -> {
                    currentAction = SwipeAction.TRASH.ordinal
                }
                R.id.move_to_archive -> {
                    currentAction = SwipeAction.ARCHIVE.ordinal
                }
                R.id.move_to_spam -> {
                    currentAction = SwipeAction.SPAM.ordinal
                }
            }

            if (swipeId == SwipeType.LEFT) {
                actionLeftSwipeChanged = currentAction != mailSettings?.leftSwipeAction
                if (actionLeftSwipeChanged) {
                    mailSettings?.leftSwipeAction = currentAction
                }
            } else if (swipeId == SwipeType.RIGHT) {
                actionRightSwipeChanged = currentAction != mailSettings?.rightSwipeAction
                if (actionRightSwipeChanged) {
                    mailSettings?.rightSwipeAction = currentAction
                }
            }
        }
    }

    private fun saveAndFinish() {
        setResult(Activity.RESULT_OK, intent)
        saveLastInteraction()
        finish()
    }
}
