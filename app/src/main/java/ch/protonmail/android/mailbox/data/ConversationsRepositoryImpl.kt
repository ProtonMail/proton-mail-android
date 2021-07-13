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
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.details.data.remote.model.ConversationResponse
import ch.protonmail.android.details.data.toDomainModelList
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.LabelContextDatabaseModel
import ch.protonmail.android.mailbox.data.remote.worker.DeleteConversationsRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.LabelConversationsRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsReadRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsUnreadRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.UnlabelConversationsRemoteWorker
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.StoreBuilder
import com.dropbox.android.external.store4.StoreRequest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.data.arch.toDataResult
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.DataResult.Error
import me.proton.core.domain.arch.DataResult.Success
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max
import kotlin.time.toDuration

const val NO_MORE_CONVERSATIONS_ERROR_CODE = 723_478
private const val MAX_LOCATION_ID_LENGTH = 2 // For non-custom locations such as: Inbox, Sent, Archive etc.

class ConversationsRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val api: ProtonMailApiManager,
    private val messageFactory: MessageFactory,
    private val markConversationsReadWorker: MarkConversationsReadRemoteWorker.Enqueuer,
    private val markConversationsUnreadWorker: MarkConversationsUnreadRemoteWorker.Enqueuer,
    private val labelConversationsRemoteWorker: LabelConversationsRemoteWorker.Enqueuer,
    private val unlabelConversationsRemoteWorker: UnlabelConversationsRemoteWorker.Enqueuer,
    private val deleteConversationsRemoteWorker: DeleteConversationsRemoteWorker.Enqueuer
) : ConversationsRepository {

    private val paramFlow = MutableSharedFlow<GetConversationsParameters>(replay = 1)

    @FlowPreview
    private val store = StoreBuilder.from(
        fetcher = Fetcher.of { key: ConversationStoreKey ->
            api.fetchConversation(key.conversationId, key.userId)
        },
        sourceOfTruth = SourceOfTruth.Companion.of(
            reader = { key -> observeConversationLocal(key.conversationId, key.userId) },
            writer = { key: ConversationStoreKey, output: ConversationResponse ->
                val messages = output.messages.map(messageFactory::createMessage)
                messageDao.saveMessages(messages)
                Timber.v("Stored new messages size: ${messages.size}")
                val conversation = output.conversation.toLocal(userId = key.userId.s)
                conversationDao.insertOrUpdate(conversation)
                Timber.v("Stored new conversation id: ${conversation.id}")
            },
            delete = { key ->
                conversationDao.deleteConversations(
                    *listOf(key.conversationId).toTypedArray(),
                    userId = key.userId.s
                )
            }
        )
    ).build()

    override fun getConversations(params: GetConversationsParameters): Flow<DataResult<List<Conversation>>> {
        loadMore(params)

        return paramFlow.transformLatest { parameters ->
            Timber.v("New parameters: $parameters")
            runCatching {
                api.fetchConversations(parameters)
            }.fold(
                onSuccess = {
                    val conversations = it.conversationResponse.toListLocal(parameters.userId.s)
                    saveConversations(conversations, parameters.userId)
                    if (it.conversationResponse.isEmpty()) {
                        emit(Error.Remote("No conversations", null, NO_MORE_CONVERSATIONS_ERROR_CODE))
                    }
                },
                onFailure = { throwable ->
                    val dbData = observeConversationsLocal(parameters).first()
                    Timber.i(throwable, "fetchConversations error, local data size: ${dbData.size}")
                    emit(Success(ResponseSource.Local, dbData))
                    delay(1.toDuration(TimeUnit.SECONDS))
                    throw throwable
                }
            )

            emitAll(
                observeConversationsLocal(parameters).map {
                    Success(ResponseSource.Local, it)
                }
            )
        }
    }

    override fun loadMore(params: GetConversationsParameters) {
        paramFlow.tryEmit(params)
    }

    @FlowPreview
    override fun getConversation(
        conversationId: String,
        userId: Id
    ): Flow<DataResult<Conversation>> =
        store.stream(StoreRequest.cached(ConversationStoreKey(conversationId, userId), true))
            .map { it.toDataResult() }
            .onStart { Timber.i("getConversation conversationId: $conversationId") }


    override suspend fun findConversation(conversationId: String, userId: Id): ConversationDatabaseModel? =
        conversationDao.findConversation(conversationId, userId.s)


    override suspend fun saveConversations(conversations: List<ConversationDatabaseModel>, userId: Id) =
        conversationDao.insertOrUpdate(*conversations.toTypedArray())

    override suspend fun deleteConversations(conversationIds: List<String>, userId: Id) {
        conversationDao.deleteConversations(*conversationIds.toTypedArray(), userId = userId.s)
    }

    override suspend fun clearConversations() = conversationDao.clear()

    override suspend fun markRead(conversationIds: List<String>) {
        markConversationsReadWorker.enqueue(conversationIds)

        conversationIds.forEach { conversationId ->
            conversationDao.updateNumUnreadMessages(0, conversationId)
            // All the messages from the conversation are marked as read
            messageDao.observeAllMessagesFromAConversation(conversationId).first().forEach { message ->
                messageDao.saveMessage(message.apply { setIsRead(true) })
            }
        }
    }

    override suspend fun markUnread(
        conversationIds: List<String>,
        userId: UserId,
        location: Constants.MessageLocationType
    ) {
        markConversationsUnreadWorker.enqueue(conversationIds)

        conversationIds.forEach forEachConversation@{ conversationId ->
            val conversation = requireNotNull(conversationDao.observeConversation(conversationId, userId.id).first())
            conversationDao.updateNumUnreadMessages(conversation.numUnread + 1, conversationId)
            // Only the latest message from the current location is marked as unread
            messageDao.observeAllMessagesFromAConversation(conversationId).first().forEach { message ->
                if (Constants.MessageLocationType.fromInt(message.location) == location) {
                    messageDao.saveMessage(message.apply { setIsRead(false) })
                    return@forEachConversation
                }
            }
        }
    }

    override suspend fun star(conversationIds: List<String>, userId: UserId) {
        val starredLabelId = Constants.MessageLocationType.STARRED.messageLocationTypeValue.toString()

        labelConversationsRemoteWorker.enqueue(conversationIds, starredLabelId, userId)

        conversationIds.forEach { conversationId ->
            var lastMessageTime = 0L
            messageDao.observeAllMessagesFromAConversation(conversationId).first().forEach { message ->
                messageDao.updateStarred(message.messageId!!, true)
                lastMessageTime = max(lastMessageTime, message.time)
            }

            addLabelsToConversation(conversationId, userId, listOf(starredLabelId), lastMessageTime)
        }
    }

    override suspend fun unstar(conversationIds: List<String>, userId: UserId) {
        val starredLabelId = Constants.MessageLocationType.STARRED.messageLocationTypeValue.toString()

        unlabelConversationsRemoteWorker.enqueue(conversationIds, starredLabelId, userId)

        conversationIds.forEach { conversationId ->
            removeLabelsFromConversation(conversationId, userId, listOf(starredLabelId))

            messageDao.observeAllMessagesFromAConversation(conversationId).first().forEach { message ->
                messageDao.updateStarred(message.messageId!!, false)
            }
        }
    }

    override suspend fun moveToFolder(
        conversationIds: List<String>,
        userId: UserId,
        folderId: String
    ) {
        labelConversationsRemoteWorker.enqueue(conversationIds, folderId, userId)

        conversationIds.forEach { conversationId ->
            var lastMessageTime = 0L
            messageDao.observeAllMessagesFromAConversation(conversationId).first().forEach { message ->
                val labelsToRemoveFromMessage = getLabelIdsForRemovingWhenMovingToFolder(message.allLabelIDs)
                val labelsToAddToMessage = getLabelIdsForAddingWhenMovingToFolder(folderId, message.allLabelIDs)
                message.removeLabels(labelsToRemoveFromMessage.toList())
                message.addLabels(labelsToAddToMessage.toList())
                messageDao.saveMessage(message)
                lastMessageTime = max(lastMessageTime, message.time)
            }

            val conversation = requireNotNull(conversationDao.observeConversation(conversationId, userId.id).first())
            val labelsToRemoveFromConversation = getLabelIdsForRemovingWhenMovingToFolder(
                conversation.labels.map { it.id }
            )
            removeLabelsFromConversation(conversationId, userId, labelsToRemoveFromConversation)
            addLabelsToConversation(conversationId, userId, listOf(folderId), lastMessageTime)
        }
    }

    override suspend fun delete(
        conversationIds: List<String>,
        userId: UserId,
        currentFolderId: String
    ) {
        deleteConversationsRemoteWorker.enqueue(conversationIds, currentFolderId, userId)

        conversationIds.forEach { conversationId ->
            val messagesFromConversation = messageDao.getAllMessagesFromAConversation(conversationId)
            // The delete action deletes the messages that are in the current mailbox folder
            val messagesToDelete = messagesFromConversation.filter {
                currentFolderId in it.allLabelIDs
            }
            messagesToDelete.forEach { it.deleted = true }
            messageDao.saveMessages(messagesToDelete)

            // If all the messages of the conversation are in the current folder, then delete the conversation
            // Else remove the current location from the conversation's labels list
            if (messagesFromConversation.size == messagesToDelete.size) {
                conversationDao.deleteConversation(conversationId, userId.id)
            } else {
                val conversation = conversationDao.findConversation(conversationId, userId.id)
                if (conversation != null) {
                    val newLabels = conversation.labels.filter { it.id != currentFolderId }
                    conversationDao.updateLabels(newLabels, conversationId)
                }
            }
        }
    }

    override suspend fun label(conversationIds: List<String>, userId: UserId, labelId: String) {
        labelConversationsRemoteWorker.enqueue(conversationIds, labelId, userId)

        conversationIds.forEach { conversationId ->
            var lastMessageTime = 0L
            messageDao.findAllMessageFromAConversation(conversationId).first().forEach { message ->
                message.addLabels(listOf(labelId))
                messageDao.saveMessage(message)
                lastMessageTime = max(lastMessageTime, message.time)
            }

            addLabelsToConversation(conversationId, userId, listOf(labelId), lastMessageTime)
        }
    }

    override suspend fun unlabel(conversationIds: List<String>, userId: UserId, labelId: String) {
        unlabelConversationsRemoteWorker.enqueue(conversationIds, labelId, userId)

        conversationIds.forEach { conversationId ->
            messageDao.findAllMessageFromAConversation(conversationId).first().forEach { message ->
                message.removeLabels(listOf(labelId))
                messageDao.saveMessage(message)
            }

            removeLabelsFromConversation(conversationId, userId, listOf(labelId))
        }
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
                messageDao.findLabelById(labelId)?.exclusive ?: false
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

    private suspend fun addLabelsToConversation(
        conversationId: String,
        userId: UserId,
        labelIds: Collection<String>,
        lastMessageTime: Long
    ) {
        val conversation = requireNotNull(conversationDao.observeConversation(conversationId, userId.id).first())
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
        labels.removeIf { it.id in labelIds }
        labels.addAll(newLabels)
        conversationDao.updateLabels(labels, conversationId)
    }

    private suspend fun removeLabelsFromConversation(
        conversationId: String,
        userId: UserId,
        labelIds: Collection<String>
    ) {
        val conversation = requireNotNull(conversationDao.observeConversation(conversationId, userId.id).first())
        val labels = conversation.labels.toMutableList()
        labels.removeIf { it.id in labelIds }
        conversationDao.updateLabels(labels, conversationId)
    }

    private fun observeConversationsLocal(params: GetConversationsParameters): Flow<List<Conversation>> =
        conversationDao.observeConversations(params.userId.s).map { list ->
            list.sortedWith(
                compareByDescending<ConversationDatabaseModel> { conversation ->
                    conversation.labels.find { label -> label.id == params.locationId }?.contextTime
                }.thenByDescending { it.order }
            ).toDomainModelList()
        }

    private fun observeConversationLocal(conversationId: String, userId: Id): Flow<Conversation?> =
        conversationDao.observeConversation(conversationId, userId.s)
            .map { conversation ->
                if (conversation != null) {
                    val messages = messageDao.getAllMessagesFromAConversation(conversation.id)
                        .onEach { Timber.d("message id: ${it.messageId}, body: ${it.messageBody?.length}") }
                        .toDomainModelList()
                    Timber.d(
                        "Processed conversation id: ${conversation.id} messages size: ${messages.size}"
                    )
                    conversation.toDomainModel(messages)
                } else {
                    null
                }
            }

    private data class ConversationStoreKey(val conversationId: String, val userId: Id)
}
