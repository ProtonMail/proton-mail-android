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
import kotlinx.coroutines.runBlocking
import me.proton.core.util.android.sharedpreferences.minusAssign
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.unsupported
import java.util.Calendar

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

        @Suppress("RedundantSuspendModifier") // Can't inject dispatcher for use `withContext`,
        //                                                  but still better than a blocking call
        suspend fun load(userPreferences: SharedPreferences): SnoozeSettings {
            val defaultStartTimeString =
                ProtonMailApplication.getApplication().getString(R.string.repeating_snooze_default_start_time)
            val defaultEndTimeString =
                ProtonMailApplication.getApplication().getString(R.string.repeating_snooze_default_end_time)

            copySnoozeEntriesTo(userPreferences, defaultStartTimeString, defaultEndTimeString)

            val scheduledStartTime = userPreferences.getString(PREF_SNOOZE_SCHEDULED_START_TIME, defaultStartTimeString)
            var snoozeScheduledStartTimeHour = DEFAULT_TIME
            var snoozeScheduledStartTimeMinute = DEFAULT_TIME
            if (!scheduledStartTime.isNullOrBlank()) {
                val (hour, minute) = scheduledStartTime.split(":")
                snoozeScheduledStartTimeHour = hour.toInt()
                snoozeScheduledStartTimeMinute = minute.toInt()
            }

            val scheduledEndTime = userPreferences.getString(PREF_SNOOZE_SCHEDULED_END_TIME, defaultEndTimeString)
            var snoozeScheduledEndTimeHour = DEFAULT_TIME
            var snoozeScheduledEndTimeMinute = DEFAULT_TIME
            if (!scheduledEndTime.isNullOrEmpty()) {
                val (hour, minute) = scheduledEndTime.split(":")
                snoozeScheduledEndTimeHour = hour.toInt()
                snoozeScheduledEndTimeMinute = minute.toInt()
            }
            val snoozeScheduledRepeatingDays =
                userPreferences.getString(PREF_SNOOZE_SCHEDULED_REPEAT_DAYS, null)

            return SnoozeSettings(
                snoozeScheduled = userPreferences.getBoolean(PREF_SNOOZE_SCHEDULED, false),
                snoozeQuick = userPreferences.getBoolean(PREF_SNOOZE_QUICK, false),
                snoozeQuickEndTime = userPreferences.getLong(PREF_SNOOZE_QUICK_END_TIME, 0L),
                snoozeScheduledStartTimeHour = snoozeScheduledStartTimeHour,
                snoozeScheduledStartTimeMinute = snoozeScheduledStartTimeMinute,
                snoozeScheduledEndTimeHour = snoozeScheduledEndTimeHour,
                snoozeScheduledEndTimeMinute = snoozeScheduledEndTimeMinute,
                snoozeScheduledRepeatingDays = snoozeScheduledRepeatingDays
            )
        }

        @Deprecated("Use suspend function", ReplaceWith("load(userId)"))
        fun loadBlocking(userPreferences: SharedPreferences): SnoozeSettings =
            runBlocking { load(userPreferences) }

        @Deprecated(
            "Load using Preferences directly",
            ReplaceWith("load(preferences)"),
            DeprecationLevel.ERROR
        )
        fun load(username: String): SnoozeSettings {
            unsupported
        }

        /**
         * Migration for snooze setting from different file from SharedPreferences.
         */
        private fun copySnoozeEntriesTo(
            sharedPreferences: SharedPreferences,
            defaultStartTimeString: String,
            defaultEndTimeString: String
        ) {
            val pref = ProtonMailApplication.getApplication()
                .getSharedPreferences(Constants.PrefsType.BACKUP_PREFS_NAME, Context.MODE_PRIVATE)

            if (PREF_SNOOZE_SCHEDULED_START_TIME in pref) {
                sharedPreferences[PREF_SNOOZE_SCHEDULED_START_TIME] =
                    pref.getString(PREF_SNOOZE_SCHEDULED_START_TIME, defaultStartTimeString)
                pref -= PREF_SNOOZE_SCHEDULED_START_TIME
            }

            if (PREF_SNOOZE_SCHEDULED_END_TIME in pref) {
                sharedPreferences[PREF_SNOOZE_SCHEDULED_END_TIME] =
                    pref.getString(PREF_SNOOZE_SCHEDULED_END_TIME, defaultEndTimeString)
                pref -= PREF_SNOOZE_SCHEDULED_END_TIME
            }

            if (PREF_SNOOZE_SCHEDULED_REPEAT_DAYS in pref) {
                sharedPreferences[PREF_SNOOZE_SCHEDULED_REPEAT_DAYS] =
                    pref.getString(PREF_SNOOZE_SCHEDULED_REPEAT_DAYS, null)
                pref -= PREF_SNOOZE_SCHEDULED_REPEAT_DAYS
            }

            if (PREF_SNOOZE_SCHEDULED in pref) {
                sharedPreferences[PREF_SNOOZE_SCHEDULED] =
                    pref.getBoolean(PREF_SNOOZE_SCHEDULED, false)
                pref -= PREF_SNOOZE_SCHEDULED
            }

            if (PREF_SNOOZE_QUICK in pref) {
                sharedPreferences[PREF_SNOOZE_QUICK] =
                    pref.getBoolean(PREF_SNOOZE_QUICK, false)
                pref -= PREF_SNOOZE_QUICK
            }

            if (PREF_SNOOZE_QUICK_END_TIME in pref) {
                sharedPreferences[PREF_SNOOZE_QUICK_END_TIME] =
                    pref.getLong(PREF_SNOOZE_QUICK_END_TIME, 0L)
                pref -= PREF_SNOOZE_QUICK_END_TIME
            }
        }
    }

    @Suppress("RedundantSuspendModifier") // Can't inject dispatcher for use `withContext`,
    //                                                  but still better than a blocking call
    suspend fun save(userPreferences: SharedPreferences) {
        saveScheduledBackup(userPreferences)
        saveScheduledSnoozeStartTimeBackup(userPreferences)
        saveScheduledSnoozeEndTimeBackup(userPreferences)
        saveScheduledSnoozeRepeatingDaysBackup(userPreferences)
    }

    @Deprecated(
        "Save using Preferences directly",
        ReplaceWith("save(preferences)"),
        DeprecationLevel.ERROR
    )
    fun save(username: String) {
        unsupported
    }

    fun getScheduledSnooze(userPreferences: SharedPreferences): Boolean =
        userPreferences.getBoolean(PREF_SNOOZE_SCHEDULED, false)

    @Deprecated(
        "Get using Preferences directly",
        ReplaceWith("getScheduledSnooze(preferences)"),
        DeprecationLevel.ERROR
    )
    fun getScheduledSnooze(username: String): Boolean {
        unsupported
    }

    private fun saveScheduledBackup(userPreferences: SharedPreferences) {
        userPreferences[PREF_SNOOZE_SCHEDULED] = snoozeScheduled
    }

    @Deprecated(
        "Save using Preferences directly",
        ReplaceWith("saveScheduledBackup(preferences)"),
        DeprecationLevel.ERROR
    )
    private fun saveScheduledBackup(username: String) {
        unsupported
    }

    fun saveQuickSnoozeBackup(userPreferences: SharedPreferences) {
        userPreferences[PREF_SNOOZE_QUICK] = snoozeQuick
    }

    @Deprecated(
        "Save using Preferences directly",
        ReplaceWith("saveQuickSnoozeBackup(preferences)"),
        DeprecationLevel.ERROR
    )
    fun saveQuickSnoozeBackup(username: String) {
        unsupported
    }

    fun saveQuickSnoozeEndTimeBackup(userPreferences: SharedPreferences) {
        userPreferences[PREF_SNOOZE_QUICK_END_TIME] = snoozeQuickEndTime
    }

    @Deprecated(
        "Save using Preferences directly",
        ReplaceWith("saveQuickSnoozeEndTimeBackup(preferences)"),
        DeprecationLevel.ERROR
    )
    fun saveQuickSnoozeEndTimeBackup(username: String) {
        unsupported
    }

    private fun saveScheduledSnoozeStartTimeBackup(userPreferences: SharedPreferences) {
        userPreferences[PREF_SNOOZE_SCHEDULED_START_TIME] =
            "$snoozeScheduledStartTimeHour:$snoozeScheduledStartTimeMinute"
    }

    @Deprecated(
        "Save using Preferences directly",
        ReplaceWith("saveScheduledSnoozeStartTimeBackup(preferences)"),
        DeprecationLevel.ERROR
    )
    private fun saveScheduledSnoozeStartTimeBackup(username: String) {
        unsupported
    }

    private fun saveScheduledSnoozeEndTimeBackup(userPreferences: SharedPreferences) {
        userPreferences[PREF_SNOOZE_SCHEDULED_END_TIME] = "$snoozeScheduledEndTimeHour:$snoozeScheduledEndTimeMinute"
    }

    @Deprecated(
        "Save using Preferences directly",
        ReplaceWith("saveScheduledSnoozeEndTimeBackup(preferences)"),
        DeprecationLevel.ERROR
    )
    private fun saveScheduledSnoozeEndTimeBackup(username: String) {
        unsupported
    }

    private fun saveScheduledSnoozeRepeatingDaysBackup(userPreferences: SharedPreferences) {
        userPreferences[PREF_SNOOZE_SCHEDULED_REPEAT_DAYS] = snoozeScheduledRepeatingDays
    }

    @Deprecated(
        "Save using Preferences directly",
        ReplaceWith("saveScheduledSnoozeRepeatingDaysBackup(preferences)"),
        DeprecationLevel.ERROR
    )
    private fun saveScheduledSnoozeRepeatingDaysBackup(username: String) {
        unsupported
    }

    /**
     * Determines if user's scheduled snooze settings should cause notifications to be suppressed.
     */
    fun shouldSuppressNotification(time: Calendar): Boolean {

        val days = mapOf<Int, CharSequence>(
            Calendar.MONDAY to "mo",
            Calendar.TUESDAY to "tu",
            Calendar.WEDNESDAY to "we",
            Calendar.THURSDAY to "th",
            Calendar.FRIDAY to "fr",
            Calendar.SATURDAY to "sa",
            Calendar.SUNDAY to "su"
        )

        val currentTimestamp = time.get(Calendar.HOUR_OF_DAY) * 60 + time.get(Calendar.MINUTE)
        val startTimestamp = snoozeScheduledStartTimeHour * 60 + snoozeScheduledStartTimeMinute
        val endTimestamp = snoozeScheduledEndTimeHour * 60 + snoozeScheduledEndTimeMinute

        var isWithinSnoozeWindow = false
        var isOnCorrectDay = false

        if (startTimestamp < endTimestamp) { // snooze happens during the day
            isWithinSnoozeWindow = currentTimestamp in startTimestamp until endTimestamp
            isOnCorrectDay = snoozeScheduledRepeatingDays
                ?.contains(days[time.get(Calendar.DAY_OF_WEEK)] ?: "xx") ?: false

        } else { // snooze happens over the midnight
            if (currentTimestamp >= startTimestamp) {
                isWithinSnoozeWindow = true
                isOnCorrectDay = snoozeScheduledRepeatingDays
                    ?.contains(days[time.get(Calendar.DAY_OF_WEEK)] ?: "xx") ?: false

            } else if (currentTimestamp < endTimestamp) {
                isWithinSnoozeWindow = true
                time.add(Calendar.DAY_OF_WEEK, -1)
                isOnCorrectDay = snoozeScheduledRepeatingDays
                    ?.contains(days[time.get(Calendar.DAY_OF_WEEK)] ?: "xx") ?: false
            }
        }

        return snoozeScheduled && isWithinSnoozeWindow && isOnCorrectDay
    }
}
