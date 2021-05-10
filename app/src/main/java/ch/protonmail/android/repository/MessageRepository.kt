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

package ch.protonmail.android.repository

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.jobs.MoveToFolderJob
import ch.protonmail.android.jobs.PostArchiveJob
import ch.protonmail.android.jobs.PostInboxJob
import ch.protonmail.android.jobs.PostReadJob
import ch.protonmail.android.jobs.PostSpamJob
import ch.protonmail.android.jobs.PostStarJob
import ch.protonmail.android.jobs.PostTrashJobV2
import ch.protonmail.android.jobs.PostUnreadJob
import ch.protonmail.android.jobs.PostUnstarJob
import ch.protonmail.android.utils.MessageBodyFileManager
import com.birbit.android.jobqueue.JobManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

const val MAX_BODY_SIZE_IN_DB = 900 * 1024 // 900 KB

private const val FILE_PREFIX = "file://"

/**
 * A repository for getting and saving messages.
 */

class MessageRepository @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val databaseProvider: DatabaseProvider,
    private val protonMailApiManager: ProtonMailApiManager,
    private val messageBodyFileManager: MessageBodyFileManager,
    private val userManager: UserManager,
    private val jobManager: JobManager
) {

    private suspend fun findMessage(userId: Id, messageId: String): Message? =
        withContext(dispatcherProvider.Io) {
            val messageDao = databaseProvider.provideMessageDao(userId)
            return@withContext messageDao.findMessageById(messageId).first()?.apply {
                messageBody?.let {
                    if (it.startsWith(FILE_PREFIX))
                        messageBody = messageBodyFileManager.readMessageBodyFromFile(this)
                }
            }
        }

    suspend fun findMessageById(messageId: String): Message? {
        val currentUser = userManager.currentUserId
        return if (currentUser != null) {
            findMessage(currentUser, messageId)
        } else {
            Timber.w("Cannot find message for null user id")
            null
        }
    }

    private suspend fun saveMessage(userId: Id, message: Message): Long =
        withContext(dispatcherProvider.Io) {
            val messageToBeSaved = message.apply {
                messageBody = messageBody?.let {
                    return@let if (it.toByteArray().size > MAX_BODY_SIZE_IN_DB) {
                        messageBodyFileManager.saveMessageBodyToFile(this)
                    } else {
                        null
                    }
                }
            }
            val messageDao = databaseProvider.provideMessageDao(userId)
            return@withContext messageDao.saveMessage(messageToBeSaved)
        }

    private suspend fun getMessageDetails(userId: Id, messageId: String): Message? =
        withContext(dispatcherProvider.Io) {
            val message = findMessage(userId, messageId)

            if (message?.messageBody != null) {
                return@withContext message
            }

            return@withContext runCatching {
                protonMailApiManager.fetchMessageDetails(messageId, UserIdTag(userId))
            }.map { messageResponse ->
                saveMessage(userId, messageResponse.message)
                return@map messageResponse.message
            }.getOrNull()
        }

    private suspend fun getMessageMetadata(userId: Id, messageId: String): Message? =
        withContext(dispatcherProvider.Io) {
            val message = findMessage(userId, messageId)

            if (message != null) {
                return@withContext message
            }

            return@withContext runCatching {
                protonMailApiManager.fetchMessageMetadata(messageId, UserIdTag(userId))
            }.map { messageResponse ->
                return@map messageResponse.messages.firstOrNull()?.let { message ->
                    saveMessage(userId, message)
                    return@let message
                }
            }.getOrNull()
        }

    /**
     * Returns an instance of Message with the given message id. Returns the message from the database
     * if it exists there. Otherwise, fetches it.
     *
     * @param messageId A message id that should be used to get a message
     * @param username Username for which the message is being retrieved. It is used to check whether the
     *        auto-download-messages setting is turned on or not.
     * @param shouldFetchMessageDetails An optional parameter that should indicate whether message details
     *        should be fetched, even if the user settings say otherwise. Example: When a message is being open.
     * @return An instance of Message
     */
    suspend fun getMessage(userId: Id, messageId: String, shouldFetchMessageDetails: Boolean = false): Message? =
        withContext(dispatcherProvider.Io) {
            val user = userManager.getLegacyUser(userId)
            return@withContext if (user.isGcmDownloadMessageDetails || shouldFetchMessageDetails) {
                getMessageDetails(userId, messageId)
            } else {
                getMessageMetadata(userId, messageId)
            }
        }

    fun moveToTrash(messageIds: List<String>, currentFolderLabelId: String) {
        jobManager.addJobInBackground(
            PostTrashJobV2(
                messageIds,
                listOf(currentFolderLabelId),
                currentFolderLabelId // TODO: Think why is this parameter needed here?
            )
        )
    }

    fun moveToArchive(messageIds: List<String>, currentFolderLabelId: String) {
        jobManager.addJobInBackground(
            PostArchiveJob(messageIds, listOf(currentFolderLabelId))
        )
    }

    fun moveToInbox(messageIds: List<String>, currentFolderLabelId: String) {
        jobManager.addJobInBackground(
            PostInboxJob(messageIds, listOf(currentFolderLabelId))
        )
    }

    fun moveToSpam(messageIds: List<String>) {
        jobManager.addJobInBackground(
            PostSpamJob(messageIds)
        )
    }

    fun moveToCustomFolderLocation(messageIds: List<String>, newFolderLocationId: String) {
        jobManager.addJobInBackground(
            MoveToFolderJob(messageIds, newFolderLocationId)
        )
    }

    fun starMessages(messageIds: List<String>) {
        jobManager.addJobInBackground(PostStarJob(messageIds))
    }

    fun unStarMessages(messageIds: List<String>) {
        jobManager.addJobInBackground(PostUnstarJob(messageIds))
    }

    fun markRead(messageIds: List<String>) {
        jobManager.addJobInBackground(PostReadJob(messageIds))
    }

    fun markUnRead(messageIds: List<String>) {
        jobManager.addJobInBackground(PostUnreadJob(messageIds))
    }

}
