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
package ch.protonmail.tokenautocomplete.example

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import ch.protonmail.tokenautocomplete.example.TokenMatchers.emailForPerson
import ch.protonmail.tokenautocomplete.example.TokenMatchers.tokenCount
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Rule
import kotlin.test.Test

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ContactsCompletionViewTest {

    @get:Rule val activityRule = ActivityTestRule(TestCleanTokenActivity::class.java)

    @Test
    fun completesOnComma() {
        onView(withId(R.id.searchView))
                .perform(typeText("mar,"))
                .check(matches(emailForPerson(0, `is`("marshall@example.com"))))
                .check(matches(tokenCount(`is`(1))))
    }

    @Test
    fun doesntCompleteWithoutComma() {
        onView(withId(R.id.searchView))
                .perform(typeText("mar"))
                .check(matches(tokenCount(`is`(0))))
    }

    @Test
    fun ignoresObjects() {
        val ignorable = Person.samplePeople()[0]
        val notIgnorable = Person.samplePeople()[1]
        val completionView = activityRule.activity.completionView
        completionView.setPersonToIgnore(ignorable)

        activityRule.activity.runOnUiThread {
            completionView.addObjectSync(notIgnorable)
            assertEquals(1, completionView.objects.size.toLong())
            completionView.addObjectSync(ignorable)
            assertEquals(1, completionView.objects.size.toLong())
        }

        onView(withId(R.id.searchView))
                .perform(typeText(ignorable.name + ","))
                .check(matches(tokenCount(`is`(1))))
        onView(withId(R.id.searchView))
                .perform(typeText("ter,"))
                .check(matches(tokenCount(`is`(2))))
    }

    @Test
    fun clearsAllObjects() {
        val completionView = activityRule.activity.completionView
        completionView.allowCollapse(true)

        val listener = TestTokenListener()
        completionView.setTokenListener(listener)

        activityRule.activity.runOnUiThread {
            for (person in Person.samplePeople()) {
                completionView.addObjectSync(person)
            }
            assertEquals(Person.samplePeople().size.toLong(), completionView.objects.size.toLong())
            assertEquals(Person.samplePeople().size.toLong(), listener.added.size.toLong())
            completionView.performCollapse(false)
            assertEquals(Person.samplePeople().size.toLong(), completionView.objects.size.toLong())
            assertEquals(Person.samplePeople().size.toLong(), listener.added.size.toLong())
        }

        onView(withId(R.id.searchView))
                //The +count text is included
                .check(matches(withText(containsString("+"))))
                .check(matches(tokenCount(`is`(Person.samplePeople().size))))
        completionView.clearAsync()
        onView(withId(R.id.searchView))
                .check(matches(tokenCount(`is`(0))))
                //The text should also reset completely
                .check(matches(withText(String.format("To: %s", completionView.hint))))

        activityRule.activity.runOnUiThread {
            assertEquals(Person.samplePeople().size.toLong(), listener.added.size.toLong())
            assertEquals(Person.samplePeople().size.toLong(), listener.removed.size.toLong())
            assertEquals(0, listener.ignored.size.toLong())
        }

        //Make sure going to 0 while collapsed, then adding an object doesn't hit a crash
        activityRule.activity.runOnUiThread {
            completionView.addObjectSync(Person.samplePeople()[0])
            assertEquals(1, completionView.objects.size.toLong())
            for (person in Person.samplePeople()) {
                completionView.addObjectSync(person)
            }
            assertEquals((Person.samplePeople().size + 1).toLong(), completionView.objects.size.toLong())
        }
        onView(withId(R.id.searchView))
                //The +count text is included
                .check(matches(withText(containsString("+"))))
    }

    @Test
    fun handlesHintOnInitialItemSelected() {
        val completionView = activityRule.activity.completionView

        val listener = TestTokenListener()
        completionView.setTokenListener(listener)

        onView(withId(R.id.searchView))
                .check(matches(tokenCount(`is`(0))))
                //The text should also reset completely
                .check(matches(withText(String.format("To: %s", completionView.hint))))
        activityRule.activity.runOnUiThread { completionView.simulateSelectingPersonFromList(Person.samplePeople()[0]) }

        onView(withId(R.id.searchView))
                .check(matches(tokenCount(`is`(1))))
                //The text should also reset completely
                .check(matches(withText(String.format("To: %s, ", Person.samplePeople()[0].toString()))))
    }

    @Test
    fun ellipsizesPreservingPrefix() {
        val completionView = activityRule.activity.completionView

        activityRule.activity.runOnUiThread {
            //6th token is as wide as the view
            completionView.addObjectSync(Person.samplePeople()[6])
            completionView.performCollapse(false)
        }

        onView(withId(R.id.searchView))
                .check(matches(tokenCount(`is`(1))))
                .check(matches(withText("To:  +1")))
    }


}
