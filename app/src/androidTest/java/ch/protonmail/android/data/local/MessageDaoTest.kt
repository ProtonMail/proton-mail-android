/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import ch.protonmail.android.data.local.model.Message
import me.proton.core.test.android.runBlockingWithTimeout
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
        runBlockingWithTimeout {
            dao.saveMessages(allMessages)
        }
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun observe_messages_ignores_unread_if_null() = runBlockingWithTimeout {
        // given
        val expected = allMessages

        // when
        dao.observeMessages(INBOX_LABEL, unread = null).test {
            // then
            assertEquals(expected.ids(), awaitItem().ids())
        }
    }

    @Test
    fun observe_messages_filter_unread_only() = runBlockingWithTimeout {
        // given
        val expected = allMessages.filter { it.Unread }
        // when
        dao.observeMessages(INBOX_LABEL, unread = true).test {
            // then
            assertEquals(expected.ids(), awaitItem().ids())
        }
    }

    @Test
    fun observe_messages_filter_read_only() = runBlockingWithTimeout {
        // given
        val expected = allMessages.filterNot { it.Unread }
        // when
        dao.observeMessages(INBOX_LABEL, unread = false).test {
            // then
            assertEquals(expected.ids(), awaitItem().ids())
        }
    }

    companion object TestData {

        const val INBOX_LABEL = "inbox"

        val allMessages = listOf(
            buildMessage("first", labelsIds = listOf(INBOX_LABEL, "ab"), unread = true),
            buildMessage("second", labelsIds = listOf(INBOX_LABEL, "ab"), unread = false)
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
