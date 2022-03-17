/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.security.domain.usecase

import ch.protonmail.android.core.Constants.Prefs.PREF_PREVENT_TAKING_SCREENSHOTS
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.mocks.newMockSharedPreferences
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.android.sharedpreferences.get
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class SavePreventTakingScreenshotsTest(
    @Suppress("unused") private val testName: String,
    private val input: Input,
    private val output: Output
) {

    private val sharedPreferences = newMockSharedPreferences
    private val save = SavePreventTakingScreenshots(
        preferences = sharedPreferences,
        dispatchers = TestDispatcherProvider
    )

    @Test
    fun suspend() = runBlockingTest {
        save(input.shouldPrevent)
        assertEquals(output.expected, sharedPreferences[PREF_PREVENT_TAKING_SCREENSHOTS])
    }

    @Test
    fun blocking() {
        save.blocking(input.shouldPrevent)
        assertEquals(output.expected, sharedPreferences[PREF_PREVENT_TAKING_SCREENSHOTS])
    }

    data class Input(val shouldPrevent: Boolean)
    data class Output(val expected: Int)
    data class Parameters(
        val testName: String,
        val input: Input,
        val output: Output
    )

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(

            Parameters(
                testName = "correctly saves prevent",
                Input(shouldPrevent = true),
                Output(expected = 1)
            ),

            Parameters(
                testName = "correctly saves not prevent",
                Input(shouldPrevent = false),
                Output(expected = 0)
            ),
        ).map { arrayOf(it.testName, it.input, it.output) }
    }
}
