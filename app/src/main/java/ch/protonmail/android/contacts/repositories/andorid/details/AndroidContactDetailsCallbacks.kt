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

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import androidx.lifecycle.MutableLiveData
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import ch.protonmail.android.contacts.repositories.andorid.details.AndroidContactDetailsRepository.Companion.ANDROID_DETAILS_PROJECTION
import ch.protonmail.android.contacts.repositories.andorid.details.AndroidContactDetailsRepository.Companion.ANDROID_DETAILS_SELECTION
import ch.protonmail.android.utils.Event
import ch.protonmail.android.views.models.LocalContact
import ch.protonmail.android.views.models.LocalContactAddress
import java.util.ArrayList

// region constants
const val ARGUMENT_CONTACT_ID = "ARG_CONTACT_ID"
const val LOADER_ID_ANDROID_CONTACTS_DETAILS = 4
// endregion

// TODO remove duplication between this class and ConvertLocalContactsJob.kt
class AndroidContactDetailsCallbacks(
    private val context: Context,
    private val contactDetails: MutableLiveData<Event<LocalContact>>
) : LoaderManager.LoaderCallbacks<Cursor> {

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val contactId = args?.getString(ARGUMENT_CONTACT_ID) ?: throw IllegalArgumentException("Unknown contact id")
        val selectionArgs = arrayOf(contactId)
        return CursorLoader(
            context,
            ContactsContract.Data.CONTENT_URI,
            ANDROID_DETAILS_PROJECTION,
            ANDROID_DETAILS_SELECTION,
            selectionArgs,
            null
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        data ?: return
        if (data.isAfterLast)
            return
        var name = ""
        val phones = ArrayList<String>()
        val emails = ArrayList<String>()
        val addresses = ArrayList<LocalContactAddress>()

        while (data.moveToNext()) {
            val dataKind = data.getString(data.getColumnIndex(ContactsContract.Data.MIMETYPE))
            when (dataKind) {
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> phones.add(data.getString(
                    data.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER)))
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                    name = data.getString(
                        data.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    emails.add(data.getString(data.getColumnIndex(
                        ContactsContract.CommonDataKinds.Email.ADDRESS)))
                }
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                    val street = data.getString(data.getColumnIndex(
                        ContactsContract.CommonDataKinds.StructuredPostal.STREET))
                    val city = data.getString(data.getColumnIndex(
                        ContactsContract.CommonDataKinds.StructuredPostal.CITY))
                    val region = data.getString(data.getColumnIndex(
                        ContactsContract.CommonDataKinds.StructuredPostal.REGION))
                    val postcode = data.getString(data.getColumnIndex(
                        ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE))
                    val country = data.getString(data.getColumnIndex(
                        ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY))
                    addresses.add(LocalContactAddress(street, city, region, postcode, country))
                }
                else -> {
                    //ignore
                }
            }
        }

        contactDetails.value = Event(LocalContact(name, emails, phones, addresses, emptyList() /* TODO get contact groups if it makes sense later */))
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        //NO OP
    }
}
