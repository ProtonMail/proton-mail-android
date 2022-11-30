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

package ch.protonmail.android.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import ch.protonmail.android.data.local.model.Message
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MessageDaoTest {

    private lateinit var database: MessageDatabase
    private lateinit var dao: MessageDao

    @BeforeTest
    fun setup() {
        database = MessageDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        dao = database.getMessageDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun observe_messages_find_messages_with_only_one_label() = runTest {
        // given
        val message = buildMessage(FIRST_MESSAGE_ID, labelsIds = listOf(INBOX_LABEL))
        dao.insertOrUpdate(message)
        val expected = listOf(message)

        // when
        dao.observeMessages(INBOX_LABEL, unread = null).test {
            // then
            assertEquals(expected.ids(), awaitItem().ids())
        }
    }

    @Test
    fun observe_messages_find_messages_with_label_at_the_start_of_the_list() = runTest {
        // given
        val message = buildMessage(FIRST_MESSAGE_ID, labelsIds = listOf(INBOX_LABEL, ARCHIVE_LABEL))
        dao.insertOrUpdate(message)
        val expected = listOf(message)

        // when
        dao.observeMessages(INBOX_LABEL, unread = null).test {
            // then
            assertEquals(expected.ids(), awaitItem().ids())
        }
    }

    @Test
    fun observe_messages_find_messages_with_label_at_the_end_of_the_list() = runTest {
        // given
        val message = buildMessage(FIRST_MESSAGE_ID, labelsIds = listOf(ARCHIVE_LABEL, INBOX_LABEL))
        dao.insertOrUpdate(message)
        val expected = listOf(message)

        // when
        dao.observeMessages(INBOX_LABEL, unread = null).test {
            // then
            assertEquals(expected.ids(), awaitItem().ids())
        }
    }

    @Test
    fun observe_messages_find_messages_with_label_in_the_middle_of_the_list() = runTest {
        // given
        val message = buildMessage(FIRST_MESSAGE_ID, labelsIds = listOf(ARCHIVE_LABEL, INBOX_LABEL, SENT_LABEL))
        dao.insertOrUpdate(message)
        val expected = listOf(message)

        // when
        dao.observeMessages(INBOX_LABEL, unread = null).test {
            // then
            assertEquals(expected.ids(), awaitItem().ids())
        }
    }

    @Test
    fun observe_messages_ignores_unread_if_null() = runTest {
        // given
        dao.saveMessages(allMessages)
        val expected = allMessages

        // when
        dao.observeMessages(INBOX_LABEL, unread = null).test {
            // then
            assertEquals(expected.ids(), awaitItem().ids())
        }
    }

    @Test
    fun observe_messages_filter_unread_only() = runTest {
        // given
        dao.saveMessages(allMessages)
        val expected = allMessages.filter { it.Unread }

        // when
        dao.observeMessages(INBOX_LABEL, unread = true).test {
            // then
            assertEquals(expected.ids(), awaitItem().ids())
        }
    }

    @Test
    fun observe_messages_filter_read_only() = runTest {
        // given
        dao.saveMessages(allMessages)
        val expected = allMessages.filterNot { it.Unread }

        // when
        dao.observeMessages(INBOX_LABEL, unread = false).test {
            // then
            assertEquals(expected.ids(), awaitItem().ids())
        }
    }

    companion object TestData {

        const val FIRST_MESSAGE_ID = "first"
        const val SECOND_MESSAGE_ID = "second"
        const val INBOX_LABEL = "inbox"
        const val ARCHIVE_LABEL = "archive"
        const val SENT_LABEL = "sent"

        val allMessages = listOf(
            buildMessage(FIRST_MESSAGE_ID, labelsIds = listOf(INBOX_LABEL), unread = true),
            buildMessage(SECOND_MESSAGE_ID, labelsIds = listOf(INBOX_LABEL), unread = false)
        )

        private fun buildMessage(
            id: String,
            labelsIds: List<String> = emptyList(),
            unread: Boolean = true
        ) = Message().apply {
            messageId = id
            allLabelIDs = labelsIds
            Unread = unread
        }

        private fun Collection<Message>.ids() = map { it.messageId }
    }
}
