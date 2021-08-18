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
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.R
import ch.protonmail.android.domain.entity.LabelId
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.testAndroidInstrumented.assertion.isGone
import ch.protonmail.android.testAndroidInstrumented.assertion.isVisible
import ch.protonmail.android.testAndroidInstrumented.matcher.withDrawable
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.util.ViewTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class SingleLineCollapsedLabelGroupViewTest :
    ViewTest<SingleLineCollapsedLabelGroupView>(::SingleLineCollapsedLabelGroupView) {

    @Test
    fun whenTheLabelsListEmptyShouldHideTheView() {
        testView.setLabels(emptyList())

        onTestView().check(isGone())
    }

    @Test
    fun whenHasLabelsShouldShowTheView() {
        testView.setLabels(LabelList.withThreeItems)

        onTestView().check(isVisible())
    }

    @Test
    fun whenHasThreeLabelsShouldShowThemAllWithoutMoreTextView() {
        runOnActivityThread {
            testView.setLabels(LabelList.withThreeItems)
        }

        LabelList.withThreeItems.forEach { expectedLabel ->
            onView(withLabelId(expectedLabel.id))
                .check(isVisible())
                .check(
                    matches(
                        withDrawable(
                            R.drawable.bg_collapsed_label,
                            expectedLabel.color
                        )
                    )
                )
        }
        onMoreTextView().check(isGone())
    }

    @Test
    fun whenHasFourLabelsShouldShowFirstThreeAndTheMoreTextView() {
        val visibleLabels = LabelList.withFourItems.take(3)
        val hiddenLabel = LabelList.withFourItems.last()

        runOnActivityThread {
            testView.setLabels(LabelList.withFourItems)
        }

        visibleLabels.forEach { visibleLabel ->
            onView(withLabelId(visibleLabel.id))
                .check(isVisible())
                .check(
                    matches(
                        withDrawable(
                            R.drawable.bg_collapsed_label,
                            visibleLabel.color
                        )
                    )
                )
        }
        onView(withLabelId(hiddenLabel.id)).check(isGone())
        onMoreTextView()
            .check(isVisible())
            .check(matches(withText("+1")))
    }

    private fun withLabelId(labelId: LabelId): Matcher<View> {
        return object : TypeSafeDiagnosingMatcher<View>() {

            override fun matchesSafely(item: View, mismatchDescription: Description) =
                (item as? CollapsedMessageLabelView)?.labelId == labelId

            override fun describeTo(description: Description) {
                description.appendText("Label id: ${labelId.id}")
            }
        }
    }

    private fun onMoreTextView(): ViewInteraction = onView(withId(R.id.more_items_ll_more_text_view))
}

private object LabelList {
    val withThreeItems = listOf(
        LabelChipUiModel(LabelId("a"), Name("first"), Color.RED),
        LabelChipUiModel(LabelId("b"), Name("second"), Color.GREEN),
        LabelChipUiModel(LabelId("c"), Name("third"), Color.BLUE),
    )

    val withFourItems = withThreeItems.toMutableList().plus(
        LabelChipUiModel(LabelId("d"), Name("fourth"), Color.YELLOW)
    )
}

