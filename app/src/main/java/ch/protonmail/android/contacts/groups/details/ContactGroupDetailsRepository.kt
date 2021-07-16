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
package ch.protonmail.android.contacts.groups.details

import androidx.work.WorkManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.contacts.receive.ContactLabelFactory
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactLabel
import ch.protonmail.android.worker.PostLabelWorker
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.proton.core.util.kotlin.toInt
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class ContactGroupDetailsRepository @Inject constructor(
    private val api: ProtonMailApiManager,
    private val databaseProvider: DatabaseProvider,
    private val workManager: WorkManager,
    private val userManager: UserManager
) {

    private val contactDao by lazy {
        Timber.v("Instantiating contactDao in ContactGroupDetailsRepository")
        databaseProvider.provideContactDao(userManager.requireCurrentUserId())
    }

    fun findContactGroupDetailsBlocking(id: String): Single<ContactLabel> =
        contactDao.findContactGroupByIdAsync(id)

    suspend fun findContactGroupDetails(id: String): ContactLabel? =
        contactDao.findContactGroupById(id).first()

    fun getContactGroupEmailsBlocking(id: String): Observable<List<ContactEmail>> {
        return contactDao.findAllContactsEmailsByContactGroupAsyncObservable(id)
            .toObservable()
    }

    fun getContactGroupEmails(id: String): Flow<List<ContactEmail>> =
        contactDao.findAllContactsEmailsByContactGroup(id)

    fun filterContactGroupEmails(id: String, filter: String): Flow<List<ContactEmail>> =
        contactDao.filterContactsEmailsByContactGroup(id, "%$filter%")

    fun createContactGroup(contactLabel: ContactLabel): Single<ContactLabel> {
        val contactLabelConverterFactory = ContactLabelFactory()
        val labelBody = contactLabelConverterFactory.createServerObjectFromDBObject(contactLabel)
        return api.createLabelCompletable(labelBody.labelBody)
            .doOnSuccess { label -> contactDao.saveContactGroupLabel(label) }
            .doOnError { throwable ->
                if (throwable is IOException) {
                    PostLabelWorker.Enqueuer(workManager).enqueue(
                        contactLabel.name,
                        contactLabel.color,
                        contactLabel.display,
                        contactLabel.exclusive.toInt(),
                        false,
                        contactLabel.ID
                    )
                }
            }
    }
}
