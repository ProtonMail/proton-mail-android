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
package ch.protonmail.android.contacts

import android.content.Intent
import android.view.ActionMode

interface IContactsListFragmentListener {

    fun dataUpdated(position: Int, count: Int)

    fun setTitle(title: String)

    fun selectPage(position: Int)

    fun doRequestContactsPermission()

    fun doStartActionMode(callback: ActionMode.Callback): ActionMode?

    fun doStartActivityForResult(intent: Intent, requestCode: Int)

    fun registerObject(registerObject: Any)

    fun unregisterObject(unregisterObject: Any)
}
