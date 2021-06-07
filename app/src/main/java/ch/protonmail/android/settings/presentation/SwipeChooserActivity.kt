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
import android.view.MenuItem
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.adapters.swipe.SwipeAction
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.jobs.UpdateSettingsJob
import ch.protonmail.android.prefs.SecureSharedPreferences
import kotlinx.android.synthetic.main.activity_swipe_chooser.*

// region constants
const val EXTRA_CURRENT_ACTION = "EXTRA_CURRENT_ACTION"
const val EXTRA_SWIPE_ID = "EXTRA_SWIPE_ID"
// endregion

enum class SwipeType {

    RIGHT,
    LEFT
}

class SwipeChooserActivity : BaseActivity() {

    private var mCurrentAction: Int = 0
    private var mSwipeId: SwipeType = SwipeType.RIGHT

    override fun getLayoutId(): Int {
        return R.layout.activity_swipe_chooser
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimensionPixelSize(R.dimen.action_bar_elevation).toFloat()
        actionBar?.elevation = elevation

        mCurrentAction = intent.getIntExtra(EXTRA_CURRENT_ACTION, 0)
        mSwipeId = intent.getSerializableExtra(EXTRA_SWIPE_ID) as SwipeType
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            saveAndFinish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        saveAndFinish()
    }

    private fun createActions() {

        swipeRadioGroup!!.setOnCheckedChangeListener { _, _ ->
            when (swipeRadioGroup!!.checkedRadioButtonId) {
                R.id.none -> {
                }
                R.id.read_unread -> {
                    mCurrentAction = SwipeAction.MARK_READ.ordinal
                }
                R.id.star_unstar -> {
                    mCurrentAction = SwipeAction.STAR.ordinal
                }
                R.id.trash -> {
                    mCurrentAction = SwipeAction.TRASH.ordinal
                }
                R.id.label_as -> {
                }
                R.id.move_to -> {
                }
                R.id.move_to_archive -> {
                    mCurrentAction = SwipeAction.ARCHIVE.ordinal
                }
                R.id.move_to_spam -> {
                    mCurrentAction = SwipeAction.SPAM.ordinal
                }
            }
            var actionLeftSwipeChanged = false
            var actionRightSwipeChanged = false

            val mailSettings = checkNotNull(mUserManager.getCurrentUserMailSettingsBlocking())
            if (mSwipeId == SwipeType.LEFT) {
                actionLeftSwipeChanged = mCurrentAction != mailSettings.leftSwipeAction
                if (actionLeftSwipeChanged) {
                    mailSettings.leftSwipeAction = mCurrentAction
                }
            } else if (mSwipeId == SwipeType.RIGHT) {
                actionRightSwipeChanged = mCurrentAction != mailSettings.rightSwipeAction
                if (actionRightSwipeChanged) {
                    mailSettings.rightSwipeAction = mCurrentAction
                }
            }
            val userPreferences =
                SecureSharedPreferences.getPrefsForUser(this, mUserManager.requireCurrentUserId())
            mailSettings.saveBlocking(userPreferences)
            val job = UpdateSettingsJob(
                actionLeftSwipeChanged = actionLeftSwipeChanged,
                actionRightSwipeChanged = actionRightSwipeChanged
            )
            mJobManager.addJobInBackground(job)
        }
    }

    private fun saveAndFinish() {
        setResult(Activity.RESULT_OK, intent)
        saveLastInteraction()
        finish()
    }
}
