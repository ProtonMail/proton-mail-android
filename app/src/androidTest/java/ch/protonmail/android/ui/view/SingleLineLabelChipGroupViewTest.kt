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

import android.graphics.Color
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.R
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.testAndroidInstrumented.assertion.isGone
import ch.protonmail.android.testAndroidInstrumented.assertion.isVisible
import ch.protonmail.android.testAndroidInstrumented.withBackgroundColor
import ch.protonmail.android.util.ViewTest
import org.junit.runner.RunWith
import kotlin.test.Test

/**
 * Test suite for [SingleLineLabelChipGroupView]
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class SingleLineLabelChipGroupViewTest : ViewTest<SingleLineLabelChipGroupView>(::SingleLineLabelChipGroupView) {

    private val testLabelsList = listOf(
        LabelChipUiModel(Id("a"), Name("first"), Color.RED),
        LabelChipUiModel(Id("b"), Name("second"), Color.GREEN),
        LabelChipUiModel(Id("c"), Name("third"), Color.BLUE),
    )

    @Test
    fun labelsAndMoreAreGoneWhenLabelsListIsEmpty() {
        val chipGroupView = testView

        // given
        val labels = emptyList<LabelChipUiModel>()

        // when
        chipGroupView.setLabels(labels)

        // then
        onLabelView().check(isGone())
        onMoreView().check(isGone())
    }

    @Test
    fun labelIsShownButMoreIsGoneWhenLabelsListHasOneElement() {
        val chipGroupView = testView

        // given
        val labels = listOf(testLabelsList.first())

        // when
        chipGroupView.setLabels(labels)

        // then
        onLabelView().check(isVisible())
        onMoreView().check(isGone())
    }

    @Test
    fun labelsAndMoreAreVisibleWhenLabelsListHasMoreThanOneElement() {
        val chipGroupView = testView

        // given
        val labels = testLabelsList

        // when
        chipGroupView.setLabels(labels)

        // then
        onLabelView().check(isVisible())
        onMoreView().check(isVisible())
    }

    @Test
    fun labelShowsTheCorrectNameAndColor() {
        val chipGroupView = testView

        // given
        val labels = testLabelsList

        // when
        chipGroupView.setLabels(labels)

        // then
        val (expectedLabelName, expectedLabelColor) = with(labels.first()) {
            name.s to checkNotNull(color)
        }
        onLabelView()
            .check(matches(withText(expectedLabelName)))
            .check(matches(withBackgroundColor(expectedLabelColor)))
    }

    @Test
    fun moreShowsTheCorrectNumber() {
        val chipGroupView = testView

        // given
        val labels = testLabelsList

        // when
        chipGroupView.setLabels(labels)

        // then
        onMoreView().check(matches(withText("+${labels.size - 1}")))
    }

    private fun onLabelView(): ViewInteraction =
        onView(withId(R.id.single_line_label_chip_group_label))

    private fun onMoreView(): ViewInteraction =
        onView(withId(R.id.single_line_label_chip_group_more))
}
