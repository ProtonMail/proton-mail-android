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
package ch.protonmail.android.uitests.robots.shared

import androidx.appcompat.widget.AppCompatImageButton
import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [SharedRobot] class contains commonly used actions that usually depend on system UI components.
 * Examples: UP button click, confirming or cancelling dialogs and similar.
 */
object SharedRobot : UIActions() {

    fun clickHamburgerOrUpButton() =
        clickObjectWithParentIdAndClass(R.id.toolbar, AppCompatImageButton::class.java)

    fun clickPositiveDialogButton() = clickOnObjectWithId(android.R.id.button1)
}