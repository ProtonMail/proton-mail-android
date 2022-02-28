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
package ch.protonmail.android.contacts.groups.list

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import ch.protonmail.android.R
import ch.protonmail.android.contacts.IContactsFragment
import ch.protonmail.android.contacts.list.ContactsListFragment
import ch.protonmail.android.contacts.list.search.ISearchListenerViewModel

// region constants
private const val COUNT = 2
// endregion

class ContactsFragmentsPagerAdapter(
    val context: Context,
    fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager) {

    private val fragmentsTags = arrayOf<String?>(null, null)
    private val fragmentCounts = arrayOf(-1, -1)
    private var hasPermission: Boolean = false

    override fun getPageTitle(position: Int): CharSequence = when (position) {
        0 -> {
            if (fragmentCounts[0] == -1) {
                context.getString(R.string.contacts)
            } else {
                String.format(context.getString(R.string.tab_contacts), fragmentCounts[0])
            }
        }
        1 -> {
            if (fragmentCounts[1] == -1) {
                context.getString(R.string.groups)
            } else {
                String.format(context.getString(R.string.tab_contact_groups), fragmentCounts[1])
            }
        }
        else -> ""
    }

    override fun getItem(position: Int): Fragment =
        if (position == 0) ContactsListFragment.newInstance(hasPermission)
        else ContactGroupsFragment.newInstance()


    override fun getCount(): Int = COUNT

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val createdFragment = super.instantiateItem(container, position) as Fragment
        fragmentsTags[position] = createdFragment.tag
        return createdFragment
    }

    fun getSearchListeners(fragmentManager: FragmentManager): List<ISearchListenerViewModel> {
        val searchListenersList = ArrayList<ISearchListenerViewModel>()
        fragmentsTags.forEach {
            searchListenersList.add((fragmentManager.findFragmentByTag(it) as IContactsFragment).getSearchListener())
        }
        return searchListenersList
    }

    fun update(position: Int, count: Int) {
        fragmentCounts[position] = count
    }

    fun onContactPermissionChange(fragmentManager: FragmentManager, hasPermission: Boolean) {
        this.hasPermission = hasPermission
        fragmentsTags.forEach {
            (fragmentManager.findFragmentByTag(it) as? IContactsFragment)?.onContactPermissionChange(hasPermission)
        }
    }
}
