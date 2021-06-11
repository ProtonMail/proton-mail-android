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

package ch.protonmail.android.ui.view

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import ch.protonmail.android.databinding.ViewDaysAndHoursPickerBinding
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import me.proton.core.presentation.ui.view.ProtonInput
import me.proton.core.presentation.utils.onTextChange
import kotlin.time.milliseconds

/**
 * Picker for Hours and Days
 */
@OptIn(FlowPreview::class)
class DaysAndHoursPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val daysInput: ProtonInput
    private val hoursInput: ProtonInput

    private val changesBuffer = Channel<DaysHoursPair>(Channel.BUFFERED)

    val onChange: Flow<DaysHoursPair> =
        changesBuffer.receiveAsFlow()
            .distinctUntilChanged()
            .debounce(50.milliseconds)

    init {
        val binding = ViewDaysAndHoursPickerBinding.inflate(
            LayoutInflater.from(context),
            this
        )

        daysInput = binding.daysAndHoursPickerDaysInput
        hoursInput = binding.daysAndHoursPickerHoursInput

        daysInput.onTextChange { text ->
            val hours = hoursInput.text?.toString()?.toIntOrNull()
            if (normaliseDays(text) == HasChanged.False && hours != null)
                changesBuffer.offer(DaysHoursPair(text.toString().toInt(), hours))
        }
        hoursInput.onTextChange { text ->
            val days = daysInput.text?.toString()?.toIntOrNull()
            if (normaliseHours(text) == HasChanged.False && days != null)
                changesBuffer.offer(DaysHoursPair(days, text.toString().toInt()))
        }
    }

    fun set(days: Int, hours: Int) {
        daysInput.text = days.toString()
        hoursInput.text = hours.toString()
    }

    private fun normaliseDays(input: Editable): HasChanged {
        val inputInt = input.toString().toIntOrNull()
        return when {
            inputInt == null || inputInt < 0 -> {
                daysInput.text = 0.toString()
                HasChanged.True
            }
            inputInt > MAX_DAYS -> {
                daysInput.text = MAX_DAYS.toString()
                HasChanged.True
            }
            else -> {
                HasChanged.False
            }
        }
    }

    private fun normaliseHours(input: Editable): HasChanged {
        val inputInt = input.toString().toIntOrNull()
        return when {
            inputInt == null || inputInt < 0 -> {
                hoursInput.text = 0.toString()
                HasChanged.True
            }
            inputInt > MAX_HOURS -> {
                hoursInput.text = MAX_HOURS.toString()
                HasChanged.True
            }
            else -> {
                HasChanged.False
            }
        }
    }

    private enum class HasChanged {
        True, False
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    companion object {

        const val MAX_DAYS = 28
        const val MAX_HOURS = 23
    }
}

data class DaysHoursPair(
    val days: Int,
    val hours: Int
)
