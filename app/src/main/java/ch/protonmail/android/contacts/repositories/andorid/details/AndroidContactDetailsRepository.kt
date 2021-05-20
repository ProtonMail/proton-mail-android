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
package ch.protonmail.android.contacts.repositories.andorid.details

import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import androidx.lifecycle.MutableLiveData
import androidx.loader.app.LoaderManager
import ch.protonmail.android.utils.Event
import ch.protonmail.android.views.models.LocalContact
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import timber.log.Timber

class AndroidContactDetailsRepository @AssistedInject constructor(
    @Assisted private val loaderManager: LoaderManager,
    private val callbacksFactory: AndroidContactDetailsCallbacksFactory
) {

    val contactDetails = MutableLiveData<Event<LocalContact>>()

    fun makeQuery(contactId: String) {
        val args = Bundle()
        args.putString(ARGUMENT_CONTACT_ID, contactId)
        val loaderCallbacks = callbacksFactory.create(contactDetails)
        Timber.v("makeQuery id: $contactId restartLoader")
        loaderManager.restartLoader(
            LOADER_ID_ANDROID_CONTACTS_DETAILS,
            args,
            loaderCallbacks
        )
    }

    companion object {

        const val ANDROID_DETAILS_SELECTION =
            Data.RAW_CONTACT_ID + " = ? AND (" +
                Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE +
                "' OR " + Data.MIMETYPE + "='" + CommonDataKinds.Email.CONTENT_ITEM_TYPE +
                "' OR " + Data.MIMETYPE + "='" + CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE +
                "' OR " + Data.MIMETYPE + "='" + CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE +
                "')"


        val ANDROID_DETAILS_PROJECTION = arrayOf(
            Contacts._ID,
            Data.MIMETYPE,
            Contacts.DISPLAY_NAME,
            CommonDataKinds.Phone.NUMBER,
            CommonDataKinds.Email.ADDRESS,
            CommonDataKinds.StructuredPostal.STREET,
            CommonDataKinds.StructuredPostal.CITY,
            CommonDataKinds.StructuredPostal.REGION,
            CommonDataKinds.StructuredPostal.POSTCODE,
            CommonDataKinds.StructuredPostal.COUNTRY,
            CommonDataKinds.GroupMembership.GROUP_ROW_ID
        )
    }

    @AssistedInject.Factory
    interface AssistedFactory {

        fun create(loaderManager: LoaderManager): AndroidContactDetailsRepository
    }
}
