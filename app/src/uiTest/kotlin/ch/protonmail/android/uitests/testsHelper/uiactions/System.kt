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

package ch.protonmail.android.uitests.testsHelper.uiactions

import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.withParent
import ch.protonmail.android.R
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf

object System {
    fun clickHamburgerOrUpButton(): ViewInteraction =
        UIActions.allOf.clickViewWithParentIdAndClass(R.id.toolbar, AppCompatImageButton::class.java)

    fun clickHamburgerOrUpButtonInAnimatedToolbar(): ViewInteraction =
        UIActions.allOf.clickViewWithParentIdAndClass(R.id.animToolbar, AppCompatImageButton::class.java)

    fun waitForMoreOptionsButton(): ViewInteraction =
        UIActions.wait.forViewByViewInteraction(
            onView(
                allOf(
                    instanceOf(AppCompatImageView::class.java),
                    withParent(instanceOf(ActionMenuView::class.java))
                )
            )
        )

    fun clickMoreOptionsButton(): ViewInteraction =
        UIActions.allOf.clickViewByClassAndParentClass(AppCompatImageView::class.java, ActionMenuView::class.java)

    fun clickNegativeDialogButton(): ViewInteraction = UIActions.id.clickViewWithId(android.R.id.button2)

    fun clickPositiveDialogButton(): ViewInteraction = UIActions.id.clickViewWithId(android.R.id.button1)

    fun clickPositiveButtonInDialogRoot(): ViewInteraction = UIActions.id.clickViewWithId(android.R.id.button1)
}
