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
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import ch.protonmail.android.uitests.testsHelper.uiactions.click

interface SelectionStateRobotInterface {

    fun exitMessageSelectionState(): Any {
        UIActions.system.clickHamburgerOrUpButton()
        return Any()
    }

    fun openMoreOptions(): Any {
        UIActions.system.clickMoreOptionsButton()
        return Any()
    }

    fun addLabel(): Any {
        UIActions.wait.forViewWithId(R.id.add_label).click()
        return Any()
    }

    fun addFolder(): Any {
        UIActions.wait.forViewWithId(R.id.add_folder).click()
        return Any()
    }

    fun selectMessage(position: Int): Any {
        UIActions.recyclerView.common.clickOnRecyclerViewItemByPosition(R.id.messages_list_view, position)
        return Any()
    }
}
