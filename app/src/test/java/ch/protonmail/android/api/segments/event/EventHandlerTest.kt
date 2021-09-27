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

package ch.protonmail.android.api.segments.event

import android.content.Context
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.event.data.remote.model.EventResponse
import ch.protonmail.android.mailbox.data.local.UnreadCounterDao
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterEntity
import ch.protonmail.android.mailbox.data.mapper.ApiToDatabaseUnreadCounterMapper
import ch.protonmail.android.mailbox.data.remote.model.CountsApiModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestCoroutineScope
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

class EventHandlerTest {

    private val context: Context = mockk {
        every { applicationContext } returns this
    }
    private val unreadCounterDao: UnreadCounterDao = mockk(relaxUnitFun = true)
    private val apiToDatabaseUnreadCounterMapper = ApiToDatabaseUnreadCounterMapper()
    private val userManager: UserManager = mockk {
        coEvery { getLegacyUser(any()) } returns mockk()
    }
    private val messageDetailsRepositoryFactory = object : MessageDetailsRepository.AssistedFactory {
        override fun create(userId: UserId): MessageDetailsRepository = mockk(relaxUnitFun = true)
    }
    private val databaseProvider: DatabaseProvider = mockk {
        every { provideContactDao(any()) } returns mockk()
        every { provideMessageDao(any()) } returns mockk()
        every { providePendingActionDao(any()) } returns mockk()
    }

    private val eventHandler = EventHandler(
        context = context,
        protonMailApiManager = mockk(),
        unreadCounterDao = unreadCounterDao,
        apiToDatabaseUnreadCounterMapper = apiToDatabaseUnreadCounterMapper,
        userManager = userManager,
        messageDetailsRepositoryFactory = messageDetailsRepositoryFactory,
        changeToConversations = mockk(),
        fetchContactEmails = mockk(),
        fetchContactsData = mockk(),
        fetchUserWorkerEnqueuer = mockk(),
        fetchUserAddressesWorkerEnqueuer = mockk(),
        fetchMailSettingsWorker = mockk(),
        databaseProvider = databaseProvider,
        launchInitialDataFetch = mockk(),
        messageFactory = mockk(),
        userId = testUserId(),
        externalScope = TestCoroutineScope(),
        messageFlagsToEncryptionMapper = mockk(),
        labelRepository = mockk()
    )

    @Test
    fun writeMessagesCountsToDatabase() {
        // given
        val countsDatabaseModels = apiToDatabaseUnreadCounterMapper.toDatabaseModels(
            countsApiModels(),
            testUserId(),
            UnreadCounterEntity.Type.MESSAGES
        )
        val eventResponse: EventResponse = mockEventResponse {
            every { messageCounts } returns countsApiModels()
        }

        // when
        eventHandler.write(eventResponse)

        // then
        coVerify { unreadCounterDao.insertOrUpdate(countsDatabaseModels) }
    }

    @Test
    fun writeConversationsCountsToDatabase() {
        // given
        val countsDatabaseModels = apiToDatabaseUnreadCounterMapper.toDatabaseModels(
            countsApiModels(),
            testUserId(),
            UnreadCounterEntity.Type.CONVERSATIONS
        )
        val eventResponse: EventResponse = mockEventResponse {
            every { conversationCounts } returns countsApiModels()
        }

        // when
        eventHandler.write(eventResponse)

        // then
        coVerify { unreadCounterDao.insertOrUpdate(countsDatabaseModels) }
    }

    private fun testUserId() = UserId("user1")
    private fun countsApiModels() = listOf(
        CountsApiModel("Inbox", 5, 10),
        CountsApiModel("Sent", 3, 7),
        CountsApiModel("Draft", 0, 2),
    )

    private fun mockEventResponse(block: EventResponse.() -> Unit = {}): EventResponse = mockk {
        every { addresses } returns null
        every { contactEmailsUpdates } returns null
        every { contactUpdates } returns null
        every { conversationCounts } returns null
        every { conversationUpdates } returns null
        every { labelUpdates } returns null
        every { mailSettingsUpdates } returns null
        every { messageCounts } returns null
        every { messageUpdates } returns null
        every { userUpdates } returns null
        every { usedSpace } returns 0
        block()
    }
}
