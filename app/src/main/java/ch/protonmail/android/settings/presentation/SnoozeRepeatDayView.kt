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

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import ch.protonmail.android.R
import me.proton.core.util.kotlin.EMPTY_STRING

class SnoozeRepeatDayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var isSelected = false
    var code: String = EMPTY_STRING

    init {

        val snoozeRepeatNames = listOf(*resources.getStringArray(R.array.snooze_repeat_values))
        val snoozeRepeatCodes = listOf(*resources.getStringArray(R.array.repeating_snooze_days))

        when (id) {
            R.id.monday -> {
                text = snoozeRepeatNames[Day.MONDAY.ordinal].toString()
                code = snoozeRepeatCodes[Day.MONDAY.ordinal].toString()
            }
            R.id.tuesday -> {
                text = snoozeRepeatNames[Day.TUESDAY.ordinal].toString()
                code = snoozeRepeatCodes[Day.TUESDAY.ordinal].toString()
            }
            R.id.wednesday -> {
                text = snoozeRepeatNames[Day.WEDNESDAY.ordinal].toString()
                code = snoozeRepeatCodes[Day.WEDNESDAY.ordinal].toString()
            }
            R.id.thursday -> {
                text = snoozeRepeatNames[Day.THURSDAY.ordinal].toString()
                code = snoozeRepeatCodes[Day.THURSDAY.ordinal].toString()
            }
            R.id.friday -> {
                text = snoozeRepeatNames[Day.FRIDAY.ordinal].toString()
                code = snoozeRepeatCodes[Day.FRIDAY.ordinal].toString()
            }
            R.id.saturday -> {
                text = snoozeRepeatNames[Day.SATURDAY.ordinal].toString()
                code = snoozeRepeatCodes[Day.SATURDAY.ordinal].toString()
            }
            R.id.sunday -> {
                text = snoozeRepeatNames[Day.SUNDAY.ordinal].toString()
                code = snoozeRepeatCodes[Day.SUNDAY.ordinal].toString()
            }
        }
        background = ContextCompat.getDrawable(context, R.drawable.repeat_day_unselected_background)
    }

    override fun isSelected(): Boolean {
        return isSelected
    }

    fun setSelected(selectedValues: List<String>) {
        for (value in selectedValues) {
            if (value == this.code) {
                setSelected(true)
            }
        }
    }

    override fun setSelected(selected: Boolean) {
        isSelected = selected
        background = if (isSelected) {
            ContextCompat.getDrawable(context, R.drawable.repeat_day_selected_background)
        } else {
            ContextCompat.getDrawable(context, R.drawable.repeat_day_unselected_background)
        }
    }

    enum class Day {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY
    }
}
