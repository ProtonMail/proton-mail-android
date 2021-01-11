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

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SnoozeSettingsTest {

    private val snoozeSettingsDuringDay = SnoozeSettings(
        snoozeScheduled = true,
        snoozeScheduledStartTimeHour = 14,
        snoozeScheduledStartTimeMinute = 15,
        snoozeScheduledEndTimeHour = 20,
        snoozeScheduledEndTimeMinute = 30,
        snoozeScheduledRepeatingDays = "mo"
    )

    private val snoozeSettingsOverMidnight = SnoozeSettings(
        snoozeScheduled = true,
        snoozeScheduledStartTimeHour = 23,
        snoozeScheduledStartTimeMinute = 0,
        snoozeScheduledEndTimeHour = 1,
        snoozeScheduledEndTimeMinute = 20,
        snoozeScheduledRepeatingDays = "sa"
    )

    private val snoozeSettingsEveryDay = SnoozeSettings(
        snoozeScheduled = true,
        snoozeScheduledStartTimeHour = 14,
        snoozeScheduledStartTimeMinute = 15,
        snoozeScheduledEndTimeHour = 20,
        snoozeScheduledEndTimeMinute = 30,
        snoozeScheduledRepeatingDays = "mo:tu:we:th:fr:sa:su"
    )

    @Test
    fun `show notification before snooze window, during the day`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 5)
        }
        assertFalse(snoozeSettingsDuringDay.shouldSuppressNotification(timestamp))
    }

    @Test
    fun `hide notification during snooze window, during the day`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 20)
        }
        assertTrue(snoozeSettingsDuringDay.shouldSuppressNotification(timestamp))
    }

    @Test
    fun `show notification after snooze window, during the day`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 35)
        }
        assertFalse(snoozeSettingsDuringDay.shouldSuppressNotification(timestamp))
    }

    @Test
    fun `show notification during snooze window, on another day`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 20)
        }
        assertFalse(snoozeSettingsDuringDay.shouldSuppressNotification(timestamp))
    }

    @Test
    fun `show notification before snooze window, over midnight`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 35)
        }
        assertFalse(snoozeSettingsOverMidnight.shouldSuppressNotification(timestamp))
    }

    @Test
    fun `hide notification during snooze window, before midnight`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 35)
        }
        assertTrue(snoozeSettingsOverMidnight.shouldSuppressNotification(timestamp))
    }

    @Test
    fun `hide notification during snooze window, after midnight`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 35)
        }
        assertTrue(snoozeSettingsOverMidnight.shouldSuppressNotification(timestamp))
    }

    @Test
    fun `show notification after snooze window, over midnight`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 35)
        }
        assertFalse(snoozeSettingsOverMidnight.shouldSuppressNotification(timestamp))
    }

    @Test
    fun `show notification during snooze window, before midnight, on another day`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 35)
        }
        assertFalse(snoozeSettingsOverMidnight.shouldSuppressNotification(timestamp))
    }

    @Test
    fun `show notification during snooze window, after midnight, on another day`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 35)
        }
        assertFalse(snoozeSettingsOverMidnight.shouldSuppressNotification(timestamp))
    }

    @Test
    fun `hide notification during snooze window, every day`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 20)
        }

        for (i in 1..7) {
            assertTrue(snoozeSettingsEveryDay.shouldSuppressNotification(timestamp))
            timestamp.add(Calendar.DAY_OF_WEEK, 1)
        }
    }

    @Test
    fun `show notification outside of snooze window, every day`() {
        val timestamp = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 20)
        }

        for (i in 1..7) {
            assertFalse(snoozeSettingsEveryDay.shouldSuppressNotification(timestamp))
            timestamp.add(Calendar.DAY_OF_WEEK, 1)
        }
    }

}
