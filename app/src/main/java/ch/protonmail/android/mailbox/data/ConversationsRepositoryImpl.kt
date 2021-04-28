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
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import javax.inject.Inject

@FlowPreview
class ConversationsRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val api: ProtonMailApiManager
) : ConversationsRepository {

    private val paramFlow = MutableSharedFlow<GetConversationsParameters>(replay = 1)

    override fun getConversations(params: GetConversationsParameters): Flow<DataResult<List<Conversation>>> {
        loadMore(params)

        return paramFlow.transformLatest { parameters ->

            runCatching {
                api.fetchConversations(parameters)
            }.fold(
                onSuccess = {
                    val conversations = it.conversationResponse.toListLocal(parameters.userId.s)
                    conversationDao.insertOrUpdate(*conversations.toTypedArray())
                },
                onFailure = {
                    emit(DataResult.Error.Remote<List<Conversation>>(it.message))
                }
            )

            emitAll(
                getConversationsLocal(parameters).map {
                    DataResult.Success(ResponseSource.Local, it)
                }
            )
        }
    }

    override fun loadMore(params: GetConversationsParameters) {
        paramFlow.tryEmit(params)
    }

    override fun clearConversations() = conversationDao.clear()

    private fun getConversationsLocal(params: GetConversationsParameters): Flow<List<Conversation>> =
        conversationDao.getConversations(params.userId.s).map { list ->
            list.sortedWith(
                compareByDescending<ConversationDatabaseModel> { conversation ->
                    conversation.labels.find { label -> label.id == params.labelId }?.contextTime
                }.thenByDescending { it.order }
            ).toDomainModelList()
        }

}
