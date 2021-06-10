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
package ch.protonmail.android.activities.contactDetails

import android.database.sqlite.SQLiteBlobTooBigException
import android.os.AsyncTask
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.FullContactDetails
import timber.log.Timber

@Deprecated("removed with the new ContactDetailsViewModel")
class ExtractFullContactDetailsTask(
    private val contactDao: ContactDao,
    private val contactId: String,
    private val callback: (FullContactDetails?) -> Unit
) : AsyncTask<Void, Void, FullContactDetails>() {

    override fun doInBackground(vararg voids: Void): FullContactDetails? {
        return try {
            contactDao.findFullContactDetailsByIdBlocking(contactId)
        } catch (tooBigException: SQLiteBlobTooBigException) {
            Timber.i(tooBigException, "Data too big to be fetched")
            null
        }
    }

    override fun onPostExecute(fullContactDetails: FullContactDetails?) {
        callback(fullContactDetails)
    }
}
