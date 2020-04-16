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
import android.os.Bundle
import androidx.loader.app.LoaderManager

// region constants
private const val LOADER_ID_ANDROID_CONTACTS = 1
const val EXTRA_SEARCH_PHRASE = "EXTRA_SEARCH_PHRASE"
// endregion

class AndroidContactsRepository<T>(private val loaderManager: LoaderManager,
                                   private val androidContactsLoaderCallbacksFactory:IAndroidContactsLoaderCallbacksFactory<T>):IAndroidContactsRepository<T> {

	private var hasPermission:Boolean=false
	private var searchPhrase:String=""

	override val androidContacts=MutableLiveData<List<T>>()

	override fun setContactsPermission(hasPermission:Boolean) {
		this.hasPermission=hasPermission
		if(hasPermission) {
			reloadContacts()
		} else {
			close()
		}
	}

	override fun setSearchPhrase(searchPhrase:String) {
		this.searchPhrase=searchPhrase
		if(hasPermission) {
			reloadContacts()
		}
	}

	private fun reloadContacts() {
		val args=Bundle()
		args.putString(EXTRA_SEARCH_PHRASE,searchPhrase)
		val callbacks=androidContactsLoaderCallbacksFactory.createAndroidContactsLoaderCallbacks(
				androidContacts)
		loaderManager.restartLoader(LOADER_ID_ANDROID_CONTACTS,args,callbacks)
	}

	override fun close() {
		loaderManager.destroyLoader(LOADER_ID_ANDROID_CONTACTS)
	}
}