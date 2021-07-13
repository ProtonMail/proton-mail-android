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
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.MessageDao
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
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
    private val jobManager: JobManager,
    private val connectivityManager: NetworkConnectivityManager
) {

    fun observeMessage(userId: UserId, messageId: String): Flow<Message?> {
        val messageDao = databaseProvider.provideMessageDao(Id(userId.id))
        return messageDao.findMessageById(messageId).onEach { message ->
            Timber.d("findMessage id: ${message?.messageId}, ${message?.isRead}, ${message?.isDownloaded}")
            message?.messageBody?.let {
                if (it.startsWith(FILE_PREFIX)) {
                    message.messageBody = messageBodyFileManager.readMessageBodyFromFile(message)
                }
            }
        }
    }

    suspend fun getMessage(userId: Id, messageId: String): Message? {
        val messageDao = databaseProvider.provideMessageDao(userId)
        return messageDao.findMessageByIdOnce(messageId)?.apply {
            messageBody?.let {
                if (it.startsWith(FILE_PREFIX)) {
                    messageBody = messageBodyFileManager.readMessageBodyFromFile(this)
                }
            }
        }
    }

    @Deprecated("Use with user Id", ReplaceWith("findMessageOnce(userId, messageId)"))
    suspend fun findMessageById(messageId: String): Message? {
        val currentUser = userManager.currentUserId
        return if (currentUser != null) {
            getMessage(currentUser, messageId)
        } else {
            Timber.w("Cannot find message for null user id")
            null
        }
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
    suspend fun getMessage(
        userId: Id,
        messageId: String,
        shouldFetchMessageDetails: Boolean = false
    ): Message? =
        withContext(dispatcherProvider.Io) {
            val user = userManager.getLegacyUser(userId)
            return@withContext if (user.isGcmDownloadMessageDetails || shouldFetchMessageDetails) {
                getMessageDetails(userId, messageId)
            } else {
                getMessageMetadata(userId, messageId)
            }
        }

    private suspend fun getMessageDetails(userId: Id, messageId: String): Message? =
        withContext(dispatcherProvider.Io) {
            val message = getMessage(userId, messageId)

            if (message?.messageBody != null) {
                return@withContext message
            }

            return@withContext runCatching {
                protonMailApiManager.fetchMessageDetails(messageId, UserIdTag(userId))
            }.map { messageResponse ->
                return@map saveMessage(userId, messageResponse.message)
            }.getOrNull()
        }

    private suspend fun getMessageMetadata(userId: Id, messageId: String): Message? =
        withContext(dispatcherProvider.Io) {
            val message = getMessage(userId, messageId)

            if (message != null) {
                return@withContext message
            }

            return@withContext runCatching {
                protonMailApiManager.fetchMessageMetadata(messageId, UserIdTag(userId))
            }.map { messageResponse ->
                return@map messageResponse.messages.firstOrNull()?.let { message ->
                    return@let saveMessage(userId, message)
                }
            }.getOrNull()
        }

    private suspend fun saveMessage(userId: Id, message: Message): Message =
        withContext(dispatcherProvider.Io) {
            message.apply {
                messageBody = messageBody?.let {
                    return@let if (it.toByteArray().size > MAX_BODY_SIZE_IN_DB) {
                        messageBodyFileManager.saveMessageBodyToFile(this)
                    } else {
                        it
                    }
                }
            }
            val messageDao = databaseProvider.provideMessageDao(userId)
            messageDao.saveMessage(message)
            return@withContext message
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
        Timber.d("markRead $messageIds")
        jobManager.addJobInBackground(PostReadJob(messageIds))
    }

    fun markUnRead(messageIds: List<String>) {
        Timber.d("markUnRead $messageIds")
        jobManager.addJobInBackground(PostUnreadJob(messageIds))
    }

    fun observeMessagesByLocation(
        location: Constants.MessageLocationType,
        userId: Id
    ): Flow<List<Message>> {
        val messagesDao = databaseProvider.provideMessageDao(userId)
        return observeLocationDbDataFlow(location, messagesDao)
            .onStart {
                if (!connectivityManager.isInternetConnectionPossible()) {
                    Timber.d("Skipping network refresh as connectivity is not available")
                    return@onStart
                }

                Timber.v("location: $location, trying fetching from remote")
                runCatching { protonMailApiManager.getMessages(location.messageLocationTypeValue, UserIdTag(userId)) }
                    .fold(
                        onSuccess = { messagesResponse ->
                            if (messagesResponse.code == Constants.RESPONSE_CODE_OK) {
                                persistMessages(messagesResponse.messages, userId, location.messageLocationTypeValue)
                            }
                        },
                        onFailure = { exception ->
                            val dbData = observeLocationDbDataFlow(location, messagesDao).first()
                            emit(dbData)
                            throw exception
                        }
                    )
            }
    }

    private fun observeLocationDbDataFlow(
        location: Constants.MessageLocationType,
        messagesDao: MessageDao
    ) = if (location != Constants.MessageLocationType.STARRED) {
        messagesDao.observeMessagesByLocation(location.messageLocationTypeValue)
    } else {
        messagesDao.observeStarredMessages()
    }

    fun observeMessagesByLabelId(
        labelId: String,
        userId: Id
    ): Flow<List<Message>> {
        val messagesDao = databaseProvider.provideMessageDao(userId)
        return messagesDao.observeMessagesByLabelId(labelId)
            .onStart {
                if (!connectivityManager.isInternetConnectionPossible()) {
                    Timber.d("Skipping network refresh as connectivity is not available")
                    return@onStart
                }

                Timber.v("labelId: $labelId, trying fetching from remote")
                runCatching { protonMailApiManager.searchByLabelAndPage(labelId, 0) }
                    .fold(
                        onSuccess = { labelsResponse ->
                            if (labelsResponse.code == Constants.RESPONSE_CODE_OK) {
                                persistMessages(labelsResponse.messages, userId)
                            }
                        },
                        onFailure = { exception ->
                            val dbData = messagesDao.observeMessagesByLabelId(labelId).first()
                            emit(dbData)
                            throw exception
                        }
                    )
            }
    }

    fun observeAllMessages(
        userId: Id
    ): Flow<List<Message>> {
        val messagesDao = databaseProvider.provideMessageDao(userId)
        return messagesDao.observeAllMessages()
            .onStart {
                // only continue when we have connectivity available
                if (!connectivityManager.isInternetConnectionPossible()) {
                    Timber.d("Skipping network refresh as connectivity is not available")
                    return@onStart
                }

                Timber.v("re-fetching messages all from remote")
                runCatching {
                    protonMailApiManager.getMessages(
                        Constants.MessageLocationType.ALL_MAIL.messageLocationTypeValue,
                        UserIdTag(userId)
                    )
                }
                    .fold(
                        onSuccess = { messagesResponse ->
                            if (messagesResponse.code == Constants.RESPONSE_CODE_OK) {
                                persistMessages(messagesResponse.messages, userId)
                            }
                        },
                        onFailure = { exception ->
                            val dbData = messagesDao.observeAllMessages().first()
                            emit(dbData)
                            throw exception
                        }
                    )
            }
    }

    private suspend fun persistMessages(
        messages: List<Message>,
        userId: Id,
        messageLocationTypeValue: Int? = null
    ) = withContext(dispatcherProvider.Io) {
        val messagesDao = databaseProvider.provideMessageDao(userId)
        messages.forEach { message ->
            if (messageLocationTypeValue != null && messageLocationTypeValue > 0) {
                message.location = messageLocationTypeValue
            }
            message.setFolderLocation(messagesDao)
        }
        messagesDao.saveMessages(messages)
    }
}
