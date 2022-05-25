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
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToHolder
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withLabelName

/**
 * [LabelsManagerRobot] class contains actions and verifications for Labels functionality.
 */
class LabelsManagerRobot {

    fun addLabel(name: String): LabelsManagerRobot {
        return labelName(name)
            .saveNewLabel()
    }

    fun editLabel(name: String, newName: String, colorPosition: Int): LabelsManagerRobot {
        selectLabel(name)
            .updateLabelName(newName)
            .saveNewLabel()
        return this
    }

    fun deleteLabel(name: String): LabelsManagerRobot {
        selectFolderCheckbox(name)
            .clickDeleteSelectedButton()
            .confirmDeletion()
        return this
    }

    private fun clickDeleteSelectedButton(): FoldersManagerRobot.DeleteSelectedFoldersDialogRobot {
//        UIActions.id.clickViewWithId(R.id.delete_labels)
        return FoldersManagerRobot.DeleteSelectedFoldersDialogRobot()
    }

    private fun selectFolderCheckbox(name: String): LabelsManagerRobot {
//        UIActions.wait.forViewWithId(R.id.labels_recycler_view)
//        UIActions.recyclerView.common
//            .clickOnRecyclerViewItemChild(R.id.labels_recycler_view, withLabelName(name), R.id.label_check)
        return this
    }


    private fun selectLabel(name: String): LabelsManagerRobot {
//        UIActions.wait.forViewWithId(R.id.labels_recycler_view)
//        UIActions.recyclerView.common.clickOnRecyclerViewMatchedItem(R.id.labels_recycler_view, withLabelName(name))
        return this
    }

    private fun updateLabelName(name: String): LabelsManagerRobot {
//        UIActions.wait.forViewWithIdAndParentId(R.id.label_name, R.id.add_label_container)
//            .insert(name)
        return this
    }

    private fun labelName(name: String): LabelsManagerRobot {
//        UIActions.id.typeTextIntoFieldWithId(R.id.label_name, name)
        return this
    }

    private fun saveNewLabel(): LabelsManagerRobot {
//        UIActions.id.clickViewWithId(R.id.save_new_label)
        return this
    }

    /**
     * Contains all the validations that can be performed by [LabelsManagerRobot].
     */
    class Verify {

        fun labelWithNameShown(name: String) {
            onView(withId(R.id.labels_recycler_view)).perform(scrollToHolder(withLabelName(name)))
        }

        fun labelWithNameDoesNotExist(name: String) {
//            UIActions.wait.untilViewWithTextIsGone(name)
        }

    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
