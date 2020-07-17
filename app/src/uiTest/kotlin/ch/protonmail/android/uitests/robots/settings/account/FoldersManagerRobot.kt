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
package ch.protonmail.android.uitests.robots.settings.account

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToHolder
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withLabelName
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [FoldersManagerRobot] class contains actions and verifications for Folders functionality.
 */
class FoldersManagerRobot {

    fun addFolder(name: String): FoldersManagerRobot {
        folderName(name)
            .saveNewFolder()
        return this
    }

    private fun folderName(name: String): FoldersManagerRobot {
        UIActions.id.typeTextIntoFieldWithId(R.id.label_name, name)
        return this
    }

    private fun saveNewFolder(): FoldersManagerRobot {
        UIActions.id.clickViewWithId(R.id.save_new_label)
        return this
    }

    /**
     * Contains all the validations that can be performed by [FoldersManagerRobot].
     */
    class Verify {

        fun folderWithNameShown(name: String) {
            onView(withId(R.id.labels_recycler_view)).perform(scrollToHolder(withLabelName(name)))
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}