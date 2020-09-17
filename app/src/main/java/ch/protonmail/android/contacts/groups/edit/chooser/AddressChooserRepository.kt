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
package ch.protonmail.android.contacts.groups.edit.chooser

import android.util.Log
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Created by kadrikj on 9/7/18. */

@Singleton
class AddressChooserRepository @Inject constructor(private val databaseProvider: DatabaseProvider) {

    private val contactsDatabase by lazy { /*TODO*/ Log.d("PMTAG", "instantiating contactsDatabase in AddressChooserRepository"); databaseProvider.provideContactsDao() }

    fun getContactGroupEmails(): Observable<List<ContactEmail>> {
        return contactsDatabase.findAllContactsEmailsAsyncObservable()
                .toObservable()
    }

    fun filterContactGroupEmails(filter: String): Observable<List<ContactEmail>> {
        val filterString = "%$filter%"
        return contactsDatabase.findAllContactsEmailsAsyncObservable(filterString)
                .toObservable()
    }
}
