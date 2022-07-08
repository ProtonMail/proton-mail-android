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
package ch.protonmail.android.uitests.robots.settings.account.labelsandfolders

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.R
import me.proton.fusion.Fusion

/**
 * [LabelsAndFoldersRobot] class contains actions and verifications for
 * Labels & Folders functionality.
 */
class LabelsAndFoldersRobot : Fusion {

    fun labelsManager(): LabelsManagerRobot {
        view.withTag(R.string.label_add).click()
        return LabelsManagerRobot()
    }

    fun foldersManager(): FoldersManagerRobot {
        view.withTag(R.string.label_add).click()
        return FoldersManagerRobot()
    }

    fun scrollToClickDelete(): FoldersManagerRobot {
        view.withId(R.id.delete_contacts).scrollTo().click()
        return FoldersManagerRobot()
    }
}
