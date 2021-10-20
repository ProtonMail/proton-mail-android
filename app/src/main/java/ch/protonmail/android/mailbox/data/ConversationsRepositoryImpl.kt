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
package ch.protonmail.android.mailbox.data

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.data.ProtonStore
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.data.remote.model.ConversationResponse
import ch.protonmail.android.details.data.toDomainModelList
import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.labels.data.remote.worker.LabelConversationsRemoteWorker
import ch.protonmail.android.labels.data.remote.worker.UnlabelConversationsRemoteWorker
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.UnreadCounterDao
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.LabelContextDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterEntity
import ch.protonmail.android.mailbox.data.mapper.ApiToDatabaseUnreadCounterMapper
import ch.protonmail.android.mailbox.data.mapper.ConversationApiModelToConversationDatabaseModelMapper
import ch.protonmail.android.mailbox.data.mapper.ConversationDatabaseModelToConversationMapper
import ch.protonmail.android.mailbox.data.mapper.ConversationsResponseToConversationsDatabaseModelsMapper
import ch.protonmail.android.mailbox.data.mapper.ConversationsResponseToConversationsMapper
import ch.protonmail.android.mailbox.data.mapper.DatabaseToDomainUnreadCounterMapper
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import ch.protonmail.android.mailbox.data.remote.worker.DeleteConversationsRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsReadRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsUnreadRemoteWorker
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import ch.protonmail.android.mailbox.domain.model.GetAllConversationsParameters
import ch.protonmail.android.mailbox.domain.model.GetOneConversationParameters
import ch.protonmail.android.mailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailbox.domain.model.createBookmarkParametersOr
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.StoreBuilder
import com.dropbox.android.external.store4.StoreRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.yield
import me.proton.core.data.arch.toDataResult
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.DataResult.Error
import me.proton.core.domain.arch.DataResult.Success
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.arch.map
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max

// For non-custom locations such as: Inbox, Sent, Archive etc.
private const val MAX_LOCATION_ID_LENGTH = 2

internal class ConversationsRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val labelsRepository: LabelRepository,
    private val unreadCounterDao: UnreadCounterDao,
    private val api: ProtonMailApiManager,
    responseToConversationsMapper: ConversationsResponseToConversationsMapper,
    private val databaseToConversationMapper: ConversationDatabaseModelToConversationMapper,
    private val apiToDatabaseConversationMapper: ConversationApiModelToConversationDatabaseModelMapper,
    responseToDatabaseConversationsMapper: ConversationsResponseToConversationsDatabaseModelsMapper,
    private val messageFactory: MessageFactory,
    private val databaseToDomainUnreadCounterMapper: DatabaseToDomainUnreadCounterMapper,
    private val apiToDatabaseUnreadCounterMapper: ApiToDatabaseUnreadCounterMapper,
    private val markConversationsReadWorker: MarkConversationsReadRemoteWorker.Enqueuer,
    private val markConversationsUnreadWorker: MarkConversationsUnreadRemoteWorker.Enqueuer,
    private val labelConversationsRemoteWorker: LabelConversationsRemoteWorker.Enqueuer,
    private val unlabelConversationsRemoteWorker: UnlabelConversationsRemoteWorker.Enqueuer,
    private val deleteConversationsRemoteWorker: DeleteConversationsRemoteWorker.Enqueuer,
    private val markUnreadLatestNonDraftMessageInLocation: MarkUnreadLatestNonDraftMessageInLocation,
    connectivityManager: NetworkConnectivityManager
) : ConversationsRepository {

    private val refreshUnreadCountersTrigger = MutableSharedFlow<Unit>(replay = 1)

    private val allConversationsStore by lazy {
        ProtonStore(
            fetcher = api::fetchConversations,
            reader = ::observeAllConversationsFromDatabase,
            writer = { params, conversations -> saveConversationsDatabaseModels(params.userId, conversations) },
            createBookmarkKey = { currentKey, data -> data.createBookmarkParametersOr(currentKey) },
            apiToDomainMapper = responseToConversationsMapper,
            databaseToDomainMapper = databaseToConversationMapper,
            apiToDatabaseMapper = responseToDatabaseConversationsMapper,
            connectivityManager = connectivityManager
        )
    }

    private val oneConversationStore by lazy {
        StoreBuilder.from(
            fetcher = Fetcher.of(api::fetchConversation),
            sourceOfTruth = SourceOfTruth.of(
                reader = ::observeConversationFromDatabase,
                writer = { params: GetOneConversationParameters, output: ConversationResponse ->
                    val messages = output.messages.map(messageFactory::createMessage)
                    messageDao.saveMessages(messages)
                    Timber.v("Stored new messages size: ${messages.size}")
                    val conversation =
                        apiToDatabaseConversationMapper.toDatabaseModel(output.conversation, params.userId)
                    conversationDao.insertOrUpdate(conversation)
                    Timber.v("Stored new conversation id: ${conversation.id}")
                },
                delete = { params ->
                    conversationDao.deleteConversation(params.userId.id, params.conversationId)
                }
            )
        ).build()
    }

    override fun observeConversations(
        params: GetAllConversationsParameters,
        refreshAtStart: Boolean,
    ): LoadMoreFlow<DataResult<List<Conversation>>> =
        allConversationsStore.loadMoreFlow(params, refreshAtStart)

    override fun getConversation(
        userId: UserId,
        conversationId: String
    ): Flow<DataResult<Conversation>> =
        oneConversationStore.stream(StoreRequest.cached(GetOneConversationParameters(userId, conversationId), true))
            .map { it.toDataResult() }
            .onStart { Timber.i("getConversation conversationId: $conversationId") }

    override fun getUnreadCounters(userId: UserId): Flow<DataResult<List<UnreadCounter>>> =
        refreshUnreadCountersTrigger.flatMapLatest {
            observeUnreadCountersFromDatabase(userId)
                .onStart { fetchAndSaveUnreadCounters(userId) }
        }
            .onStart { refreshUnreadCounters() }
            .catch { exception ->
                if (exception is CancellationException) {
                    throw exception
                } else {
                    emit(Error.Remote(exception.message, exception))
                }
            }

    override fun refreshUnreadCounters() {
        refreshUnreadCountersTrigger.tryEmit(Unit)
    }

    override suspend fun saveConversationsDatabaseModels(
        userId: UserId,
        conversations: List<ConversationDatabaseModel>
    ) {
        conversationDao.insertOrUpdate(*conversations.toTypedArray())
    }

    override suspend fun saveConversationsApiModels(
        userId: UserId,
        conversations: List<ConversationApiModel>
    ) {
        val databaseModels = apiToDatabaseConversationMapper.toDatabaseModels(conversations, userId)
        conversationDao.insertOrUpdate(*databaseModels.toTypedArray())
    }

    override suspend fun deleteConversations(conversationIds: List<String>, userId: UserId) {
        conversationDao.deleteConversations(userId = userId.id, *conversationIds.toTypedArray())
    }

    override suspend fun clearConversations() = conversationDao.clear()

    override suspend fun markRead(
        conversationIds: List<String>,
        userId: UserId
    ): ConversationsActionResult {
        markConversationsReadWorker.enqueue(conversationIds, userId)

        conversationIds.forEach { conversationId ->
            conversationDao.updateNumUnreadMessages(conversationId, 0)
            // All the messages from the conversation are marked as read
            getAllConversationMessagesSortedByNewest(conversationId).forEach { message ->
                yield()
                messageDao.saveMessage(message.apply { setIsRead(true) })
            }
        }

        return ConversationsActionResult.Success
    }

    override suspend fun markUnread(
        conversationIds: List<String>,
        userId: UserId,
        locationId: String
    ): ConversationsActionResult {
        markConversationsUnreadWorker.enqueue(conversationIds, locationId, userId)

        conversationIds.forEach forEachConversation@{ conversationId ->
            val conversation = conversationDao.findConversation(userId.id, conversationId)
            if (conversation == null) {
                Timber.e("Conversation with id $conversationId could not be found in DB")
                return ConversationsActionResult.Error
            }
            conversationDao.updateNumUnreadMessages(conversationId, conversation.numUnread + 1)
            val conversationMessages = getAllConversationMessagesSortedByNewest(conversationId)
            markUnreadLatestNonDraftMessageInLocation(conversationMessages, locationId, userId)
        }

        return ConversationsActionResult.Success
    }

    override suspend fun star(
        conversationIds: List<String>,
        userId: UserId
    ): ConversationsActionResult {
        val starredLabelId = Constants.MessageLocationType.STARRED.messageLocationTypeValue.toString()

        labelConversationsRemoteWorker.enqueue(conversationIds, starredLabelId, userId)

        conversationIds.forEach { conversationId ->
            Timber.v("Star conversation $conversationId")
            var lastMessageTime = 0L
            val starredMessages = getAllConversationMessagesSortedByNewest(conversationId).map { message ->
                yield()
                message.addLabels(listOf(starredLabelId))
                message.isStarred = true
                lastMessageTime = max(lastMessageTime, message.time)
                return@map message
            }
            messageDao.saveMessages(starredMessages)

            val result = addLabelsToConversation(conversationId, userId, listOf(starredLabelId), lastMessageTime)
            if (result is ConversationsActionResult.Error) {
                return result
            }
        }

        return ConversationsActionResult.Success
    }

    override suspend fun unstar(
        conversationIds: List<String>,
        userId: UserId
    ): ConversationsActionResult {
        val starredLabelId = Constants.MessageLocationType.STARRED.messageLocationTypeValue.toString()

        unlabelConversationsRemoteWorker.enqueue(conversationIds, starredLabelId, userId)

        conversationIds.forEach { conversationId ->
            Timber.v("UnStar conversation $conversationId")
            val unstarredMessages = getAllConversationMessagesSortedByNewest(conversationId).map { message ->
                yield()
                message.removeLabels(listOf(starredLabelId))
                message.isStarred = false
                return@map message
            }
            messageDao.saveMessages(unstarredMessages)

            val result = removeLabelsFromConversation(conversationId, userId, listOf(starredLabelId))
            if (result is ConversationsActionResult.Error) {
                return result
            }
        }

        return ConversationsActionResult.Success
    }

    override suspend fun moveToFolder(
        conversationIds: List<String>,
        userId: UserId,
        folderId: String
    ): ConversationsActionResult {
        labelConversationsRemoteWorker.enqueue(conversationIds, folderId, userId)

        conversationIds.forEach { conversationId ->
            Timber.v("Move conversation $conversationId to folder: $folderId")
            var lastMessageTime = 0L
            val messagesToUpdate = getAllConversationMessagesSortedByNewest(conversationId).map { message ->
                yield()
                val labelsToAddToMessage = getLabelIdsForAddingWhenMovingToFolder(folderId, message.allLabelIDs)
                val labelsToRemoveFromMessage = getLabelIdsForRemovingWhenMovingToFolder(message.allLabelIDs)
                message.addLabels(labelsToAddToMessage.toList())
                message.removeLabels(labelsToRemoveFromMessage.toList())
                Timber.v("Remove labels $labelsToRemoveFromMessage, add labels: $labelsToAddToMessage")
                lastMessageTime = max(lastMessageTime, message.time)
                message
            }
            // save all updated messages from a conversation in one go
            messageDao.saveMessages(messagesToUpdate)

            val conversation = conversationDao.findConversation(userId.id, conversationId)
            if (conversation == null) {
                Timber.e("Conversation with id $conversationId could not be found in DB")
                return ConversationsActionResult.Error
            }
            val labelsToRemoveFromConversation = getLabelIdsForRemovingWhenMovingToFolder(
                conversation.labels.map { it.id }
            )
            val removeLabelsResult = removeLabelsFromConversation(
                conversationId,
                userId,
                labelsToRemoveFromConversation
            )
            val addLabelsResult = addLabelsToConversation(
                conversationId,
                userId,
                listOf(folderId),
                lastMessageTime
            )
            if (
                removeLabelsResult is ConversationsActionResult.Error ||
                addLabelsResult is ConversationsActionResult.Error
            ) {
                return ConversationsActionResult.Error
            }
        }

        return ConversationsActionResult.Success
    }

    override suspend fun delete(
        conversationIds: List<String>,
        userId: UserId,
        currentFolderId: String
    ) {
        deleteConversationsRemoteWorker.enqueue(conversationIds, currentFolderId, userId)

        conversationIds.forEach { conversationId ->
            val messagesFromConversation = getAllConversationMessagesSortedByNewest(conversationId)
            // The delete action deletes the messages that are in the current mailbox folder
            val messagesToDelete = messagesFromConversation.filter {
                currentFolderId in it.allLabelIDs
            }
            messagesToDelete.forEach { it.deleted = true }
            messageDao.saveMessages(messagesToDelete)

            // If all the messages of the conversation are in the current folder, then delete the conversation
            // Else remove the current location from the conversation's labels list
            if (messagesFromConversation.size == messagesToDelete.size) {
                conversationDao.deleteConversation(userId.id, conversationId)
            } else {
                val conversation = conversationDao.findConversation(userId.id, conversationId)
                if (conversation != null) {
                    val newLabels = conversation.labels.filter { it.id != currentFolderId }
                    conversationDao.updateLabels(conversationId, newLabels)
                }
            }
        }
    }

    override suspend fun label(
        conversationIds: List<String>,
        userId: UserId,
        labelId: String
    ): ConversationsActionResult {
        labelConversationsRemoteWorker.enqueue(conversationIds, labelId, userId)

        conversationIds.forEach { conversationId ->
            var lastMessageTime = 0L
            val labeledMessages = messageDao.findAllConversationMessagesSortedByNewest(conversationId).map { message ->
                yield()
                message.addLabels(listOf(labelId))
                lastMessageTime = max(lastMessageTime, message.time)
                return@map message
            }
            messageDao.saveMessages(labeledMessages)

            val result = addLabelsToConversation(conversationId, userId, listOf(labelId), lastMessageTime)
            if (result is ConversationsActionResult.Error) {
                return result
            }
        }

        return ConversationsActionResult.Success
    }

    override suspend fun unlabel(
        conversationIds: List<String>,
        userId: UserId,
        labelId: String
    ): ConversationsActionResult {
        unlabelConversationsRemoteWorker.enqueue(conversationIds, labelId, userId)

        conversationIds.forEach { conversationId ->
            val unlabeledMessages = messageDao.findAllConversationMessagesSortedByNewest(conversationId).map { message ->
                yield()
                message.removeLabels(listOf(labelId))
                return@map message
            }
            messageDao.saveMessages(unlabeledMessages)

            val result = removeLabelsFromConversation(conversationId, userId, listOf(labelId))
            if (result is ConversationsActionResult.Error) {
                return result
            }
        }

        return ConversationsActionResult.Success
    }

    private fun observeAllConversationsFromDatabase(
        params: GetAllConversationsParameters
    ): Flow<List<ConversationDatabaseModel>> {

        val labelIdFilter = { conversation: ConversationDatabaseModel ->
            params.labelId != null && params.labelId in conversation.labels.map { it.id }
        }

        val contextTimeComparator = compareByDescending { conversation: ConversationDatabaseModel ->
            conversation.labels.find { label -> label.id == params.labelId }?.contextTime
        }

        return conversationDao.observeConversations(params.userId.id).map { list ->
            Timber.d("Conversations update size: ${list.size}, params: $params")
            list
                .filter(labelIdFilter)
                .sortedWith(contextTimeComparator.thenByDescending { it.order })
        }
    }

    private suspend fun getAllConversationMessagesSortedByNewest(conversationId: String): List<Message> =
        messageDao.findAllConversationMessagesSortedByNewest(conversationId).onEach { message ->
            message.attachments = message.attachments(messageDao)
        }

    /**
     * When we move a conversation to Inbox, the destination folder is not always the only destination
     * that needs to be added to the list of label ids. When the conversation contains messages that are
     * drafts or sent messages, the DRAFT and SENT locations need to be added to the list of label ids as well.
     */
    private fun getLabelIdsForAddingWhenMovingToFolder(
        destinationFolderId: String,
        labelIds: Collection<String>
    ): Collection<String> {
        val labelsToBeAdded = mutableListOf(destinationFolderId)
        if (destinationFolderId == Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString()) {
            if (labelIds.contains(Constants.MessageLocationType.ALL_SENT.messageLocationTypeValue.toString())) {
                labelsToBeAdded.add(Constants.MessageLocationType.SENT.messageLocationTypeValue.toString())
            }
            if (labelIds.contains(Constants.MessageLocationType.ALL_DRAFT.messageLocationTypeValue.toString())) {
                labelsToBeAdded.add(Constants.MessageLocationType.DRAFT.messageLocationTypeValue.toString())
            }
        }
        return labelsToBeAdded
    }

    /**
     * Filter out the non-exclusive labels and locations like: ALL_DRAFT, ALL_SENT, ALL_MAIL, that shouldn't be
     * removed when moving a conversation to folder.
     */
    private suspend fun getLabelIdsForRemovingWhenMovingToFolder(labelIds: Collection<String>): Collection<String> {
        return labelIds.filter { labelId ->
            val isLabelExclusive = if (labelId.length > MAX_LOCATION_ID_LENGTH) {
                labelsRepository.findLabel(LabelId(labelId))?.type == LabelType.FOLDER
            } else {
                true
            }

            return@filter isLabelExclusive &&
                labelId !in arrayOf(
                Constants.MessageLocationType.ALL_DRAFT.messageLocationTypeValue.toString(),
                Constants.MessageLocationType.ALL_SENT.messageLocationTypeValue.toString(),
                Constants.MessageLocationType.ALL_MAIL.messageLocationTypeValue.toString(),
                Constants.MessageLocationType.STARRED.messageLocationTypeValue.toString()
            )
        }
    }

    private suspend fun fetchAndSaveUnreadCounters(userId: UserId) {
        val counts = api.fetchConversationsCounts(userId).counts
            .map(apiToDatabaseUnreadCounterMapper) {
                toDatabaseModel(
                    it, userId, UnreadCounterEntity.Type.CONVERSATIONS
                )
            }
        unreadCounterDao.insertOrUpdate(counts)
    }

    private suspend fun addLabelsToConversation(
        conversationId: String,
        userId: UserId,
        labelIds: Collection<String>,
        lastMessageTime: Long
    ): ConversationsActionResult {
        val conversation = conversationDao.findConversation(userId.id, conversationId)
        if (conversation == null) {
            Timber.e("Conversation with id $conversationId could not be found in DB")
            return ConversationsActionResult.Error
        }
        val newLabels = mutableListOf<LabelContextDatabaseModel>()
        labelIds.forEach { labelId ->
            val newLabel = LabelContextDatabaseModel(
                labelId,
                conversation.numUnread,
                conversation.numMessages,
                lastMessageTime,
                conversation.size.toInt(),
                conversation.numAttachments
            )
            newLabels.add(newLabel)
        }
        val labels = conversation.labels.toMutableList()

        labels.removeAll { it.id in labelIds }
        labels.addAll(newLabels)
        Timber.v("Update labels: $labels conversation: $conversationId")
        conversationDao.updateLabels(conversationId, labels)
        return ConversationsActionResult.Success
    }

    private suspend fun removeLabelsFromConversation(
        conversationId: String,
        userId: UserId,
        labelIds: Collection<String>
    ): ConversationsActionResult {
        val conversation = conversationDao.findConversation(userId.id, conversationId)
        if (conversation == null) {
            Timber.e("Conversation with id $conversationId could not be found in DB")
            return ConversationsActionResult.Error
        }
        val labels = conversation.labels.toMutableList()
        labels.removeAll { it.id in labelIds }
        conversationDao.updateLabels(conversationId, labels)
        return ConversationsActionResult.Success
    }

    private fun observeConversationFromDatabase(params: GetOneConversationParameters): Flow<Conversation?> =
        conversationDao.observeConversation(params.userId.id, params.conversationId).combine(
            messageDao.observeAllMessagesInfoFromConversation(params.conversationId)
        ) { conversation, messages ->
            conversation?.let {
                databaseToConversationMapper.toDomainModel(conversation, messages.toDomainModelList())
            }
        }.distinctUntilChanged()


    private fun observeUnreadCountersFromDatabase(userId: UserId): Flow<DataResult<List<UnreadCounter>>> =
        unreadCounterDao.observeConversationsUnreadCounters(userId).map { list ->
            val domainModels = databaseToDomainUnreadCounterMapper.toDomainModels(list)
            Success(ResponseSource.Local, domainModels)
        }

}
