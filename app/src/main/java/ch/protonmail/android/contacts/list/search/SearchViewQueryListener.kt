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
package ch.protonmail.android.contacts.list.search

import androidx.appcompat.widget.SearchView

internal class SearchViewQueryListener(
    private val searchView: SearchView,
    private val viewModels: List<ISearchListenerViewModel>
) : SearchView.OnQueryTextListener {

    override fun onQueryTextSubmit(searchPhrase: String): Boolean {
        for (viewModel in viewModels) {
            viewModel.setSearchPhrase(searchPhrase)
        }
        searchView.clearFocus()
        return false
    }

    override fun onQueryTextChange(searchPhrase: String): Boolean {
        for (viewModel in viewModels) {
            viewModel.setSearchPhrase(searchPhrase)
        }
        return true
    }
}
