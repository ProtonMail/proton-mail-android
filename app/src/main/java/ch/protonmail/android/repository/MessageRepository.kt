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
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.NoProtonStoreMapper
import ch.protonmail.android.data.ProtonStore
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.domain.loadMoreCatch
import ch.protonmail.android.domain.loadMoreCombineTransform
import ch.protonmail.android.domain.loadMoreEmitInitialNull
import ch.protonmail.android.domain.loadMoreFlow
import ch.protonmail.android.jobs.MoveToFolderJob
import ch.protonmail.android.jobs.PostArchiveJob
import ch.protonmail.android.jobs.PostInboxJob
import ch.protonmail.android.jobs.PostReadJob
import ch.protonmail.android.jobs.PostSpamJob
import ch.protonmail.android.jobs.PostStarJob
import ch.protonmail.android.jobs.PostTrashJobV2
import ch.protonmail.android.jobs.PostUnreadJob
import ch.protonmail.android.jobs.PostUnstarJob
import ch.protonmail.android.mailbox.data.mapper.MessagesResponseToMessagesMapper
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import ch.protonmail.android.mailbox.domain.model.createBookmarkParametersOr
import ch.protonmail.android.utils.MessageBodyFileManager
import com.birbit.android.jobqueue.JobManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.DataResult.Error
import me.proton.core.domain.arch.DataResult.Processing
import me.proton.core.domain.arch.DataResult.Success
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.unsupported
import timber.log.Timber
import javax.inject.Inject

const val MAX_BODY_SIZE_IN_DB = 900 * 1024 // 900 KB
const val NO_MORE_MESSAGES_ERROR_CODE = 235_894
private const val FILE_PREFIX = "file://"

/**
 * A repository for getting and saving messages.
 */
class MessageRepository @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val databaseProvider: DatabaseProvider,
    private val protonMailApiManager: ProtonMailApiManager,
    messagesResponseToMessagesMapper: MessagesResponseToMessagesMapper,
    private val messageBodyFileManager: MessageBodyFileManager,
    private val userManager: UserManager,
    private val jobManager: JobManager,
    private val connectivityManager: NetworkConnectivityManager
) {

    private val allMessagesStore by lazy {
        ProtonStore(
            fetcher = protonMailApiManager::getMessages,
            reader = ::observeAllMessagesFromDatabase,
            writer = { params, messages -> saveMessages(params.userId, messages) },
            createBookmarkKey = { currentKey, data -> data.createBookmarkParametersOr(currentKey) },
            apiToDomainMapper = messagesResponseToMessagesMapper,
            databaseToDomainMapper = NoProtonStoreMapper(),
            apiToDatabaseMapper = messagesResponseToMessagesMapper
        )
    }

    fun observeMessages(params: GetAllMessagesParameters, refreshAtStart: Boolean = true) =
        allMessagesStore.loadMoreFlow(params, refreshAtStart)

    fun observeMessage(userId: UserId, messageId: String): Flow<Message?> {
        val messageDao = databaseProvider.provideMessageDao(userId)
        return messageDao.findMessageById(messageId).onEach { message ->
            Timber.d("findMessage id: ${message?.messageId}, ${message?.isRead}, ${message?.isDownloaded}")
            message?.messageBody?.let {
                if (it.startsWith(FILE_PREFIX)) {
                    message.messageBody = messageBodyFileManager.readMessageBodyFromFile(message)
                }
            }
        }
    }

    suspend fun findMessage(userId: UserId, messageId: String): Message? {
        val messageDao = databaseProvider.provideMessageDao(userId)
        return messageDao.findMessageByIdOnce(messageId)?.apply {
            messageBody?.let {
                if (it.startsWith(FILE_PREFIX)) {
                    messageBody = messageBodyFileManager.readMessageBodyFromFile(this)
                }
            }
        }
    }

    @Deprecated("Use with user Id", ReplaceWith("findMessage(userId, messageId)"))
    suspend fun findMessageById(messageId: String): Message? {
        val currentUser = userManager.currentUserId
        return if (currentUser != null) {
            findMessage(currentUser, messageId)
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
     * @param userId User id for which the message is being retrieved. It is used to check whether the
     *        auto-download-messages setting is turned on or not.
     * @param shouldFetchMessageDetails An optional parameter that should indicate whether message details
     *        should be fetched, even if the user settings say otherwise. Example: When a message is being open.
     * @return An instance of Message
     */
    suspend fun getMessage(
        userId: UserId,
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

    private suspend fun getMessageDetails(userId: UserId, messageId: String): Message? =
        withContext(dispatcherProvider.Io) {
            val message = findMessage(userId, messageId)

            if (message?.messageBody != null) {
                return@withContext message
            }

            return@withContext runCatching {
                protonMailApiManager.fetchMessageDetails(messageId, UserIdTag(userId))
            }.map { messageResponse ->
                return@map saveMessage(userId, messageResponse.message)
            }.getOrNull()
        }

    private suspend fun getMessageMetadata(userId: UserId, messageId: String): Message? =
        withContext(dispatcherProvider.Io) {
            val message = findMessage(userId, messageId)

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

    fun moveToTrash(messageIds: List<String>, currentFolderLabelId: String) {
        jobManager.addJobInBackground(
            PostTrashJobV2(
                messageIds,
                listOf(currentFolderLabelId),
                currentFolderLabelId
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
        userId: UserId,
        location: MessageLocationType
    ): LoadMoreFlow<DataResult<List<Message>>> = allMessagesStore.loadMoreFlow(
        GetAllMessagesParameters(
            userId = userId,
            labelId = location.asLabelId()
        ),
        refreshAtStart = true
    )

    fun observeMessagesByLabelId(
        userId: UserId,
        labelId: String
    ): LoadMoreFlow<DataResult<List<Message>>> {
        val messagesDao = databaseProvider.provideMessageDao(userId)
        val fromDatabaseFlow = messagesDao.observeMessagesByLabelId(labelId)
        return observeMessagesByLocationOrLabelId(userId, labelId, fromDatabaseFlow)
    }

    private fun observeMessagesByLocationOrLabelId(
        userId: UserId,
        locationId: String,
        databaseFlow: Flow<List<Message>>
    ): LoadMoreFlow<DataResult<List<Message>>> {
        val fromDatabaseFlow = databaseFlow
            .map { Success(ResponseSource.Local, it) }

        val parameters = GetAllMessagesParameters(userId, labelId = locationId)
        val fromApiFlow = getMessageByLocationIdFromApi(parameters)
            // Emit a null at start, in order emit data from Database, without waiting for first emission from Api
            .loadMoreEmitInitialNull()

        var lastSaved: Success<List<Message>>? = null
        val saveMessages: suspend (Success<List<Message>>) -> Unit = { result ->
            val areDifferentMessages = result.value.map { it.messageId } != lastSaved?.value?.map { it.messageId }
            if (areDifferentMessages) saveMessages(userId, result.value)
            lastSaved = result
        }

        return loadMoreCombineTransform(fromDatabaseFlow, fromApiFlow) { fromDatabase, fromApi ->
            when (fromApi) {
                is Success -> saveMessages(fromApi)
                is Error -> if (connectivityManager.isInternetConnectionPossible()) emit(fromApi)
                null, is Processing -> {
                    // noop
                }
            }

            emit(fromDatabase)
        }
    }

    private fun observeMessagesByLocationFromDatabase(
        location: MessageLocationType,
        messagesDao: MessageDao
    ) = when (location) {
        MessageLocationType.ALL_MAIL -> messagesDao.observeAllMessages()
        MessageLocationType.STARRED -> messagesDao.observeStarredMessages()
        else -> messagesDao.observeMessagesByLocation(location.messageLocationTypeValue)
    }

    private fun getMessageByLocationIdFromApi(initialParams: GetAllMessagesParameters) =
        loadMoreFlow(
            initialBookmark = initialParams,
            createNextBookmark = { dataResult, currentParameters ->
                dataResult.createBookmarkParametersOr(currentParameters)
            },
            load = { params ->
                Timber.v("Fetching messages from API. Params = $params")
                val response = protonMailApiManager.getMessages(params).messages

                Timber.v("Fetched ${response.size} messages from API")
                if (response.isNotEmpty()) {
                    Success(ResponseSource.Remote, response)
                } else {
                    Error.Remote("No messages", null, NO_MORE_MESSAGES_ERROR_CODE)
                }
            }
        ).loadMoreCatch { throwable ->
            if (throwable is CancellationException) throw throwable
            Timber.e(throwable, "Cannot fetch messages from API")
            emit(Error.Remote(throwable.message, throwable))
        }

    private fun observeAllMessagesFromDatabase(params: GetAllMessagesParameters): Flow<List<Message>> {
        val dao = databaseProvider.provideMessageDao(params.userId)

        // We threat Sent as a Label, since when we send a message to ourself it should be in both Sent and Inbox, but
        //  it can have only one location, which is Inbox
        fun sentAsLabelId() =
            MessageLocationType.SENT.asLabelId()

        fun starredAsLabelId() =
            MessageLocationType.STARRED.asLabelId()

        fun allMailAsLabelId() =
            MessageLocationType.ALL_MAIL.asLabelId()

        fun locationTypesAlLabelId() =
            MessageLocationType.values().map { it.asLabelId() }

        return if (params.keyword != null) {
            dao.searchMessages(params.keyword)
        } else {
            when (params.labelId) {
                sentAsLabelId() -> dao.observeMessagesByLabelId(params.labelId)
                allMailAsLabelId() -> dao.observeAllMessages()
                starredAsLabelId() -> dao.observeStarredMessages()
                null -> unsupported
                in locationTypesAlLabelId() -> dao.observeMessagesByLocation(params.labelId.toInt())
                else -> dao.observeMessagesByLabelId(params.labelId)
            }
        }
    }

    private suspend fun saveMessages(userId: UserId, messages: List<Message>) {
        withContext(dispatcherProvider.Io) {
            val messagesDao = databaseProvider.provideMessageDao(userId)
            messages.forEach { message ->
                message.saveBodyToFileIfNeeded()
                message.setFolderLocation(messagesDao)
            }
            messagesDao.saveMessages(messages)
        }
    }

    private suspend fun saveMessage(userId: UserId, message: Message): Message {
        message.saveBodyToFileIfNeeded()
        val messageDao = databaseProvider.provideMessageDao(userId)
        messageDao.saveMessage(message)
        return message
    }

    private suspend fun Message.saveBodyToFileIfNeeded() {
        withContext(dispatcherProvider.Io) {
            messageBody = messageBody?.let {
                return@let if (it.toByteArray().size > MAX_BODY_SIZE_IN_DB) {
                    messageBodyFileManager.saveMessageBodyToFile(this@saveBodyToFileIfNeeded)
                } else {
                    it
                }
            }
        }
    }
}
