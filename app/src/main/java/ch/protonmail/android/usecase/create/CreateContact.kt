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

package ch.protonmail.android.usecase.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.WorkInfo
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.utils.extensions.filter
import ch.protonmail.android.worker.CreateContactWorker
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class CreateContact @Inject constructor(
   private val dispatcherProvider: DispatcherProvider,
   private val contactsDao: ContactsDao,
   private val createContactScheduler: CreateContactWorker.Enqueuer
) {

    suspend operator fun invoke(
        contactData: ContactData,
        contactEmails: List<ContactEmail>,
        encryptedContactData: String,
        signedContactData: String
    ): LiveData<CreateContactResult> =
        withContext(dispatcherProvider.Io) {
            contactEmails.forEach { it.contactId = contactData.contactId }
            contactsDao.saveAllContactsEmails(contactEmails)

            createContactScheduler.enqueue(encryptedContactData, signedContactData)
                .filter { it?.state?.isFinished == true }
                .map { workInfo ->
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        CreateContactResult.Success
                    } else {
                        CreateContactResult.Error
                    }

                }
        }

    sealed class CreateContactResult {
        object Success : CreateContactResult()
        object Error : CreateContactResult()
    }
}
