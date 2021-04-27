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
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.testAndroidInstrumented.withBackgroundColor
import ch.protonmail.android.util.ViewTest
import org.junit.runner.RunWith
import kotlin.test.Test

/**
 * Test suite for [MultiLineLabelChipGroupView]
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class MultiLineLabelChipGroupViewTest : ViewTest<MultiLineLabelChipGroupView>(::MultiLineLabelChipGroupView) {

    private val testLabelsList = listOf(
        LabelChipUiModel(Id("a"), Name("long name for first label"), Color.RED),
        LabelChipUiModel(Id("b"), Name("second label"), Color.GREEN),
        LabelChipUiModel(Id("c"), Name("third"), Color.BLUE),
        LabelChipUiModel(Id("d"), Name("long name for forth label"), Color.CYAN),
        LabelChipUiModel(Id("e"), Name("fifth label"), Color.MAGENTA),
        LabelChipUiModel(Id("f"), Name("sixth"), Color.GRAY),
        LabelChipUiModel(Id("g"), Name("long name for seventh label"), Color.BLACK),
    )

    @Test
    fun listIsEmptyWhenLabelsListIsEmpty() {
        val chipGroupView = testView

        // given
        val labels = emptyList<LabelChipUiModel>()

        // when
        chipGroupView.setLabels(labels)

        // then
        onRecyclerView().check(matches(hasChildCount(labels.size)))
    }

    @Test
    fun listContainsAllTheLabels() {
        val chipGroupView = testView

        // given
        val labels = testLabelsList

        // when
        chipGroupView.setLabels(labels)

        // then
        onRecyclerView().check(matches(hasChildCount(labels.size)))
    }

    @Test
    fun correctLabelsNamesAndColorsAreDisplayed() {
        val chipGroupView = testView

        // given
        val labels = testLabelsList

        // when
        chipGroupView.setLabels(labels)

        // then
        for (label in labels) {
            onView(withText(label.name.s)).check(matches(withBackgroundColor(label.color)))
        }
    }

    private fun onRecyclerView(): ViewInteraction =
        onView(withId(MultiLineLabelChipGroupView.RECYCLER_VIEW_ID))
}
