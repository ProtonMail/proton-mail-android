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
package ch.protonmail.android.api.local

import android.content.Context
import android.content.SharedPreferences
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import java.util.*

// region constants
private const val DEFAULT_TIME = 0

private const val PREF_SNOOZE_SCHEDULED = "snooze_scheduled"
private const val PREF_SNOOZE_SCHEDULED_START_TIME = "snooze_scheduled_start_time"
private const val PREF_SNOOZE_SCHEDULED_END_TIME = "snooze_scheduled_end_time"
private const val PREF_SNOOZE_SCHEDULED_REPEAT_DAYS = "snooze_scheduled_repeat_days"
private const val PREF_SNOOZE_QUICK = "snooze_quick"
private const val PREF_SNOOZE_QUICK_END_TIME = "snooze_quick_end_time"
// endregion

class SnoozeSettings(
        var snoozeScheduled: Boolean = false,
        var snoozeQuick: Boolean = false,
        var snoozeQuickEndTime: Long = 0L,
        var snoozeScheduledStartTimeHour: Int,
        var snoozeScheduledStartTimeMinute: Int,
        var snoozeScheduledEndTimeHour: Int,
        var snoozeScheduledEndTimeMinute: Int,
        var snoozeScheduledRepeatingDays: String?
) {


    companion object {

        fun load(username: String): SnoozeSettings {
            val pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(username)
            val defaultStartTimeString = ProtonMailApplication.getApplication().resources.getString(R.string.repeating_snooze_default_start_time)
            val defaultEndTimeString = ProtonMailApplication.getApplication().resources.getString(R.string.repeating_snooze_default_end_time)

            copySnoozeEntriesTo(pref, defaultStartTimeString, defaultEndTimeString)

            val scheduledStartTime = pref.getString(PREF_SNOOZE_SCHEDULED_START_TIME, defaultStartTimeString)
            var snoozeScheduledStartTimeHour = DEFAULT_TIME
            var snoozeScheduledStartTimeMinute = DEFAULT_TIME
            if (!scheduledStartTime.isNullOrBlank()) {
                val hour = Integer.valueOf(scheduledStartTime.split(":")[0])
                val minute = Integer.valueOf(scheduledStartTime.split(":")[1])
                snoozeScheduledStartTimeHour = hour
                snoozeScheduledStartTimeMinute = minute
            }

            val scheduledEndTime = pref.getString(PREF_SNOOZE_SCHEDULED_END_TIME, defaultEndTimeString)
            var snoozeScheduledEndTimeHour = DEFAULT_TIME
            var snoozeScheduledEndTimeMinute = DEFAULT_TIME
            if (!scheduledEndTime.isNullOrEmpty()) {
                val hour = Integer.valueOf(scheduledEndTime.split(":")[0])
                val minute = Integer.valueOf(scheduledEndTime.split(":")[1])
                snoozeScheduledEndTimeHour = hour
                snoozeScheduledEndTimeMinute = minute
            }
            val snoozeScheduledRepeatingDays = pref.getString(PREF_SNOOZE_SCHEDULED_REPEAT_DAYS, null)

            return SnoozeSettings(snoozeScheduled = pref.getBoolean(PREF_SNOOZE_SCHEDULED, false),
                    snoozeQuick = pref.getBoolean(PREF_SNOOZE_QUICK, false),
                    snoozeQuickEndTime = pref.getLong(PREF_SNOOZE_QUICK_END_TIME, 0L),
                    snoozeScheduledStartTimeHour = snoozeScheduledStartTimeHour,
                    snoozeScheduledStartTimeMinute = snoozeScheduledStartTimeMinute,
                    snoozeScheduledEndTimeHour = snoozeScheduledEndTimeHour,
                    snoozeScheduledEndTimeMinute = snoozeScheduledEndTimeMinute,
                    snoozeScheduledRepeatingDays = snoozeScheduledRepeatingDays)
        }

        /**
         * Migration for snooze setting from different file from SharedPreferences.
         */
        private fun copySnoozeEntriesTo(sharedPreferences: SharedPreferences, defaultStartTimeString: String, defaultEndTimeString: String) {
            val pref = ProtonMailApplication.getApplication().getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
            if (pref.contains(PREF_SNOOZE_SCHEDULED_START_TIME)) {
                sharedPreferences.edit().putString(PREF_SNOOZE_SCHEDULED_START_TIME, pref.getString(PREF_SNOOZE_SCHEDULED_START_TIME, defaultStartTimeString)).apply()
                pref.edit().remove(PREF_SNOOZE_SCHEDULED_START_TIME).apply()
            }

            if (pref.contains(PREF_SNOOZE_SCHEDULED_END_TIME)) {
                sharedPreferences.edit().putString(PREF_SNOOZE_SCHEDULED_END_TIME, pref.getString(PREF_SNOOZE_SCHEDULED_END_TIME, defaultEndTimeString)).apply()
                pref.edit().remove(PREF_SNOOZE_SCHEDULED_END_TIME).apply()
            }

            if (pref.contains(PREF_SNOOZE_SCHEDULED_REPEAT_DAYS)) {
                sharedPreferences.edit().putString(PREF_SNOOZE_SCHEDULED_REPEAT_DAYS, pref.getString(PREF_SNOOZE_SCHEDULED_REPEAT_DAYS, null)).apply()
                pref.edit().remove(PREF_SNOOZE_SCHEDULED_REPEAT_DAYS).apply()
            }

            if (pref.contains(PREF_SNOOZE_SCHEDULED)) {
                sharedPreferences.edit().putBoolean(PREF_SNOOZE_SCHEDULED, pref.getBoolean(PREF_SNOOZE_SCHEDULED, false)).apply()
                pref.edit().remove(PREF_SNOOZE_SCHEDULED).apply()
            }

            if (pref.contains(PREF_SNOOZE_QUICK)) {
                sharedPreferences.edit().putBoolean(PREF_SNOOZE_QUICK, pref.getBoolean(PREF_SNOOZE_QUICK, false)).apply()
                pref.edit().remove(PREF_SNOOZE_QUICK).apply()
            }

            if (pref.contains(PREF_SNOOZE_QUICK_END_TIME)) {
                sharedPreferences.edit().putLong(PREF_SNOOZE_QUICK_END_TIME, pref.getLong(PREF_SNOOZE_QUICK_END_TIME, 0L)).apply()
                pref.edit().remove(PREF_SNOOZE_QUICK_END_TIME).apply()
            }

        }


    }

    fun save(username: String) {
        saveScheduledBackup(username)
        saveScheduledSnoozeStartTimeBackup(username)
        saveScheduledSnoozeEndTimeBackup(username)
        saveScheduledSnoozeRepeatingDaysBackup(username)
    }

    fun getScheduledSnooze(username: String): Boolean {
        val pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(username)
        return pref.getBoolean(PREF_SNOOZE_SCHEDULED, false)
    }

    private fun saveScheduledBackup(username: String) {
        val pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(username)
        pref.edit().putBoolean(PREF_SNOOZE_SCHEDULED, snoozeScheduled).apply()
    }

    fun saveQuickSnoozeBackup(username: String) {
        val pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(username)
        pref.edit().putBoolean(PREF_SNOOZE_QUICK, snoozeQuick).apply()
    }

    fun saveQuickSnoozeEndTimeBackup(username: String) {
        val pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(username)
        pref.edit().putLong(PREF_SNOOZE_QUICK_END_TIME, snoozeQuickEndTime).apply()
    }

    private fun saveScheduledSnoozeStartTimeBackup(username: String) {
        val pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(username)
        pref.edit().putString(PREF_SNOOZE_SCHEDULED_START_TIME, "$snoozeScheduledStartTimeHour:$snoozeScheduledStartTimeMinute").apply()
    }

    private fun saveScheduledSnoozeEndTimeBackup(username: String) {
        val pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(username)
        pref.edit().putString(PREF_SNOOZE_SCHEDULED_END_TIME, "$snoozeScheduledEndTimeHour:$snoozeScheduledEndTimeMinute").apply()
    }

    private fun saveScheduledSnoozeRepeatingDaysBackup(username: String) {
        val pref = ProtonMailApplication.getApplication().getSecureSharedPreferences(username)
        pref.edit().putString(PREF_SNOOZE_SCHEDULED_REPEAT_DAYS, snoozeScheduledRepeatingDays).apply()
    }

    /**
     * Determines if user's scheduled snooze settings should cause notifications to be suppressed.
     */
    fun shouldSuppressNotification(time: Calendar): Boolean {

        val days = mapOf<Int, CharSequence>(Calendar.MONDAY to "mo",
                Calendar.TUESDAY to "tu",
                Calendar.WEDNESDAY to "we",
                Calendar.THURSDAY to "th",
                Calendar.FRIDAY to "fr",
                Calendar.SATURDAY to "sa",
                Calendar.SUNDAY to "su")

        val currentTimestamp = time.get(Calendar.HOUR_OF_DAY) * 60 + time.get(Calendar.MINUTE)
        val startTimestamp = snoozeScheduledStartTimeHour * 60 + snoozeScheduledStartTimeMinute
        val endTimestamp = snoozeScheduledEndTimeHour * 60 + snoozeScheduledEndTimeMinute

        var isWithinSnoozeWindow = false
        var isOnCorrectDay = false

        if (startTimestamp < endTimestamp) { // snooze happens during the day
            isWithinSnoozeWindow = (currentTimestamp >= startTimestamp) && (currentTimestamp < endTimestamp)
            isOnCorrectDay = snoozeScheduledRepeatingDays?.contains(days[time.get(Calendar.DAY_OF_WEEK)]
                    ?: "xx") ?: false
        } else { // snooze happens over the midnight
            if (currentTimestamp >= startTimestamp) {
                isWithinSnoozeWindow = true
                isOnCorrectDay = snoozeScheduledRepeatingDays?.contains(days[time.get(Calendar.DAY_OF_WEEK)]
                        ?: "xx") ?: false
            } else if (currentTimestamp < endTimestamp) {
                isWithinSnoozeWindow = true
                time.add(Calendar.DAY_OF_WEEK, -1)
                isOnCorrectDay = snoozeScheduledRepeatingDays?.contains(days[time.get(Calendar.DAY_OF_WEEK)]
                        ?: "xx") ?: false
            }
        }

        return snoozeScheduled && isWithinSnoozeWindow && isOnCorrectDay
    }
}
