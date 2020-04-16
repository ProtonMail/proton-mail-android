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

import androidx.lifecycle.MutableLiveData
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader

internal class AndroidContactsLoaderCallbacks<T>(private val context:Context,
											  private val localContactsLiveData:MutableLiveData<List<T>>,
											  private val itemListFactory:(Cursor)->List<T>): LoaderManager.LoaderCallbacks<Cursor> {
	override fun onCreateLoader(loaderId:Int,args:Bundle?): Loader<Cursor> {

		val searchString=args?.getString(EXTRA_SEARCH_PHRASE) ?: ""
		val selectionArgs=arrayOf("%$searchString%","%$searchString%","%$searchString%")
		return CursorLoader(
                context,
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                ANDROID_PROJECTION,
                ANDROID_SELECTION,
                selectionArgs,
                ANDROID_ORDER_BY
        )
	}


	override fun onLoadFinished(loader: Loader<Cursor>, data:Cursor) {
		if(data.isBeforeFirst)
			localContactsLiveData.value=itemListFactory(data)
	}

	override fun onLoaderReset(loader: Loader<Cursor>) {
		localContactsLiveData.value=emptyList()
	}

	companion object {
		private const val ANDROID_ORDER_BY = ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY + " ASC"
		private const val ANDROID_SELECTION = (
				ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY + " LIKE ?" + " OR " + ContactsContract.CommonDataKinds.Email.ADDRESS + " LIKE ?" + " OR "
						+ ContactsContract.CommonDataKinds.Email.DATA + " LIKE ?")
		private val ANDROID_PROJECTION = arrayOf(ContactsContract.CommonDataKinds.Email.RAW_CONTACT_ID,
				ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY,
				ContactsContract.CommonDataKinds.Email.ADDRESS,
				ContactsContract.CommonDataKinds.Email.DATA)
	}
}