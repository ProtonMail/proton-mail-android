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

package ch.protonmail.android.mailbox.data

import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.repository.MessageRepository
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import org.junit.Test

class MarkUnreadLatestNonDraftMessageInLocationTest {

    private val messageRepositoryMock: MessageRepository = mockk {
        coEvery { saveMessage(TestData.USER_ID, any()) } returns Message()
    }
    private val markUnreadLatestNonDraftMessageInLocation = MarkUnreadLatestNonDraftMessageInLocation(
        messageRepositoryMock
    )

    @Test
    fun shouldMarkUnreadOnlyTheLatestMessageMeetingTheCriteria() = runBlockingTest {
        // given
        val expectedMessageMarkedUnread = TestData.Messages.listWithAllButFirstMeetingTheCriteria[1]
            .copy(Unread = true)

        // when
        markUnreadLatestNonDraftMessageInLocation(
            messagesSortedByNewest = TestData.Messages.listWithAllButFirstMeetingTheCriteria,
            locationId = TestData.CURRENT_LOCATION_ID,
            userId = TestData.USER_ID
        )

        // then
        coVerify {
            messageRepositoryMock.saveMessage(TestData.USER_ID, expectedMessageMarkedUnread)
        }
        confirmVerified(messageRepositoryMock)
    }

    @Test
    fun shouldNotMarkUnreadIfMessagesDoNotMeetTheCriteria() = runBlockingTest {
        // when
        markUnreadLatestNonDraftMessageInLocation(
            messagesSortedByNewest = TestData.Messages.listWithNoneMeetingTheCriteria,
            locationId = TestData.CURRENT_LOCATION_ID,
            userId = TestData.USER_ID
        )

        // then
        coVerify { messageRepositoryMock wasNot called }
    }
}

private object TestData {
    val USER_ID = UserId("user id")
    const val CURRENT_LOCATION_ID = "location id"

    object Messages {
        private val messageMeetingTheCriteria = Message(
            messageId = "messageMeetingTheCriteria",
            Unread = false,
            location = Constants.MessageLocationType.INBOX.messageLocationTypeValue,
            allLabelIDs = listOf(Constants.MessageLocationType.INBOX.asLabelIdString(), CURRENT_LOCATION_ID)
        )
        private val messageNotRead = messageMeetingTheCriteria.copy(
            messageId = "messageNotRead",
            Unread = true
        )
        private val messageDraft = messageMeetingTheCriteria.copy(
            messageId = "messageDraft",
            allLabelIDs = listOf(Constants.MessageLocationType.DRAFT.asLabelIdString())
        )
        private val messageInWrongLocation = messageMeetingTheCriteria.copy(
            messageId = "messageInWrongLocation",
            allLabelIDs =  listOf(Constants.MessageLocationType.INBOX.asLabelIdString())
        )
        private val messageNotReadAndDraft = messageNotRead.copy(
            messageId = "messageNotReadAndDraft",
            location = Constants.MessageLocationType.DRAFT.messageLocationTypeValue
        )
        private val messageNotReadAndInWrongLocation = messageNotRead.copy(
            messageId = "messageNotReadAndInWrongLocation",
            allLabelIDs =  listOf(Constants.MessageLocationType.INBOX.asLabelIdString())
        )
        private val messageDraftAndInWrongLocation = messageDraft.copy(
            messageId = "messageDraftAndInWrongLocation",
            allLabelIDs = listOf(Constants.MessageLocationType.DRAFT.asLabelIdString())
        )
        private val messageNotReadDraftAndInWrongLocation = messageNotReadAndDraft.copy(
            messageId = "messageNotReadDraftAndInWrongLocation",
            allLabelIDs = listOf(Constants.MessageLocationType.DRAFT.asLabelIdString())
        )

        val listWithAllButFirstMeetingTheCriteria = listOf(messageNotReadDraftAndInWrongLocation) +
            (1..9).map { index -> messageMeetingTheCriteria.copy(messageId = index.toString()) }
        val listWithNoneMeetingTheCriteria = listOf(
            messageNotRead,
            messageDraft,
            messageInWrongLocation,
            messageNotReadAndDraft,
            messageNotReadAndInWrongLocation,
            messageDraftAndInWrongLocation,
            messageNotReadDraftAndInWrongLocation
        )
    }
}
