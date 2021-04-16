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
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationsResponse
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import com.dropbox.android.external.store4.ExperimentalStoreApi
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.StoreBuilder
import com.dropbox.android.external.store4.StoreRequest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.data.arch.toDataResult
import me.proton.core.domain.arch.DataResult
import javax.inject.Inject

@FlowPreview
class ConversationsRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val api: ProtonMailApiManager
) : ConversationsRepository {

    private data class StoreKey(val params: GetConversationsParameters, val userId: Id)
    private val store = StoreBuilder.from(
        fetcher = Fetcher.of { key: StoreKey ->
            api.fetchConversations(key.params, key.userId)
        },
        sourceOfTruth = SourceOfTruth.Companion.of(
            reader = { key -> geConversationsLocal(key.params.labelId, key.userId) },
            writer = { key: StoreKey, output: ConversationsResponse ->
                val conversations = output.conversationResponse.toListLocal(key.userId.s)
                conversationDao.insertOrUpdate(*conversations.toTypedArray())
            },
            deleteAll = { conversationDao.clear() }
        )
    ).build()

    private fun geConversationsLocal(labelId: String, userId: Id): Flow<List<Conversation>> =
        conversationDao.getConversations(userId.s).map { list ->
            list.sortedWith(
                compareByDescending<ConversationDatabaseModel> { conversation ->
                    conversation.labels.find { label -> label.id == labelId }?.contextTime
                }.thenByDescending { it.order }
            ).toDomainModelList()
        }

    override fun getConversations(params: GetConversationsParameters, userId: Id):
        Flow<DataResult<List<Conversation>>> =
            store.stream(StoreRequest.cached(StoreKey(params, userId), true)).map { it.toDataResult() }

    @ExperimentalStoreApi
    override suspend fun clearConversations() = store.clearAll()
}
