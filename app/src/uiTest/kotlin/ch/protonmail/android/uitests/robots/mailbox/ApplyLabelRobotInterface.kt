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
package ch.protonmail.android.uitests.robots.mailbox

import ch.protonmail.android.R
import me.proton.core.test.android.instrumented.Robot

interface ApplyLabelRobotInterface : Robot {

    fun addLabel(name: String): Any {
        labelName(name)
            .add()
        return this
    }

    fun labelName(name: String): ApplyLabelRobotInterface {
        view.withId(R.id.label_name_text_view).withParent(view.withId(R.id.add_label_container)).typeText(name)
        return this
    }

    fun selectLabelByName(name: String): ApplyLabelRobotInterface {
        view.withId(R.id.textview_checkbox_manage_labels_title).withText(name).click()
        return this
    }

    fun checkAlsoArchiveCheckBox(): ApplyLabelRobotInterface {
        view.withId(R.id.labels_sheet_archive_switch).click()
        return this
    }

    fun apply(): Any {
        view.withId(R.id.textview_actions_sheet_right_action).click()
        return this
    }

    fun applyAndArchive(): Any {
        apply()
        return this
    }

    fun add() {
        view.withId(R.id.add).click()
    }

    fun closeLabelModal(): Any {
        view.withId(R.id.closeButton).click()
        return Any()
    }
}
