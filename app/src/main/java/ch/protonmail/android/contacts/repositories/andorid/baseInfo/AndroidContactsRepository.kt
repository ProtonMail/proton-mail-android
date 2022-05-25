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
package ch.protonmail.android.contacts.repositories.andorid.baseInfo

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.loader.app.LoaderManager
import ch.protonmail.android.contacts.list.listView.ContactItem
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import java.io.Closeable

// region constants
private const val LOADER_ID_ANDROID_CONTACTS = 1
const val EXTRA_SEARCH_PHRASE = "EXTRA_SEARCH_PHRASE"
// endregion

class AndroidContactsRepository @AssistedInject constructor (
    @Assisted private val loaderManager: LoaderManager,
    private val androidContactsLoaderCallbacksFactory: AndroidContactsLoaderCallbacksFactory
) : Closeable {

    private var hasPermission: Boolean = false
    private var searchPhrase: String = ""

    val androidContacts = MutableLiveData<List<ContactItem>>()

    fun setContactsPermission(hasPermission: Boolean) {
        this.hasPermission = hasPermission
        if (hasPermission) {
            reloadContacts()
        } else {
            close()
        }
    }

    fun setSearchPhrase(searchPhrase: String) {
        this.searchPhrase = searchPhrase
        if (hasPermission) {
            reloadContacts()
        }
    }

    private fun reloadContacts() {
        val args = Bundle()
        args.putString(EXTRA_SEARCH_PHRASE, searchPhrase)
        val callbacks = androidContactsLoaderCallbacksFactory.createAndroidContactsLoaderCallbacks(androidContacts)
        loaderManager.restartLoader(LOADER_ID_ANDROID_CONTACTS, args, callbacks)
    }

    override fun close() {
        loaderManager.destroyLoader(LOADER_ID_ANDROID_CONTACTS)
    }

    @AssistedInject.Factory
    interface AssistedFactory {

        fun create(loaderManager: LoaderManager): AndroidContactsRepository
    }
}
