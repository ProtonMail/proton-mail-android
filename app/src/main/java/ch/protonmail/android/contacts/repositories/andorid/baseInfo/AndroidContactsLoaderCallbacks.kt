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
package ch.protonmail.android.contacts.repositories.andorid.baseInfo

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds
import androidx.lifecycle.MutableLiveData
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import ch.protonmail.android.contacts.list.listView.ContactItem

internal class AndroidContactsLoaderCallbacks(
    private val context: Context,
    private val localContactsLiveData: MutableLiveData<List<ContactItem>>,
    private val itemListFactory: (Cursor) -> List<ContactItem>
) : LoaderManager.LoaderCallbacks<Cursor> {

    override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Cursor> {

        val searchString = args?.getString(EXTRA_SEARCH_PHRASE) ?: ""
        val selectionArgs = arrayOf("%$searchString%", "%$searchString%", "%$searchString%")
        return CursorLoader(
            context,
            CommonDataKinds.Email.CONTENT_URI,
            ANDROID_PROJECTION,
            ANDROID_SELECTION,
            selectionArgs,
            ANDROID_ORDER_BY
        )
    }


    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        if (data.isBeforeFirst)
            localContactsLiveData.value = itemListFactory(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        localContactsLiveData.value = emptyList()
    }

    companion object {

        private const val ANDROID_ORDER_BY = CommonDataKinds.Email.DISPLAY_NAME_PRIMARY + " ASC"

        private const val ANDROID_SELECTION = """
            ${CommonDataKinds.Email.DISPLAY_NAME_PRIMARY} LIKE ? 
                OR ${CommonDataKinds.Email.ADDRESS} LIKE ?
                OR ${CommonDataKinds.Email.DATA} LIKE ?
        """

        private val ANDROID_PROJECTION = arrayOf(
            CommonDataKinds.Email.RAW_CONTACT_ID,
            CommonDataKinds.Email.DISPLAY_NAME_PRIMARY,
            CommonDataKinds.Email.ADDRESS,
            CommonDataKinds.Email.DATA
        )
    }
}
