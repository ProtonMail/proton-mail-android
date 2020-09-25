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
import androidx.work.WorkInfo
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.worker.DeleteLabelWorker
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

    suspend operator fun invoke(labelId: String): LiveData<List<Boolean>> =
        withContext(dispatchers.Io) {
            val contactLabel = contactsDatabase.findContactGroupById(labelId)
            contactLabel?.let {
                contactsDatabase.deleteContactGroup(it)
            }
            Timber.v("Delete DB labels ")
            messagesDatabase.deleteLabelById(labelId)
            workerScheduler.enqueue(labelId)

            workerScheduler.getWorkStatusLiveData()
                .map { mapStateToBoolean(it) }
        }

//    suspend operator fun invoke(labelId: List<String>): LiveData<List<Boolean>> =
//        withContext(dispatchers.Io) {
//            val contactLabel = contactsDatabase.findContactGroupById(labelId)
//            contactLabel?.let {
//                contactsDatabase.deleteContactGroup(it)
//            }
//            messagesDatabase.deleteLabelById(labelId)
//            workerScheduler.enqueue(labelId)
//
//            return@withContext workerScheduler.getWorkStatusLiveData()
//                .map { mapStateToBoolean(it) }
//        }

    private fun mapStateToBoolean(infoList: List<WorkInfo>): List<Boolean> =
        infoList.map { it.state }
            .filter { it.isFinished }
            .map { it == WorkInfo.State.SUCCEEDED }
}
