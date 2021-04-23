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

package ch.protonmail.android.activities.messageDetails.labelactions.domain

import ch.protonmail.android.jobs.MoveToFolderJob
import ch.protonmail.android.jobs.PostArchiveJob
import ch.protonmail.android.jobs.PostInboxJob
import ch.protonmail.android.jobs.PostSpamJob
import ch.protonmail.android.jobs.PostTrashJobV2
import com.birbit.android.jobqueue.JobManager
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class MoveMessagesToFolder @Inject constructor(
    private val jobManager: JobManager
) {

    operator fun invoke(
        messageIds: List<String>,
        newFolderLocation: NewFolderLocation,
        currentFolderLabelId: String = EMPTY_STRING,
    ) {
        val job = when (newFolderLocation) {
            is NewFolderLocation.Trash -> {
                PostTrashJobV2(
                    messageIds,
                    listOf(currentFolderLabelId),
                    currentFolderLabelId // TODO: Think why is that all needed here?
                )
            }
            is NewFolderLocation.Archive -> {
                PostArchiveJob(messageIds, listOf(currentFolderLabelId))
            }
            is NewFolderLocation.Inbox -> {
                PostInboxJob(messageIds, listOf(currentFolderLabelId))
            }
            is NewFolderLocation.Spam -> {
                PostSpamJob(messageIds)
            }
            is NewFolderLocation.CustomFolder -> {
                MoveToFolderJob(messageIds, newFolderLocation.folderId)
            }
        }
        jobManager.addJobInBackground(job)
    }

    // based on Constants.MessageLocationType
    sealed class NewFolderLocation {

        object Archive : NewFolderLocation()
        object Inbox : NewFolderLocation()
        object Spam : NewFolderLocation()
        object Trash : NewFolderLocation()
        data class CustomFolder(val folderId: String) : NewFolderLocation()
    }
}
