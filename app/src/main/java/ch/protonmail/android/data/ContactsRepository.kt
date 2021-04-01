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

import androidx.lifecycle.LiveData
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.ContactEmail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

class ContactsRepository @Inject constructor(
    private val databaseProvider: DatabaseProvider,
    private val userManager: UserManager,
    private val dispatchers: DispatcherProvider
) {

    private val contactDao by lazy {
        Timber.v("Instantiating contactDao in ContactsRepository")
        databaseProvider.provideContactDao(userManager.requireCurrentUserId())
    }

    fun findContactEmailByEmailLiveData(email: String): LiveData<ContactEmail> =
        contactDao.findContactEmailByEmailLiveData(email)

    suspend fun findAllContactEmails(): Flow<List<ContactEmail>> =
        withContext(dispatchers.Io) {
            contactDao.findAllContactsEmails()
        }

}
