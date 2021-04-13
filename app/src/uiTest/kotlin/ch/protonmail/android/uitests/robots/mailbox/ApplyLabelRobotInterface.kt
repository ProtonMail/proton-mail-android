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
import ch.protonmail.android.uitests.robots.mailbox.MailboxMatchers.withLabelName
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import ch.protonmail.android.uitests.testsHelper.uiactions.click
import ch.protonmail.android.uitests.testsHelper.uiactions.type
import me.proton.core.test.android.instrumented.CoreRobot

interface ApplyLabelRobotInterface: CoreRobot {

    fun addLabel(name: String): Any {
        labelName(name)
            .add()
        return this
    }

    fun labelName(name: String): ApplyLabelRobotInterface {
        UIActions.wait
            .forViewWithIdAndParentId(R.id.label_name, R.id.add_label_container)
            .type(name)
        return this
    }

    fun selectLabelByName(name: String): ApplyLabelRobotInterface {
        UIActions.wait.forViewWithId(R.id.labels_list_view)
        UIActions.wait.forViewWithText(name)
        UIActions.listView.clickListItemChildByTextAndId(
            withLabelName(name),
            R.id.label_check,
            R.id.labels_list_view
        )
        return this
    }

    fun checkAlsoArchiveCheckBox(): ApplyLabelRobotInterface {
        view.withId(R.id.also_archive).click()
        return this
    }

    fun apply(): Any {
        view.withId(R.id.done).click()
        return this
    }

    fun applyAndArchive(): Any {
        apply()
        return this
    }

    fun add() {
        view.withId(R.id.done).click()
    }

    fun closeLabelModal(): Any {
        view.withId(R.id.close).click()
        return Any()
    }
}
