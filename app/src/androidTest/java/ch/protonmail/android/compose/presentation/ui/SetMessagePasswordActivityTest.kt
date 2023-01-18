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

package ch.protonmail.android.compose.presentation.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.R
import ch.protonmail.android.util.withProtonInputEditTextId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.not
import org.hamcrest.core.AllOf.allOf
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@HiltAndroidTest
@RunWith(AndroidJUnit4ClassRunner::class)
@Suppress("SameParameterValue")
class SetMessagePasswordActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val baseIntent =
        Intent(ApplicationProvider.getApplicationContext(), SetMessagePasswordActivity::class.java)

    @BeforeTest
    fun setup() {
        Intents.init()
    }

    @Test
    fun inputIsSetCorrectly() {

        // given
        val expectedPassword = "12345"
        val expectedHint = "hint"

        // when
        val intent = baseIntent
            .putExtra(ARG_SET_MESSAGE_PASSWORD_PASSWORD, expectedPassword)
            .putExtra(ARG_SET_MESSAGE_PASSWORD_HINT, expectedHint)
        ActivityScenario.launch<SetMessagePasswordActivity>(intent)

        // then
        onPasswordView().check(matches(withText(expectedPassword)))
        onRepeatView().check(matches(withText(expectedPassword)))
        onHintView().check(matches(withText(expectedHint)))
    }

    @Test
    fun resultIsSetCorrectly() {

        // given
        val expectedPassword = "12345"
        val expectedHint = "hint"

        // when
        val scenario = ActivityScenario.launchActivityForResult<SetMessagePasswordActivity>(baseIntent)
        setPassword(expectedPassword)
        setHint(expectedHint)
        performApplyClick()

        // then
        assertResult(scenario, expectedPassword, expectedHint)
    }

    @Test
    fun passwordIsRemovedCorrectly() {

        // given
        val expectedPassword: String? = null
        val expectedHint: String? = null

        // when
        val intent = baseIntent
            .putExtra(ARG_SET_MESSAGE_PASSWORD_PASSWORD, "old password")
            .putExtra(ARG_SET_MESSAGE_PASSWORD_HINT, "old hint")
        val scenario = ActivityScenario.launchActivityForResult<SetMessagePasswordActivity>(intent)
        performRemovePasswordClick()

        // then
        assertResult(scenario, expectedPassword, expectedHint)
    }

    private fun onPasswordView(): ViewInteraction =
        onView(withProtonInputEditTextId(R.id.set_msg_password_msg_password_input))

    private fun onRepeatView(): ViewInteraction =
        onView(withProtonInputEditTextId(R.id.set_msg_password_repeat_password_input))

    private fun onHintView(): ViewInteraction =
        onView(withProtonInputEditTextId(R.id.set_msg_password_hint_input))

    private fun setPassword(password: String) {
        onPasswordView().perform(replaceText(password))
        onRepeatView().perform(replaceText(password))
    }

    private fun setHint(hint: String) {
        onHintView().perform(replaceText(hint))
    }

    private fun performApplyClick() {
        onView(withId(R.id.set_msg_password_apply_button)).perform(click())
    }

    private fun performRemovePasswordClick() {
        onView(withId(R.id.set_msg_password_remove_button))
            .check(matches(isEnabled()))
            .perform(click())
    }

    private fun assertResult(
        scenario: ActivityScenario<SetMessagePasswordActivity>,
        expectedPassword: String?,
        expectedHint: String?
    ) {
        val resultIntent = scenario.result.resultData

        val withExtrasMatcher = allOf(
            hasExtra(ARG_SET_MESSAGE_PASSWORD_PASSWORD, expectedPassword),
            hasExtra(ARG_SET_MESSAGE_PASSWORD_HINT, expectedHint)
        )

        val matcher = if (expectedPassword == null && expectedHint == null) {
            anyOf(
                allOf(
                    not(hasExtraWithKey(ARG_SET_MESSAGE_PASSWORD_PASSWORD)),
                    not(hasExtraWithKey(ARG_SET_MESSAGE_PASSWORD_HINT))
                ),
                withExtrasMatcher
            )
        } else {
            withExtrasMatcher
        }

        ViewMatchers.assertThat(resultIntent, matcher)
    }
}
