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

package ch.protonmail.android.repository

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.core.Constants.MAX_MESSAGE_ID_WORKER_ARGUMENTS
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.NoProtonStoreMapper
import ch.protonmail.android.data.ProtonStore
import ch.protonmail.android.data.local.CounterDao
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessagePreferenceEntity
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.jobs.PostReadJob
import ch.protonmail.android.jobs.PostStarJob
import ch.protonmail.android.jobs.PostUnreadJob
import ch.protonmail.android.jobs.PostUnstarJob
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterEntity.Type
import ch.protonmail.android.mailbox.data.mapper.ApiToDatabaseUnreadCounterMapper
import ch.protonmail.android.mailbox.data.mapper.DatabaseToDomainUnreadCounterMapper
import ch.protonmail.android.mailbox.data.mapper.MessagesResponseToMessagesMapper
import ch.protonmail.android.mailbox.data.remote.worker.MoveMessageToLocationWorker
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters.UnreadStatus.ALL
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters.UnreadStatus.READ_ONLY
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters.UnreadStatus.UNREAD_ONLY
import ch.protonmail.android.mailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailbox.domain.model.createBookmarkParametersOr
import ch.protonmail.android.utils.MessageBodyFileManager
import ch.protonmail.android.worker.EmptyFolderRemoteWorker
import com.birbit.android.jobqueue.JobManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.arch.map
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

const val MAX_BODY_SIZE_IN_DB = 900 * 1024 // 900 KB
private const val FILE_PREFIX = "file://"
// For non-custom labels such as: Inbox, Sent, Archive etc.
private const val MAX_LABEL_ID_LENGTH = 2

/**
 * A repository for getting and saving messages.
 */
class MessageRepository @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val databaseProvider: DatabaseProvider,
    private val protonMailApiManager: ProtonMailApiManager,
    private val databaseToDomainUnreadCounterMapper: DatabaseToDomainUnreadCounterMapper,
    private val apiToDatabaseUnreadCounterMapper: ApiToDatabaseUnreadCounterMapper,
    messagesResponseToMessagesMapper: MessagesResponseToMessagesMapper,
    private val messageBodyFileManager: MessageBodyFileManager,
    private val userManager: UserManager,
    private val jobManager: JobManager,
    connectivityManager: NetworkConnectivityManager,
    private val labelRepository: LabelRepository,
    private var moveMessageToLocationWorker: MoveMessageToLocationWorker.Enqueuer,
    private val emptyFolderRemoteWorker: EmptyFolderRemoteWorker.Enqueuer
) {

    private val allMessagesStore by lazy {
        ProtonStore(
            fetcher = protonMailApiManager::getMessages,
            reader = ::observeAllMessagesFromDatabase,
            writer = { params, messages -> saveMessages(params.userId, messages) },
            createBookmarkKey = { currentKey, data -> data.createBookmarkParametersOr(currentKey) },
            apiToDomainMapper = messagesResponseToMessagesMapper,
            databaseToDomainMapper = NoProtonStoreMapper(),
            apiToDatabaseMapper = messagesResponseToMessagesMapper,
            connectivityManager = connectivityManager
        )
    }

    private val refreshUnreadCountersTrigger = MutableSharedFlow<Unit>(replay = 1)

    fun observeMessages(
        params: GetAllMessagesParameters,
        refreshAtStart: Boolean = true
    ): LoadMoreFlow<DataResult<List<Message>>> =
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

    suspend fun getMessage(userId: UserId, messageDatabaseId: Long): Message? {
        val messageDao = databaseProvider.provideMessageDao(userId)
        return messageDao.findMessageByDatabaseId(messageDatabaseId).first()?.apply {
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
            Timber.d("Cannot find message for null user id")
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

            if (message?.messageBody != null && message.isDownloaded) {
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

    fun getUnreadCounters(userId: UserId): Flow<DataResult<List<UnreadCounter>>> =
        refreshUnreadCountersTrigger.flatMapLatest {
            observerUnreadCountersFromDatabase(userId)
                .onStart { fetchAndSaveUnreadCounters(userId) }
                .catch { exception ->
                    if (exception is CancellationException) {
                        throw exception
                    } else {
                        emit(DataResult.Error.Remote(exception.message, exception))
                    }
                }
        }.onStart { refreshUnreadCounters() }

    private suspend fun fetchAndSaveUnreadCounters(userId: UserId) {
        val response = protonMailApiManager.fetchMessagesCounts(userId)
        Timber.v("Fetch Messages Unread response: $response")
        val counts = response.counts
            .map(apiToDatabaseUnreadCounterMapper) { toDatabaseModel(it, userId, Type.MESSAGES) }
        val unreadCounterDao = databaseProvider.provideUnreadCounterDao(userId)
        unreadCounterDao.insertOrUpdate(counts)
    }

    fun refreshUnreadCounters() {
        refreshUnreadCountersTrigger.tryEmit(Unit)
    }

    suspend fun moveToTrash(
        messageIds: List<String>,
        userId: UserId
    ) {
        val newLocation = MessageLocationType.TRASH
        messageIds
            .chunked(MAX_MESSAGE_ID_WORKER_ARGUMENTS)
            .forEach { ids ->
                moveMessageToLocationWorker.enqueue(userId, ids, newLocation = newLocation)
                moveMessageInDb(ids, newLocation, userId)
            }
    }

    suspend fun moveToArchive(
        messageIds: List<String>,
        userId: UserId
    ) {
        val newLocation = MessageLocationType.ARCHIVE
        messageIds
            .chunked(MAX_MESSAGE_ID_WORKER_ARGUMENTS)
            .forEach { ids ->
                moveMessageToLocationWorker.enqueue(userId, ids, newLocation = newLocation)
                moveMessageInDb(ids, newLocation, userId)
            }
    }

    suspend fun moveToInbox(
        messageIds: List<String>,
        userId: UserId
    ) {
        val newLocation = MessageLocationType.INBOX
        messageIds
            .chunked(MAX_MESSAGE_ID_WORKER_ARGUMENTS)
            .forEach { ids ->
                moveMessageToLocationWorker.enqueue(userId, ids, newLocation = newLocation)
                moveMessageInDb(ids, newLocation, userId)
            }
    }

    suspend fun moveToSpam(
        messageIds: List<String>,
        userId: UserId
    ) {
        val newLocation = MessageLocationType.SPAM
        messageIds
            .chunked(MAX_MESSAGE_ID_WORKER_ARGUMENTS)
            .forEach { ids ->
                moveMessageToLocationWorker.enqueue(userId, ids, newLocation = newLocation)
                moveMessageInDb(ids, newLocation, userId)
            }
    }

    suspend fun moveToCustomFolderLocation(
        messageIds: List<String>,
        newCustomLocationId: String,
        userId: UserId
    ) {
        val newLocation = MessageLocationType.LABEL
        messageIds
            .chunked(MAX_MESSAGE_ID_WORKER_ARGUMENTS)
            .forEach { ids ->
                moveMessageToLocationWorker.enqueue(userId, ids, newCustomLocation = newCustomLocationId)
                moveMessageInDb(ids, newLocation, userId, newCustomLocationId)
            }
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

    /**
     * The empty folder action deletes all messages that have a certain label (example: Trash).
     * When performing the action with conversation mode ON, the conversations are not deleted. They are updated
     * in a way that deleted messages are removed from the conversations.
     */
    suspend fun emptyFolder(userId: UserId, labelId: LabelId) {
        emptyFolderRemoteWorker.enqueue(userId, labelId)
        val messageDao = databaseProvider.provideMessageDao(userId)
        messageDao.deleteMessagesByLabel(labelId.id)
    }

    private fun observeAllMessagesFromDatabase(params: GetAllMessagesParameters): Flow<List<Message>> {
        val dao = databaseProvider.provideMessageDao(params.userId)
        val contactDao = databaseProvider.provideContactDao(params.userId)

        val unreadFilter = when (params.unreadStatus) {
            UNREAD_ONLY -> true
            READ_ONLY -> false
            ALL -> null
        }

        return if (params.keyword != null) {
            dao.searchMessages(params.keyword)
        } else {
            requireNotNull(params.labelId) { "Label Id is required" }
            dao.observeMessages(
                params.labelId.id,
                unread = unreadFilter,
                params.sortDirection == GetAllMessagesParameters.SortDirection.DESCENDANT
            )
        }.combineTransform(contactDao.findAllContactsEmails()) { messages, contactEmails ->
            // Makes sure that the correct name of the contact is displayed when showing the messages, because
            //  the sender/recipient name in the message can be outdated if the name of the contact has been changed
            messages.map { message ->
                val sender = requireNotNull(message.sender)
                message.sender = updateSenderWithContactName(sender, contactEmails)

                message.toList = message.toList.map { recipient ->
                    updateRecipientWithContactName(recipient, contactEmails)
                }
                message.ccList = message.ccList.map { recipient ->
                    updateRecipientWithContactName(recipient, contactEmails)
                }
                message.bccList = message.bccList.map { recipient ->
                    updateRecipientWithContactName(recipient, contactEmails)
                }
            }
            emit(messages)
        }
    }

    private fun updateSenderWithContactName(
        sender: MessageSender,
        contactEmails: List<ContactEmail>
    ): MessageSender {
        val senderContactName = contactEmails.find { it.email == sender.emailAddress }?.name
        return MessageSender(
            if (!senderContactName.isNullOrEmpty()) senderContactName else sender.name,
            sender.emailAddress,
            sender.isProton
        )
    }

    private fun updateRecipientWithContactName(
        recipient: MessageRecipient,
        contactEmails: List<ContactEmail>
    ): MessageRecipient {
        val recipientContactName = contactEmails.find { it.email == recipient.emailAddress }?.name
        return MessageRecipient(
            if (!recipientContactName.isNullOrEmpty()) recipientContactName else recipient.name,
            recipient.emailAddress,
            recipient.group
        )
    }

    private suspend fun saveMessages(userId: UserId, messages: List<Message>) {
        withContext(dispatcherProvider.Io) {
            val messagesDao = databaseProvider.provideMessageDao(userId)
            messages.forEach { message ->
                message.saveBodyToFileIfNeeded()
                message.setFolderLocation(labelRepository)
            }
            messagesDao.saveMessages(messages)
        }
    }

    suspend fun saveMessage(userId: UserId, message: Message): Message {
        message.saveBodyToFileIfNeeded()
        val messageDao = databaseProvider.provideMessageDao(userId)
        messageDao.saveMessage(message)
        return message
    }

    suspend fun deleteMessagesInDb(userId: UserId, messageIds: List<String>) {
        val messageDao = databaseProvider.provideMessageDao(userId)

        messageDao.deleteAttachmentsByMessageIds(messageIds)
        messageDao.deleteMessagesByIds(messageIds)
    }

    suspend fun saveViewInDarkModeMessagePreference(userId: UserId, messageId: String, viewInDarkMode: Boolean) {
        val messagePreferenceDao = databaseProvider.provideMessagePreferenceDao(userId)
        val messagePreference = messagePreferenceDao.findMessagePreference(messageId)
            ?.copy(viewInDarkMode = viewInDarkMode)
            ?: MessagePreferenceEntity(messageId = messageId, viewInDarkMode = viewInDarkMode)
        messagePreferenceDao.saveMessagePreference(messagePreference)
    }

    suspend fun getViewInDarkModeMessagePreference(userId: UserId, messageId: String): Boolean? {
        val messagePreferenceDao = databaseProvider.provideMessagePreferenceDao(userId)
        return messagePreferenceDao.findMessagePreference(messageId)?.viewInDarkMode
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

    private fun observerUnreadCountersFromDatabase(userId: UserId): Flow<DataResult<List<UnreadCounter>>> =
        databaseProvider
            .provideUnreadCounterDao(userId)
            .observeMessagesUnreadCounters(userId).map { list ->
                val domainModels = databaseToDomainUnreadCounterMapper.toDomainModels(list)
                DataResult.Success(ResponseSource.Local, domainModels)
            }

    private suspend fun moveMessageInDb(
        messageIds: List<String>,
        newLocation: MessageLocationType,
        userId: UserId,
        newCustomLocationId: String? = null // for custom folder locations
    ) {
        val counterDao = databaseProvider.provideCounterDao(userId)
        val messagesDao = databaseProvider.provideMessageDao(userId)
        var totalUnread = 0
        for (id in messageIds) {
            yield()
            val message: Message? = messagesDao.findMessageByIdOnce(id)
            if (message != null) {
                Timber.d("Move to $newLocation, message: %s", message.messageId)
                if (updateMessageLocally(
                        counterDao,
                        messagesDao,
                        message,
                        newLocation,
                        newCustomLocationId
                    )
                ) {
                    totalUnread++
                }
            }
        }

        val unreadLocationCounter = counterDao.findUnreadLocationById(newLocation.messageLocationTypeValue) ?: return
        unreadLocationCounter.increment(totalUnread)
        counterDao.insertUnreadLocation(unreadLocationCounter)
    }

    private suspend fun updateMessageLocally(
        counterDao: CounterDao,
        messagesDao: MessageDao,
        message: Message,
        newLocation: MessageLocationType,
        newCustomLocationId: String? = null
    ): Boolean {
        var unreadIncrease = false
        if (!message.isRead) {
            val unreadLocationCounter = counterDao.findUnreadLocationById(message.location)
            if (unreadLocationCounter != null) {
                unreadLocationCounter.decrement()
                counterDao.insertUnreadLocation(unreadLocationCounter)
            }
            unreadIncrease = true
        }
        val newLocationString = if (!newCustomLocationId.isNullOrEmpty()) {
            newCustomLocationId
        } else {
            newLocation.messageLocationTypeValue.toString()
        }
        message.removeLabels(
            getLabelIdsToRemoveOnMoveToFolderAction(
                labelIds = message.allLabelIDs,
                isTrashAction = newLocation == MessageLocationType.TRASH,
                message.isScheduled
            )
        )
        message.addLabels(listOf(newLocationString))
        Timber.d(
            "Archive message id: %s, location: %s, labels: %s", message.messageId, message.location, message.allLabelIDs
        )

        messagesDao.saveMessage(message)
        return unreadIncrease
    }

    /**
     * Filter out the non-exclusive labels and locations like: ALL_DRAFT, ALL_SENT, ALL_MAIL, that shouldn't be
     * removed when moving a message to folder.
     */
    private suspend fun getLabelIdsToRemoveOnMoveToFolderAction(
        labelIds: List<String>,
        isTrashAction: Boolean,
        isScheduled: Boolean
    ): List<String> {
        return labelIds.filter { labelId ->
            val isLabelExclusive = if (labelId.length > MAX_LABEL_ID_LENGTH) {
                labelRepository.findLabel(LabelId(labelId))?.type == LabelType.FOLDER
            } else {
                labelId != MessageLocationType.STARRED.asLabelIdString()
            }

            if (isScheduled) return@filter labelId !in arrayOf(
                MessageLocationType.ALL_DRAFT.asLabelIdString(),
                MessageLocationType.ALL_MAIL.asLabelIdString()
            )

            if (isTrashAction) return@filter labelId !in arrayOf(
                MessageLocationType.ALL_DRAFT.asLabelIdString(),
                MessageLocationType.ALL_SENT.asLabelIdString(),
                MessageLocationType.ALL_MAIL.asLabelIdString()
            )

            return@filter isLabelExclusive &&
                labelId !in arrayOf(
                MessageLocationType.ALL_DRAFT.asLabelIdString(),
                MessageLocationType.ALL_SENT.asLabelIdString(),
                MessageLocationType.ALL_MAIL.asLabelIdString()
            )
        }
    }

    fun observeMessagesCountByLocationFromDatabase(userId: UserId, location: String): Flow<Int> =
        databaseProvider
            .provideMessageDao(userId)
            .observeMessagesCountByLocation(location)
}
