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

package ch.protonmail.android.usecase.delete

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.utils.extensions.reduceWorkInfoToBoolean
import ch.protonmail.android.worker.DeleteLabelWorker
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * Use case responsible for removing a label from the DB and scheduling
 * [DeleteLabelWorker] that will send a deferrable delete label network request.
 */
class DeleteLabel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val contactsDatabase: ContactsDatabase,
    @Named("messages") private val messagesDatabase: MessagesDatabase,
    private val workerScheduler: DeleteLabelWorker.Enqueuer
) {

    suspend operator fun invoke(labelIds: List<String>): LiveData<Boolean> =
        withContext(dispatchers.Io) {

            // delete labels in DB
            labelIds.forEach { labelId ->
                ensureActive()

                val contactLabel = contactsDatabase.findContactGroupById(labelId)
                contactLabel?.let { label ->
                    Timber.v("Delete DB contact group $label")
                    contactsDatabase.deleteContactGroup(label)
                }
                Timber.v("Delete DB label $labelId")
                messagesDatabase.deleteLabelById(labelId)
            }

            // schedule worker to remove label ids over the network
            workerScheduler.enqueue(labelIds)
            return@withContext workerScheduler.getWorkStatusLiveData()
                .map { it.reduceWorkInfoToBoolean() }
        }
}
