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
package ch.protonmail.android.uitests.robots.settings.account.labelsandfolders

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToHolder
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withLabelName
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import ch.protonmail.android.uitests.testsHelper.uiactions.click
import ch.protonmail.android.uitests.testsHelper.uiactions.insert
import ch.protonmail.android.uitests.testsHelper.uiactions.type

/**
 * [FoldersManagerRobot] class contains actions and verifications for Folders functionality.
 */
class FoldersManagerRobot {

    fun addFolder(name: String): FoldersManagerRobot {
        folderName(name)
            .saveFolder()
        return this
    }

    fun deleteFolder(name: String): FoldersManagerRobot {
        selectFolderCheckbox(name)
            .clickDeleteSelectedButton()
            .confirmDeletion()
        return this
    }

    fun editFolder(name: String, newName: String, colorPosition: Int): FoldersManagerRobot {
        selectFolder(name)
            .updateFolderName(newName)
            .saveFolder()
        return this
    }

    fun navigateUpToLabelsAndFolders(): LabelsAndFoldersRobot {
        UIActions.system.clickHamburgerOrUpButton()
        return LabelsAndFoldersRobot()
    }

    private fun clickDeleteSelectedButton(): DeleteSelectedFoldersDialogRobot {
        UIActions.id.clickViewWithId(R.id.delete_labels)
        return DeleteSelectedFoldersDialogRobot()
    }

    private fun clickFolder(name: String): FoldersManagerRobot {
        UIActions.id.clickViewWithId(R.id.save_new_label)
        return this
    }

    private fun folderName(name: String): FoldersManagerRobot {
        UIActions.wait.forViewWithIdAndParentId(R.id.label_name, R.id.add_label_container).type(name)
        return this
    }

    private fun updateFolderName(name: String): FoldersManagerRobot {
        UIActions.wait.forViewWithIdAndParentId(R.id.label_name, R.id.add_label_container).insert(name)
        return this
    }

    private fun saveFolder(): FoldersManagerRobot {
        UIActions.wait.forViewWithId(R.id.save_new_label).click()
        return this
    }

    private fun selectFolder(name: String): FoldersManagerRobot {
        UIActions.wait.forViewWithId(R.id.labels_recycler_view)
        UIActions.recyclerView.common.clickOnRecyclerViewMatchedItem(R.id.labels_recycler_view, withLabelName(name))
        return this
    }

    private fun selectFolderCheckbox(name: String): FoldersManagerRobot {
        UIActions.wait.forViewWithId(R.id.labels_recycler_view)
        UIActions.recyclerView.common
            .clickOnRecyclerViewItemChild(R.id.labels_recycler_view, withLabelName(name), R.id.label_check)
        return this
    }


    class DeleteSelectedFoldersDialogRobot {

        fun confirmDeletion(): FoldersManagerRobot {
            UIActions.system.clickPositiveDialogButton()
            return FoldersManagerRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [FoldersManagerRobot].
     */
    class Verify {

        fun folderWithNameShown(name: String) {
            onView(withId(R.id.labels_recycler_view)).perform(scrollToHolder(withLabelName(name)))
        }

        fun folderWithNameDoesNotExist(name: String) {
            UIActions.wait.untilViewWithTextIsGone(name)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
