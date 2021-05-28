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

package ch.protonmail.android.compose.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.compose.presentation.ui.SetMessageExpirationActivity.Expiration.Custom
import ch.protonmail.android.compose.presentation.ui.SetMessageExpirationActivity.Expiration.None
import ch.protonmail.android.compose.presentation.ui.SetMessageExpirationActivity.Expiration.OneDay
import ch.protonmail.android.compose.presentation.ui.SetMessageExpirationActivity.Expiration.OneHour
import ch.protonmail.android.compose.presentation.ui.SetMessageExpirationActivity.Expiration.OneWeek
import ch.protonmail.android.compose.presentation.ui.SetMessageExpirationActivity.Expiration.ThreeDays
import ch.protonmail.android.databinding.ActivitySetMessageExpirationBinding
import ch.protonmail.android.ui.view.DaysHoursPair
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.presentation.utils.onClick
import timber.log.Timber

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val ARG_SET_MESSAGE_EXPIRATION_DAYS = "arg.days"
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val ARG_SET_MESSAGE_EXPIRATION_HOURS = "arg.hours"

/**
 * Activity for set expiration for a given Message
 * @see ComposeMessageKotlinActivity
 */
class SetMessageExpirationActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivitySetMessageExpirationBinding.inflate(layoutInflater)
    }
    private val noneEntry: Pair<TextView, ImageView> get() =
        binding.setMsgExpirationNoneTextView to binding.setMsgExpirationNoneCheck
    private val oneHourEntry: Pair<TextView, ImageView> get() =
        binding.setMsgExpiration1HourTextView to binding.setMsgExpiration1HourCheck
    private val oneDayEntry: Pair<TextView, ImageView> get() =
        binding.setMsgExpiration1DayTextView to binding.setMsgExpiration1DayCheck
    private val threeDaysEntry: Pair<TextView, ImageView> get() =
        binding.setMsgExpiration3DaysTextView to binding.setMsgExpiration3DaysCheck
    private val oneWeekEntry: Pair<TextView, ImageView> get() =
        binding.setMsgExpiration1WeekTextView to binding.setMsgExpiration1WeekCheck
    private val customEntry: Pair<TextView, ImageView> get() =
        binding.setMsgExpirationCustomTextView to binding.setMsgExpirationCustomCheck

    private lateinit var expiration: Expiration

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.setMsgExpirationToolbar)

        val days = intent.getIntExtra(ARG_SET_MESSAGE_EXPIRATION_DAYS, 0)
        val hours = intent.getIntExtra(ARG_SET_MESSAGE_EXPIRATION_HOURS, 0)

        setExpiration(parseExpiration(days, hours))

        noneEntry.first.onClick { setExpiration(None) }
        oneHourEntry.first.onClick { setExpiration(OneHour) }
        oneDayEntry.first.onClick { setExpiration(OneDay) }
        threeDaysEntry.first.onClick { setExpiration(ThreeDays) }
        oneWeekEntry.first.onClick { setExpiration(OneWeek) }
        customEntry.first.onClick { setExpiration(Custom(0, 0)) }
        binding.setMsgExpirationPickerView.onChange
            .onEach { setExpiration(Custom(it.days, it.hours)) }
            .launchIn(lifecycleScope)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_set_message_expiration, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.set_msg_expiration_set -> setResultAndFinish()
            android.R.id.home -> cancelResultAndFinish()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setResultAndFinish(): Boolean {
        Timber.v("Set expiration result - days: ${expiration.days}, hours: ${expiration.hours}")
        val resultIntent = Intent()
            .putExtra(ARG_SET_MESSAGE_EXPIRATION_DAYS, expiration.days)
            .putExtra(ARG_SET_MESSAGE_EXPIRATION_HOURS, expiration.hours)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
        return true
    }

    private fun cancelResultAndFinish(): Boolean {
        finish()
        return true
    }

    private fun setExpiration(expiration: Expiration) {
        this.expiration = expiration
        listOf(
            noneEntry,
            oneHourEntry,
            oneDayEntry,
            threeDaysEntry,
            oneWeekEntry,
            customEntry,
        ).forEach(::setUnchecked)
        binding.setMsgExpirationPickerView.isVisible = false

        when (expiration) {
            None -> setChecked(noneEntry)
            OneHour -> setChecked(oneHourEntry)
            OneDay -> setChecked(oneDayEntry)
            ThreeDays -> setChecked(threeDaysEntry)
            OneWeek -> setChecked(oneWeekEntry)
            is Custom -> {
                setChecked(customEntry)
                binding.setMsgExpirationPickerView.apply {
                    isVisible = true
                    set(expiration.days, expiration.hours)
                }
            }
        }
    }

    private fun setChecked(entry: Pair<TextView, ImageView>) {
        entry.second.isVisible = true
    }

    private fun setUnchecked(entry: Pair<TextView, ImageView>) {
        entry.second.isVisible = false
    }

    private fun parseExpiration(days: Int, hours: Int): Expiration {
        return when {
            days == None.days && hours == None.hours -> None
            days == OneHour.days && hours == OneHour.hours -> OneHour
            days == OneDay.days && hours == OneDay.hours -> OneDay
            days == ThreeDays.days && hours == ThreeDays.hours -> ThreeDays
            days == OneWeek.days && hours == OneWeek.hours -> OneWeek
            else -> Custom(days, hours)
        }
    }

    @SuppressWarnings("MagicNumber") // Constant not needed for sealed class
    private sealed class Expiration(open val days: Int, open val hours: Int) {

        object None : Expiration(0, 0)
        object OneHour : Expiration(0, 1)
        object OneDay : Expiration(1, 0)
        object ThreeDays : Expiration(3, 0)
        object OneWeek : Expiration(7, 0)
        data class Custom(override val days: Int, override val hours: Int) : Expiration(days, hours)
    }

    class ResultContract : ActivityResultContract<DaysHoursPair, DaysHoursPair>() {

        private lateinit var input: DaysHoursPair

        override fun createIntent(context: Context, input: DaysHoursPair): Intent {
            this.input = input
            return Intent(context, SetMessageExpirationActivity::class.java)
                .putExtra(ARG_SET_MESSAGE_EXPIRATION_DAYS, input.days)
                .putExtra(ARG_SET_MESSAGE_EXPIRATION_HOURS, input.hours)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): DaysHoursPair =
            intent?.let {
                val days = intent.getIntExtra(ARG_SET_MESSAGE_EXPIRATION_DAYS, 0)
                val hours = intent.getIntExtra(ARG_SET_MESSAGE_EXPIRATION_HOURS, 0)
                DaysHoursPair(days, hours)
            } ?: input
    }
}
