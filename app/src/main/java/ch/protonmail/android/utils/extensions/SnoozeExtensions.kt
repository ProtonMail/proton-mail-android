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
package ch.protonmail.android.utils.extensions

import android.content.Context
import android.text.TextUtils
import ch.protonmail.android.R
import ch.protonmail.android.settings.presentation.SnoozeRepeatDayView
import me.proton.core.util.kotlin.EMPTY_STRING

/**
 * Do the true values represent days of the weekend only (0 position is representing Monday)
 */
fun Array<Boolean>.isWeekend(): Boolean {
    return countTrue() == 2 && this[5] && this[6]
}

/**
 * Do the true values represent working days only (0 position in the array is Monday)
 */
fun Array<Boolean>.isWorkDays(): Boolean {
    return countTrue() == 5 && !this[5] && !this[6]
}

/**
 * Calculates how many true values are there in the Boolean array.
 * @return number of true values in the Boolean array
 */
fun Array<Boolean>.countTrue(): Int {
    return this.filter {
        it
    }.count()
}

fun Array<Boolean>.buildUILabel(context: Context, daysSelected: String): String {
    return when {
        countTrue() == 0 -> EMPTY_STRING
        countTrue() == 7 -> context.getString(R.string.every_day)
        isWorkDays() -> context.getString(R.string.work_days)
        isWeekend() -> context.getString(R.string.weekends)
        else -> daysSelected
    }
}

fun List<SnoozeRepeatDayView>.countSelected(): Int {
    return this.filter { it.isSelected }.count()
}

fun List<SnoozeRepeatDayView>.selectAll() {
    this.forEach {
        it.isSelected = true
    }
}

fun MutableList<String>?.buildRepeatingDaysString(delimiter: String): String {
    var result = EMPTY_STRING
    val delimiterAdjusted = if (TextUtils.isEmpty(delimiter)) " " else delimiter
    this?.let {
        for (day in this) {
            result = result + day + delimiterAdjusted
        }
    }
    return if (result.isNotEmpty()) result.substring(0, result.length - delimiterAdjusted.length) else result
}

fun Int.roundHourOrMinute(): String {
    return if (this < 10) "0$this" else this.toString()
}
