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
import ch.protonmail.android.api.models.messages.receive.AttachmentFactory
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.messages.receive.MessageSenderFactory
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.details.data.remote.model.ConversationResponse
import ch.protonmail.android.details.data.toDomainModelList
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import ch.protonmail.android.mailbox.domain.model.MessageDomainModel
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.StoreBuilder
import com.dropbox.android.external.store4.StoreRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.data.arch.toDataResult
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.DataResult.Error
import me.proton.core.domain.arch.DataResult.Success
import me.proton.core.domain.arch.ResponseSource
import javax.inject.Inject

const val NO_MORE_CONVERSATIONS_ERROR_CODE = 723478

class ConversationsRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val api: ProtonMailApiManager
) : ConversationsRepository {

    private val paramFlow = MutableSharedFlow<GetConversationsParameters>(replay = 1)

    @FlowPreview
    private val store = StoreBuilder.from(
        fetcher = Fetcher.of { key: ConversationStoreKey ->
            api.fetchConversation(key.conversationId, key.userId)
        },
        sourceOfTruth = SourceOfTruth.Companion.of(
            reader = { key -> getConversationLocal(key.conversationId, key.userId, key.scope) },
            writer = { key: ConversationStoreKey, output: ConversationResponse ->
                val conversation = output.conversation.toLocal(userId = key.userId.s)
                conversationDao.insertOrUpdate(conversation)
                val messages by lazy {
                    val attachmentFactory = AttachmentFactory()
                    val messageSenderFactory = MessageSenderFactory()
                    val messageFactory = MessageFactory(attachmentFactory, messageSenderFactory)

                    output.messages.map(messageFactory::createMessage) ?: emptyList()
                }
                messageDao.saveMessages(*messages.toTypedArray())
            },
            delete = { key -> conversationDao.deleteConversation(key.conversationId, key.userId.s) }
        )
    ).build()


    override fun getConversations(params: GetConversationsParameters): Flow<DataResult<List<Conversation>>> {
        loadMore(params)

        return paramFlow.transformLatest { parameters ->

            runCatching {
                api.fetchConversations(parameters)
            }.fold(
                onSuccess = {
                    val conversations = it.conversationResponse.toListLocal(parameters.userId.s)
                    conversationDao.insertOrUpdate(*conversations.toTypedArray())
                    if (conversations.isEmpty()) {
                        emit(Error.Remote("No conversations", null, NO_MORE_CONVERSATIONS_ERROR_CODE))
                    }
                },
                onFailure = {
                    emit(Error.Remote(it.message, it))
                }
            )

            emitAll(
                getConversationsLocal(parameters).map {
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
        userId: Id,
        scope: CoroutineScope
    ): Flow<DataResult<Conversation>> =
        store.stream(StoreRequest.cached(ConversationStoreKey(conversationId, userId, scope), true))
            .map { it.toDataResult() }

    override fun clearConversations() = conversationDao.clear()


    private fun getConversationsLocal(params: GetConversationsParameters): Flow<List<Conversation>> =
        conversationDao.getConversations(params.userId.s).map { list ->
            list.sortedWith(
                compareByDescending<ConversationDatabaseModel> { conversation ->
                    conversation.labels.find { label -> label.id == params.locationId }?.contextTime
                }.thenByDescending { it.order }
            ).toDomainModelList()
        }

    private fun getConversationLocal(conversationId: String, userId: Id, scope: CoroutineScope): Flow<Conversation> {
        var messages = listOf<MessageDomainModel>()
        getMessagesForConversation(conversationId).onEach { list ->
            messages = list
        }.launchIn(scope)
        return conversationDao.getConversation(conversationId, userId.s).map { conversation ->
            conversation.toDomainModel(messages)
        }
    }

    private fun getMessagesForConversation(conversationId: String): Flow<List<MessageDomainModel>> =
        messageDao.findAllMessageFromAConversation(conversationId).map { list ->
            list.toDomainModelList()
        }

    private data class ConversationStoreKey(val conversationId: String, val userId: Id, val scope: CoroutineScope)

}
