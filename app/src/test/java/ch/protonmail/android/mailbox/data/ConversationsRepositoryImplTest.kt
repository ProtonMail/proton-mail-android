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
import ch.protonmail.android.core.Constants
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationsResponse
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.model.Parameters
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ConversationsRepositoryImplTest : CoroutinesTest, ArchTest {

    private val testUserId = Id("id")

    private val conversationsRemote = ConversationsResponse(

        total = 5,
        listOf(
            ConversationApiModel(
                id = "conversation5",
                order = 4,
                subject = "subject5"
            ),
            ConversationApiModel(
                id = "conversation4",
                order = 3,
                subject = "subject4"
            ),
            ConversationApiModel(
                id = "conversation3",
                order = 2,
                subject = "subject3"
            ),
            ConversationApiModel(
                id = "conversation2",
                order = 1,
                subject = "subject2"
            ),
            ConversationApiModel(
                id = "conversation1",
                order = 0,
                subject = "subject1"
            )
        )
    )

    private val conversationsOrdered =
        listOf(
            Conversation(
                id = "conversation5",
                subject = "subject5",
                listOf(),
                listOf(),
                0,
                0,
                0,
                0
            ),
            Conversation(
                id = "conversation4",
                subject = "subject4",
                listOf(),
                listOf(),
                0,
                0,
                0,
                0
            ),
            Conversation(
                id = "conversation3",
                subject = "subject3",
                listOf(),
                listOf(),
                0,
                0,
                0,
                0
            ),
            Conversation(
                id = "conversation2",
                subject = "subject2",
                listOf(),
                listOf(),
                0,
                0,
                0,
                0
            ),
            Conversation(
                id = "conversation1",
                subject = "subject1",
                listOf(),
                listOf(),
                0,
                0,
                0,
                0
            )
        )

    @MockK
    private lateinit var conversationDao: ConversationDao

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var conversationsRepository: ConversationsRepositoryImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        conversationsRepository =
            ConversationsRepositoryImpl(
                conversationDao,
                api
            )
    }

    @Test
    fun verifyConversationsIsFetchedFromLocalInitially() {
        runBlockingTest {
            // given
            val parameters = Parameters.GetConversationsParameters(
                userId = testUserId,
                location = Constants.MessageLocationType.INBOX,
                page = 0,
                pageSize = 2
            )
            coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(listOf())

            // when
            val result = conversationsRepository.getConversations(parameters).first()

            // then
            assertEquals(DataResult.Success(ResponseSource.Local, listOf()), result)
        }
    }


    @Test
    fun verifyConversationsAreRetrievedCorrectly() =
        runBlockingTest {

            // given
            val parameters = Parameters.GetConversationsParameters(
                userId = testUserId,
                location = Constants.MessageLocationType.INBOX,
                page = 0,
                pageSize = 5
            )

            val conversationsEntity = conversationsRemote.conversationResponse.toListLocal(testUserId.s)
            coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(conversationsEntity)

            // when
            val result = conversationsRepository.getConversations(parameters).first()

            // then
            assertEquals(DataResult.Success(ResponseSource.Local, conversationsOrdered), result)

        }

//    @Test
//    fun verifyGetConversationsTriesToFetchDataFromRemote() =
//        runBlockingTest {
//
//            // given
//            val parameters = Parameters.GetConversationsParameters(
//                userId = testUserId,
//                location = Constants.MessageLocationType.INBOX,
//                page = 0,
//                pageSize = 5
//            )
//
//            coEvery { conversationDao.getConversations(testUserId.s) } returns flowOf(emptyList())
//            coEvery { conversationDao.insertOrUpdate(any()) } returns Unit
//            coEvery { api.fetchConversations(any()) } returns conversationsRemote
//
//            // when
//            conversationsRepository.getConversations(parameters).test {
//
//                expectItem()
//                expectItem()
//                expectItem()
//
//                // then
////            coVerify(exactly = 1) { conversationDao.getConversations(testUserId.s)}
////            coVerify(exactly = 1) { conversationDao.insertOrUpdate(*emptyList<ConversationEntity>().toTypedArray()) }
//            }
//        }

}
