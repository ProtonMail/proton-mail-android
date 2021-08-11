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
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.data.remote.model.ConversationResponse
import ch.protonmail.android.details.data.toDomainModelList
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.LabelContextDatabaseModel
import ch.protonmail.android.mailbox.data.remote.worker.DeleteConversationsRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.LabelConversationsRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsReadRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.MarkConversationsUnreadRemoteWorker
import ch.protonmail.android.mailbox.data.remote.worker.UnlabelConversationsRemoteWorker
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.StoreBuilder
import com.dropbox.android.external.store4.StoreRequest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.yield
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
                val conversation = output.conversation.toLocal(userId = key.userId.id)
                conversationDao.insertOrUpdate(conversation)
                Timber.v("Stored new conversation id: ${conversation.id}")
            },
            delete = { key ->
                conversationDao.deleteConversations(
                    *listOf(key.conversationId).toTypedArray(),
                    userId = key.userId.id
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
                    val conversations = it.conversationResponse.toListLocal(parameters.userId.id)
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
        userId: UserId
    ): Flow<DataResult<Conversation>> =
        store.stream(StoreRequest.cached(ConversationStoreKey(conversationId, userId), true))
            .map { it.toDataResult() }
            .onStart { Timber.i("getConversation conversationId: $conversationId") }


    override suspend fun findConversation(conversationId: String, userId: UserId): ConversationDatabaseModel? =
        conversationDao.findConversation(conversationId, userId.id)


    override suspend fun saveConversations(
        conversations: List<ConversationDatabaseModel>,
        userId: UserId
    ) =
        conversationDao.insertOrUpdate(*conversations.toTypedArray())

    override suspend fun deleteConversations(conversationIds: List<String>, userId: UserId) {
        conversationDao.deleteConversations(*conversationIds.toTypedArray(), userId = userId.id)
    }

    override suspend fun clearConversations() = conversationDao.clear()

    override suspend fun markRead(
        conversationIds: List<String>,
        userId: UserId
    ): ConversationsActionResult {
        markConversationsReadWorker.enqueue(conversationIds, userId)

        conversationIds.forEach { conversationId ->
            conversationDao.updateNumUnreadMessages(0, conversationId)
            // All the messages from the conversation are marked as read
            getAllMessagesFromAConversation(conversationId).forEach { message ->
                yield()
                messageDao.saveMessage(message.apply { setIsRead(true) })
            }
        }

        return ConversationsActionResult.Success
    }

    override suspend fun markUnread(
        conversationIds: List<String>,
        userId: UserId,
        location: Constants.MessageLocationType,
        locationId: String
    ): ConversationsActionResult {
        markConversationsUnreadWorker.enqueue(conversationIds, locationId, userId)

        conversationIds.forEach forEachConversation@{ conversationId ->
            val conversation = conversationDao.findConversation(conversationId, userId.id)
            if (conversation == null) {
                Timber.e("Conversation with id $conversationId could not be found in DB")
                return ConversationsActionResult.Error
            }
            conversationDao.updateNumUnreadMessages(conversation.numUnread + 1, conversationId)

            // Only the latest unread message from the current location is marked as unread
            getAllMessagesFromAConversation(conversationId).forEach { message ->
                yield()
                if (message.isRead) {
                    Timber.v("Make message ${message.messageId} read")
                    messageDao.saveMessage(message.apply { setIsRead(false) })
                    return@forEachConversation
                }
            }
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
            getAllMessagesFromAConversation(conversationId).forEach { message ->
                yield()
                messageDao.updateStarred(requireNotNull(message.messageId), true)
                lastMessageTime = max(lastMessageTime, message.time)
            }

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
            val result = removeLabelsFromConversation(conversationId, userId, listOf(starredLabelId))
            if (result is ConversationsActionResult.Error) {
                return result
            }

            getAllMessagesFromAConversation(conversationId).forEach { message ->
                yield()
                messageDao.updateStarred(message.messageId!!, false)
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
            val messagesToUpdate = getAllMessagesFromAConversation(conversationId).map { message ->
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

            val conversation = conversationDao.findConversation(conversationId, userId.id)
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
            val messagesFromConversation = getAllMessagesFromAConversation(conversationId)
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

    override suspend fun label(
        conversationIds: List<String>,
        userId: UserId,
        labelId: String
    ): ConversationsActionResult {
        labelConversationsRemoteWorker.enqueue(conversationIds, labelId, userId)

        conversationIds.forEach { conversationId ->
            var lastMessageTime = 0L
            messageDao.findAllMessagesInfoFromConversation(conversationId).forEach { message ->
                yield()
                message.addLabels(listOf(labelId))
                messageDao.saveMessage(message)
                lastMessageTime = max(lastMessageTime, message.time)
            }

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
            messageDao.findAllMessagesInfoFromConversation(conversationId).forEach { message ->
                yield()
                message.removeLabels(listOf(labelId))
                messageDao.saveMessage(message)
            }

            val result = removeLabelsFromConversation(conversationId, userId, listOf(labelId))
            if (result is ConversationsActionResult.Error) {
                return result
            }
        }

        return ConversationsActionResult.Success
    }

    private suspend fun getAllMessagesFromAConversation(conversationId: String): List<Message> =
        messageDao.findAllMessagesInfoFromConversation(conversationId).onEach { message ->
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
    ): ConversationsActionResult {
        val conversation = conversationDao.findConversation(conversationId, userId.id)
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
        labels.removeIf { it.id in labelIds }
        labels.addAll(newLabels)
        Timber.v("Update labels: $labels conversation: $conversationId")
        conversationDao.updateLabels(labels, conversationId)
        return ConversationsActionResult.Success
    }

    private suspend fun removeLabelsFromConversation(
        conversationId: String,
        userId: UserId,
        labelIds: Collection<String>
    ): ConversationsActionResult {
        val conversation = conversationDao.findConversation(conversationId, userId.id)
        if (conversation == null) {
            Timber.e("Conversation with id $conversationId could not be found in DB")
            return ConversationsActionResult.Error
        }
        val labels = conversation.labels.toMutableList()
        labels.removeIf { it.id in labelIds }
        conversationDao.updateLabels(labels, conversationId)
        return ConversationsActionResult.Success
    }

    private fun observeConversationsLocal(params: GetConversationsParameters): Flow<List<Conversation>> =
        conversationDao.observeConversations(params.userId.id).map { list ->
            Timber.d("Conversations update size: ${list.size}, params: $params")
            list.sortedWith(
                compareByDescending<ConversationDatabaseModel> { conversation ->
                    conversation.labels.find { label -> label.id == params.locationId }?.contextTime
                }.thenByDescending { it.order }
            ).toDomainModelList()
        }

    private fun observeConversationLocal(conversationId: String, userId: UserId): Flow<Conversation?> =
        conversationDao.observeConversation(conversationId, userId.id).combine(
            messageDao.observeAllMessagesInfoFromConversation(conversationId)
        ) { conversation, messages ->
            conversation?.toDomainModel(messages.toDomainModelList())
        }.distinctUntilChanged()

    private data class ConversationStoreKey(val conversationId: String, val userId: UserId)
}
