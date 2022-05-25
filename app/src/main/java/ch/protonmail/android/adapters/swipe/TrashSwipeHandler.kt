/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.adapters.swipe

import ch.protonmail.android.api.models.SimpleMessage
import ch.protonmail.android.core.Constants
import ch.protonmail.android.jobs.MoveToFolderJob
import ch.protonmail.android.jobs.PostArchiveJob
import ch.protonmail.android.jobs.PostDraftJob
import ch.protonmail.android.jobs.PostInboxJob
import ch.protonmail.android.jobs.PostTrashJobV2
import com.birbit.android.jobqueue.Job

class TrashSwipeHandler : ISwipeHandler {

    override fun handleSwipe(
        message: SimpleMessage,
        currentLocation: String
    ): Job =
        PostTrashJobV2(listOf(message.messageId), currentLocation)

    override fun handleUndo(
        message: SimpleMessage,
        messageLocation: Constants.MessageLocationType,
        currentLocation: String
    ): Job = when (messageLocation) {
        Constants.MessageLocationType.INBOX -> PostInboxJob(listOf(message.messageId))
        Constants.MessageLocationType.ARCHIVE -> PostArchiveJob(listOf(message.messageId))
        Constants.MessageLocationType.LABEL_FOLDER -> MoveToFolderJob(
            listOf(message.messageId), currentLocation
        )
        else -> PostDraftJob(listOf(message.messageId))
    }
}
