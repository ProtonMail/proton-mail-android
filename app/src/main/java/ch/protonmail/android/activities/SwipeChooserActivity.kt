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
package ch.protonmail.android.activities

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import ch.protonmail.android.R
import ch.protonmail.android.adapters.swipe.SwipeAction
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.jobs.UpdateSettingsJob
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.utils.moveToLogin
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.activity_swipe_chooser.*

// region constants
const val EXTRA_CURRENT_ACTION = "EXTRA_CURRENT_ACTION"
const val EXTRA_SWIPE_ID = "EXTRA_SWIPE_ID"
// endregion

/**
 * Created by dkadrikj on 10/24/16.
 */

enum class SwipeType {
    RIGHT,
    LEFT
}

class SwipeChooserActivity : BaseActivity() {

    private var mAvailableActions: Array<String>? = null

    private var mSwipeActionsIds: IntArray? = null
    private var mInflater: LayoutInflater? = null
    private var mCurrentAction: Int = 0
    private var mSwipeId: SwipeType = SwipeType.RIGHT

    override fun getLayoutId(): Int {
        return R.layout.activity_swipe_chooser
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        mCurrentAction = intent.getIntExtra(EXTRA_CURRENT_ACTION, 0)
        mSwipeId = intent.getSerializableExtra(EXTRA_SWIPE_ID) as SwipeType
        mInflater = LayoutInflater.from(this)
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

        mAvailableActions = arrayOf(getString(SwipeAction.TRASH.actionDescription), getString(SwipeAction.SPAM.actionDescription), getString(SwipeAction.STAR.actionDescription), getString(SwipeAction.ARCHIVE.actionDescription), getString(SwipeAction.MARK_READ.actionDescription))

        mSwipeActionsIds = IntArray(mAvailableActions!!.size)
        for (i in mAvailableActions!!.indices) {
            val swipeAction = mAvailableActions!![i]
            val radioButton = mInflater!!.inflate(R.layout.swipe_list_item, swipeRadioGroup, false) as RadioButton
            radioButton.id = View.generateViewId()

            mSwipeActionsIds!![i] = radioButton.id
            radioButton.text = swipeAction
            swipeRadioGroup!!.addView(radioButton)
            val divider = mInflater!!.inflate(R.layout.horizontal_divider, swipeRadioGroup, false)
            swipeRadioGroup!!.addView(divider)
            radioButton.isChecked = getString(SwipeAction.values()[mCurrentAction].actionDescription) == swipeAction
        }

        swipeRadioGroup!!.setOnCheckedChangeListener { _, _ ->
            val selectedOption = swipeRadioGroup!!.checkedRadioButtonId
            for (i in mSwipeActionsIds!!.indices) {
                if (mSwipeActionsIds!![i] == selectedOption) {
                    mCurrentAction = i
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
                    val job = UpdateSettingsJob(actionRightSwipeChanged = actionRightSwipeChanged,
                            actionLeftSwipeChanged = actionLeftSwipeChanged)
                    mJobManager.addJobInBackground(job)
                    break
                }
            }
        }
    }

    private fun saveAndFinish() {
        setResult(Activity.RESULT_OK, intent)
        saveLastInteraction()
        finish()
    }

    @Subscribe
    fun onLogoutEvent(event: LogoutEvent) {
        moveToLogin()
    }
}
