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

import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import ch.protonmail.android.mailbox.domain.model.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import javax.inject.Inject

class FakeConversationsRepository @Inject constructor() : ConversationsRepository {

    override fun getConversations(
        params: GetConversationsParameters,
        userId: Id
    ): Flow<DataResult<List<Conversation>>> {
        val sender = Correspondent("senderName", "sender@protonmail.com")
        val recipients = listOf(
            Correspondent("recipient", "recipient@protonmail.com"),
            Correspondent("recipient1", "recipient1@pm.ch")
        )
        val correspondent = Correspondent("conversation sender", "bomber@pm.me")
        val anotherCorrespondent = Correspondent("Ronaldo", "anotherSender@protonmail.com")
        val yetAnotherCorrespondent = Correspondent("Messi", "yetanotherfootballplayer@protonmail.com")
        return flowOf(
            DataResult.Success(
                ResponseSource.Local,
                listOf(
                    Conversation(
                        "conversationId",
                        "A Fake conversation for you with a long subject that we expect to be truncated",
                        listOf(correspondent, anotherCorrespondent, yetAnotherCorrespondent),
                        recipients,
                        1,
                        1,
                        2,
                        0,
                        "senderAddressId",
                        listOf(),
                        listOf(
                            MessageEntity(
                                "messageId",
                                "conversationId",
                                "subject",
                                true,
                                sender,
                                recipients,
                                123421L,
                                0,
                                0,
                                false,
                                false,
                                true,
                                emptyList(),
                                emptyList(),
                                ""
                            )
                        ),
                        1617205075000
                    ),
                    Conversation(
                        "conversationId1",
                        "Another fake conversation",
                        listOf(anotherCorrespondent),
                        recipients,
                        10,
                        2,
                        5,
                        23492348,
                        "senderAddress923942834",
                        listOf("10", "Mylabel82384", "82374"),
                        listOf(
                            MessageEntity(
                                "messageId",
                                "conversationId",
                                "subject",
                                true,
                                sender,
                                recipients,
                                123421L,
                                0,
                                0,
                                false,
                                false,
                                true,
                                emptyList(),
                                emptyList(),
                                ""
                            )
                        ),
                        1718906095000
                    )
                )
            )
        )
    }

    override suspend fun getConversation(conversationId: String, messageId: String): Conversation? {
        TODO("Not yet implemented")
    }

}
