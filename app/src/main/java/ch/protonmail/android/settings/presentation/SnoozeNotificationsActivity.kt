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
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.isVisible
import butterknife.OnClick
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.utils.extensions.buildRepeatingDaysString
import ch.protonmail.android.utils.extensions.buildUILabel
import ch.protonmail.android.utils.extensions.countSelected
import ch.protonmail.android.utils.extensions.countTrue
import ch.protonmail.android.utils.extensions.roundHourOrMinute
import ch.protonmail.android.utils.extensions.selectAll
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import kotlinx.android.synthetic.main.activity_snooze_notifications.*
import java.util.ArrayList
import java.util.Arrays

// region constants
private const val TAG_START_TIME_PICKED = "StartTimePicker"
private const val TAG_END_TIME_PICKED = "EndTimePicker"

private const val DAYS_OF_THE_WEEK = "mo:tu:we:th:fr:sa:su"
// endregion

class SnoozeNotificationsActivity : BaseActivity() {

    private val startTimePicker: TimePickerDialog by lazy {
        val repeatingSnoozeDefaultStartHour = resources.getInteger(R.integer.repeating_snooze_start_hour)
        TimePickerDialog.newInstance(mStartTimePickerListener, repeatingSnoozeDefaultStartHour, 0, true)
    }
    private val endTimePicker: TimePickerDialog by lazy {
        val repeatingSnoozeDefaultEndHour = resources.getInteger(R.integer.repeating_snooze_end_hour)
        TimePickerDialog.newInstance(mEndTimePickerListener, repeatingSnoozeDefaultEndHour, 0, true)
    }

    private var quickSnoozeEnabled: Boolean = false
    private var snoozeScheduledEnabled: Boolean = false
    private var startTimeHour: Int = 0
    private var startTimeMinute: Int = 0
    private var endTimeHour: Int = 0
    private var endTimeMinute: Int = 0
    private var repeatingDays: String? = null
    private var chosenDays: MutableList<String>? = null

    private lateinit var dayViewsList: List<SnoozeRepeatDayView>

    private val scheduledSnoozeCheckListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        snoozeScheduledEnabled = isChecked
        notificationsSnoozeScheduledContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        if (mUserManager != null) {
            mUserManager.setSnoozeScheduledBlocking(
                isChecked,
                startTimeHour,
                startTimeMinute,
                endTimeHour,
                endTimeMinute,
                buildRepeatingDaysString()
            )
        }
        setCurrentStatus()
        if (isChecked) {
            selectAllDaysIfAllAreUnselected()
        }
    }

    private val mStartTimePickerListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute, _ ->
        startTimeHour = hourOfDay
        startTimeMinute = minute

        snoozeStartTime.text = "${startTimeHour.roundHourOrMinute()} : ${startTimeMinute.roundHourOrMinute()}"
    }

    private val mEndTimePickerListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute, _ ->
        endTimeHour = hourOfDay
        endTimeMinute = minute

        snoozeEndTime.text = "${endTimeHour.roundHourOrMinute()} : ${endTimeMinute.roundHourOrMinute()}"
    }

    private val dayClickListener = View.OnClickListener { dayView ->
        dayView.isSelected = !dayView.isSelected
        setRepeatingDaysLabel()
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_snooze_notifications
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        actionBar?.elevation = elevation

        dayViewsList = listOf(monday, tuesday, wednesday, thursday, friday, saturday, sunday)

        startTimePicker.version = TimePickerDialog.Version.VERSION_2
        endTimePicker.version = TimePickerDialog.Version.VERSION_2

        snoozeScheduledEnabled = mUserManager.isSnoozeScheduledEnabled()
        quickSnoozeEnabled = mUserManager.isSnoozeQuickEnabledBlocking()
        val snoozeSettings = mUserManager.snoozeSettings
        snoozeSettings?.let {
            startTimeHour = it.snoozeScheduledStartTimeHour
            startTimeMinute = it.snoozeScheduledStartTimeMinute
            endTimeHour = it.snoozeScheduledEndTimeHour
            endTimeMinute = it.snoozeScheduledEndTimeMinute
            repeatingDays = it.snoozeScheduledRepeatingDays
        }
        if (repeatingDays == null) {
            repeatingDays = DAYS_OF_THE_WEEK
        }
        chosenDays = Arrays.asList(
            *repeatingDays!!.split(resources.getString(R.string.default_delimiter).toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        snoozeStartTime.text = "${startTimeHour.roundHourOrMinute()} : ${startTimeMinute.roundHourOrMinute()}"
        snoozeEndTime.text = "${endTimeHour.roundHourOrMinute()} : ${endTimeMinute.roundHourOrMinute()}"
        setCurrentStatus()
        setRepeatingDaysStatus()
        setRepeatingDaysLabel()

        notificationSnoozeScheduledSwitch.isChecked = snoozeScheduledEnabled
        notificationsSnoozeScheduledContainer.isVisible = snoozeScheduledEnabled

        dayViewsList.forEach {
            it.setOnClickListener(dayClickListener)
        }

        notificationSnoozeScheduledSwitch.setOnCheckedChangeListener(scheduledSnoozeCheckListener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                saveAndFinish()
                true
            }
            else -> false
        }
    }

    override fun onStart() {
        super.onStart()
        mApp.bus.register(this)
    }

    override fun onStop() {
        super.onStop()
        mApp.bus.unregister(this)
        mUserManager.setSnoozeScheduledBlocking(
            snoozeScheduledEnabled,
            startTimeHour,
            startTimeMinute,
            endTimeHour,
            endTimeMinute,
            buildRepeatingDaysString()
        )
    }

    override fun onBackPressed() {
        saveAndFinish()
    }

    @OnClick(R.id.snoozeStartTime, R.id.snooze_start_time_container)
    fun onStartTimeClicked() {
        startTimePicker.show(fragmentManager, TAG_START_TIME_PICKED)
    }

    @OnClick(R.id.snoozeEndTime, R.id.snooze_end_time_container)
    fun onEndTimeClicked() {
        endTimePicker.show(fragmentManager, TAG_END_TIME_PICKED)
    }

    private fun setRepeatingDaysStatus() {
        for (dayView in dayViewsList) {
            dayView.setSelected(chosenDays!!)
        }
    }

    /**
     * Sets the label for the days that are selected for repeating.
     */
    private fun setRepeatingDaysLabel() {
        val stringBuilder = StringBuilder()
        val daysSelected = Array(7) {
            false
        }
        for (i in dayViewsList.indices) {
            val view = dayViewsList[i]
            if (view.isSelected) {
                daysSelected[i] = true
                if (stringBuilder.isNotEmpty()) {
                    stringBuilder.append(", ")
                }
                stringBuilder.append(view.text)
            }
        }

        if (daysSelected.countTrue() == 0) {
            notificationSnoozeScheduledSwitch.isChecked = false
        }
        repeatingDaysLabel.text = daysSelected.buildUILabel(this, stringBuilder.toString())
    }

    /**
     * Selects all days if all are unselected, since that state is not logical nor allowed.
     */
    private fun selectAllDaysIfAllAreUnselected() {
        val allDaysUnselected = dayViewsList.countSelected() == 0
        if (allDaysUnselected) {
            dayViewsList.selectAll()
            repeatingDaysLabel.setText(R.string.every_day)
        }
    }

    private fun buildRepeatingDaysString(): String {
        chosenDays = ArrayList()
        for (dayView in dayViewsList) {
            if (dayView.isSelected) {
                chosenDays!!.add(dayView.code)
            }
        }
        return chosenDays.buildRepeatingDaysString(":")
    }

    private fun setCurrentStatus() {
        if (quickSnoozeEnabled && snoozeScheduledEnabled) {
            notificationsSnoozeCurrentStatus.text = getString(R.string.quick_and_scheduled_enabled)
        } else if (quickSnoozeEnabled) {
            notificationsSnoozeCurrentStatus.text = getString(R.string.quick_only_enabled)
        } else if (snoozeScheduledEnabled) {
            notificationsSnoozeCurrentStatus.text = getString(R.string.scheduled_only_enabled)
        } else {
            notificationsSnoozeCurrentStatus.text = getString(R.string.quick_and_scheduled_disabled)
        }
    }

    private fun saveAndFinish() {
        setResult(Activity.RESULT_OK)
        saveLastInteraction()
        finish()
    }
}
