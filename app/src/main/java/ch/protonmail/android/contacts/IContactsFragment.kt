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

import android.view.ActionMode
import android.widget.AbsListView
import androidx.annotation.Px
import ch.protonmail.android.contacts.list.search.ISearchListenerViewModel

interface IContactsFragment : AbsListView.MultiChoiceModeListener {
    val actionMode: ActionMode?

    fun getSearchListener(): ISearchListenerViewModel
    fun onContactPermissionChange(hasPermission: Boolean)
    fun onDelete()
    fun updateRecyclerViewBottomPadding(@Px size: Int)
}
