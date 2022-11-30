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

package ch.protonmail.android.mailbox.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class UnreadCounterDaoTest {

    // region Test data
    private val oneUserId = UserId("one")
    private val twoUserId = UserId("two")
    private val inboxLabelId = "inbox"
    private val sentLabelId = "sent"

    private val oneMessagesInboxCounter = UnreadCounterEntity(
        userId = oneUserId,
        type = UnreadCounterEntity.Type.MESSAGES,
        labelId = inboxLabelId,
        unreadCount = Random.nextInt()
    )
    private val oneMessagesSendCounter = UnreadCounterEntity(
        userId = oneUserId,
        type = UnreadCounterEntity.Type.MESSAGES,
        labelId = sentLabelId,
        unreadCount = Random.nextInt()
    )
    private val oneConversationsInboxCounter = UnreadCounterEntity(
        userId = oneUserId,
        type = UnreadCounterEntity.Type.CONVERSATIONS,
        labelId = inboxLabelId,
        unreadCount = Random.nextInt()
    )
    private val twoMessagesInboxCounter = UnreadCounterEntity(
        userId = twoUserId,
        type = UnreadCounterEntity.Type.MESSAGES,
        labelId = inboxLabelId,
        unreadCount = Random.nextInt()
    )
    // endregion

    private lateinit var database: MessageDatabase
    private lateinit var dao: UnreadCounterDao

    @BeforeTest
    fun setup() {
        database = MessageDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        dao = database.getUnreadCounterDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun canInsertAndRetrieveMessagesCounter() = runTest {
        // given
        val input = oneMessagesInboxCounter

        // when
        dao.insertOrUpdate(input)
        val result = dao.observeMessagesUnreadCounters(input.userId)
            .first()

        // then
        assertEquals(listOf(input), result)
    }

    @Test
    fun canInsertAndRetrieveConversationsCounter() = runTest {
        // given
        val input = oneConversationsInboxCounter

        // when
        dao.insertOrUpdate(input)
        val result = dao.observeConversationsUnreadCounters(input.userId)
            .first()

        // then
        assertEquals(listOf(input), result)
    }

    @Test
    fun insertIfDifferentUserId() = runTest {
        // given
        val first = oneMessagesInboxCounter
        val second = twoMessagesInboxCounter

        // when
        dao.insertOrUpdate(first, second)
        val firstResult = dao.observeUnreadCounters(first.userId, first.type)
            .first()
        val secondResult = dao.observeUnreadCounters(second.userId, second.type)
            .first()

        // then
        assertEquals(listOf(first), firstResult)
        assertEquals(listOf(second), secondResult)
    }

    @Test
    fun insertIfDifferentType() = runTest {
        // given
        val first = oneMessagesInboxCounter
        val second = oneConversationsInboxCounter

        // when
        dao.insertOrUpdate(first, second)
        val firstResult = dao.observeUnreadCounters(first.userId, first.type)
            .first()
        val secondResult = dao.observeUnreadCounters(second.userId, second.type)
            .first()

        // then
        assertEquals(listOf(first), firstResult)
        assertEquals(listOf(second), secondResult)
    }

    @Test
    fun insertIfDifferentLabelId() = runTest {
        // given
        val first = oneMessagesInboxCounter
        val second = oneMessagesSendCounter

        // when
        dao.insertOrUpdate(first, second)
        val result = dao.observeUnreadCounters(first.userId, first.type)
            .first()

        // then
        assertEquals(2, result.size)
        assert(first in result)
        assert(second in result)
    }

    @Test
    fun updateIfSameUserIdTypeAndLabelId() = runTest {
        // given
        val first = oneMessagesInboxCounter
        val second = first.copy(unreadCount = 15)

        // when
        dao.insertOrUpdate(first)
        dao.insertOrUpdate(second)
        val result = dao.observeUnreadCounters(first.userId, first.type)
            .first()

        // then
        assertEquals(listOf(second), result)
    }
}
