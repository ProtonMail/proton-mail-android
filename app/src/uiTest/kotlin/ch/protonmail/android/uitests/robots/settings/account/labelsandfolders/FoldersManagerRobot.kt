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

import androidx.appcompat.widget.AppCompatImageButton
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withLabelName
import me.proton.fusion.Fusion

/**
 * [FoldersManagerRobot] class contains actions and verifications for Folders functionality.
 */
class FoldersManagerRobot : Fusion {

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
        view.instanceOf(AppCompatImageButton::class.java).hasParent(view.withId(R.id.toolbar)).click()
        return LabelsAndFoldersRobot()
    }

    private fun clickDeleteSelectedButton(): DeleteSelectedFoldersDialogRobot {
        view.withId(R.id.delete_labels).click()
        return DeleteSelectedFoldersDialogRobot()
    }

    private fun clickFolder(name: String): FoldersManagerRobot {
        view.withId(R.id.label).click()
        return this
    }

    private fun folderName(name: String): FoldersManagerRobot {
        view.withId(R.id.label).hasParent(view.withId(R.id.add_label_container)).typeText(name)
        return this
    }

    private fun updateFolderName(name: String): FoldersManagerRobot {
        view.withId(R.id.label).hasParent(view.withId(R.id.add_label_container)).clearText().typeText(name)
        return this
    }

    private fun saveFolder(): FoldersManagerRobot {
        view.withId(R.id.label).click()
        return this
    }

    private fun selectFolder(name: String): FoldersManagerRobot {
        recyclerView
            .withId(R.id.labels_recycler_view)
//            .waitUntilPopulated()
            .onHolderItem(withLabelName(name))
            .click()
        return this
    }

    private fun selectFolderCheckbox(name: String): FoldersManagerRobot {
        recyclerView
            .withId(R.id.labels_recycler_view)
//            .waitUntilPopulated()
            .onHolderItem(withLabelName(name))
            .onItemChildView(view.withId(R.id.label))
            .click()
        return this
    }

    class DeleteSelectedFoldersDialogRobot : Fusion {

        fun confirmDeletion(): FoldersManagerRobot {
            view.withId(android.R.id.button1).click()
            return FoldersManagerRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [FoldersManagerRobot].
     */
    class Verify : Fusion {

        fun folderWithNameShown(name: String) {
            recyclerView
                .withId(R.id.labels_recycler_view)
                .scrollToHolder(withLabelName(name))
        }

        fun folderWithNameDoesNotExist(name: String) {
            view.withText(name).checkDoesNotExist()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
