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
import ch.protonmail.android.data.local.ContactsDao
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.utils.extensions.filter
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
    private val contactsDao: ContactsDao,
    @Named("messages") private val messageDao: MessageDao,
    private val deleteLabelWorker: DeleteLabelWorker.Enqueuer
) {

    suspend operator fun invoke(labelIds: List<String>): LiveData<Boolean> =
        withContext(dispatchers.Io) {

            // delete labels in DB
            labelIds.forEach { labelId ->
                ensureActive()

                val contactLabel = contactsDao.findContactGroupById(labelId)
                contactLabel?.let { label ->
                    Timber.v("Delete DB contact group $label")
                    contactsDao.deleteContactGroup(label)
                }
                Timber.v("Delete DB label $labelId")
                messageDao.deleteLabelById(labelId)
            }

            // schedule worker to remove label ids over the network
            deleteLabelWorker.enqueue(labelIds)
                .filter { it?.state?.isFinished == true }
                .map { workInfo ->
                    Timber.v("Finished worker State ${workInfo.state}")
                    workInfo.state == WorkInfo.State.SUCCEEDED
                }
        }
}
