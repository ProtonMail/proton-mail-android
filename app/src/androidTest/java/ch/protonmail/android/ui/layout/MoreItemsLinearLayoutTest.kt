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

package ch.protonmail.android.ui.layout

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.R
import ch.protonmail.android.testAndroidInstrumented.assertion.isGone
import ch.protonmail.android.util.ViewTest
import me.proton.core.test.kotlin.assertIs
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import kotlin.test.Test

/**
 * Test suite for [MoreItemsLinearLayout]
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class MoreItemsLinearLayoutTest : ViewTest<MoreItemsLinearLayout>(
    ::MoreItemsLinearLayout,
    width = VIEW_WIDTH
) {

    @Test
    fun viewsAreNeverAddedAfterMore() {

        // given - when
        testView.apply {
            addView(ImageView(context))
            addView(ImageView(context), -1)
            addView(ImageView(context), 1)
            addView(ImageView(context), 15)
        }

        // then
        repeat(4) { index ->
            assertIs<ImageView>(testView.getChildAt(index))
        }
    }

    @Test
    fun viewsAreInsertedAtTheRightIndex() {

        // given
        testView.addView(TextView(context))

        // when
        testView.addView(ImageView(context), 1)

        // then
        assertIs<ImageView>(testView.getChildAt(1))
    }

    @Test
    fun addOnlyTheViewsThatCanFit() {

        // given
        val childParams = LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.MATCH_PARENT)

        // when
        testView.apply {
            orientation = LinearLayout.HORIZONTAL
            repeat(5) {
                addView(View(context), childParams)
            }
        }

        // then
        awaitCompletion()
        assertEquals(5, testView.allChildCount)
        assertEquals(2, testView.visibleChildCount)
        assertEquals(3, testView.hiddenChildCount)
    }

    @Test
    fun moreShowTheRightText() {

        // given
        val childParams = LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.MATCH_PARENT)

        // when
        testView.apply {
            orientation = LinearLayout.HORIZONTAL
            repeat(5) {
                addView(View(context), childParams)
            }
        }

        // then
        onMoreView().check(matches(withText("+3")))
    }

    @Test
    fun moreIsHiddenIfAllTheViewsCanFit() {

        // given
        val childParams = LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.MATCH_PARENT)

        // when
        testView.apply {
            orientation = LinearLayout.HORIZONTAL
            repeat(2) {
                addView(View(context), childParams)
            }
        }

        // then
        onMoreView().check(isGone())
    }

    private fun onMoreView(): ViewInteraction =
        onView(withId(R.id.more_items_ll_more_text_view))

    private fun awaitCompletion() {
        onTestView()
    }

    private companion object {

        const val VIEW_WIDTH = 1000
    }
}
