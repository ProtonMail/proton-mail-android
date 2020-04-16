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
package ch.protonmail.android.data

import android.util.Log
import androidx.lifecycle.LiveData
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import javax.inject.Inject

class ContactsRepository @Inject constructor(private val databaseProvider: DatabaseProvider) {

    private val contactsDao by lazy { /*TODO*/ Log.d("PMTAG", "instantiating contactsDao in ContactsRepository"); databaseProvider.provideContactsDao() }

    fun findContactEmailByEmailLiveData(email: String): LiveData<ContactEmail> = contactsDao.findContactEmailByEmailLiveData(email)

    fun findAllContactsEmailsAsync(): LiveData<List<ContactEmail>> = contactsDao.findAllContactsEmailsAsync()

}
