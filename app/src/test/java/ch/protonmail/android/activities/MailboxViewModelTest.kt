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

package ch.protonmail.android.activities

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.mailbox.presentation.MailboxUiItem
import ch.protonmail.android.mailbox.presentation.MessageData
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.MessageUtils.toContactsAndGroupsString
import com.birbit.android.jobqueue.JobManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class MailboxViewModelTest : CoroutinesTest {

    @RelaxedMockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var jobManager: JobManager

    @RelaxedMockK
    private lateinit var deleteMessage: DeleteMessage

    @RelaxedMockK
    private lateinit var verifyConnection: VerifyConnection

    @RelaxedMockK
    private lateinit var networkConfigurator: NetworkConfigurator

    private lateinit var viewModel: MailboxViewModel

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel = MailboxViewModel(
            messageDetailsRepository,
            userManager,
            jobManager,
            deleteMessage,
            dispatchers,
            verifyConnection,
            networkConfigurator
        )
    }

    @Test
    fun messagesToMailboxMapsAValidMessageToAMailboxItemCorrectly() {
        // Given
        val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
        val messages = listOf(
            Message().apply {
                messageId = "messageId"
                senderDisplayName = "senderName"
                subject = "subject"
                time = 1617205075 // Wednesday, March 31, 2021 5:37:55 PM GMT+02:00 in seconds
                numAttachments = 1
                isStarred = true
                Unread = true
                expirationTime = 82334L
                deleted = false
                allLabelIDs = listOf("label1", "label2")
                toList = recipients
                location = Constants.MessageLocationType.SENT.messageLocationTypeValue
                isReplied = true
                isRepliedAll = false
                isForwarded = false
                isBeingSent = false
                isAttachmentsBeingUploaded = false
                isInline = false
            }
        )
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }

        // When
        val actual = viewModel.messagesToMailboxItems(messages)

        // Then
        val expected = MailboxUiItem(
            itemId = "messageId",
            sender = "senderName",
            subject = "subject",
            timeMs = 1617205075000, // Wednesday, March 31, 2021 5:37:55 PM GMT+02:00 in millis
            hasAttachments = true,
            isStarred = true,
            isRead = false,
            expirationTime = 82334L,
            messagesCount = 0,
            isDeleted = false,
            labelIds = listOf("label1", "label2"),
            recipients = toContactsAndGroupsString(
                recipients
            ),
            messageData = MessageData(
                location = Constants.MessageLocationType.SENT.messageLocationTypeValue,
                isReplied = true,
                isRepliedAll = false,
                isForwarded = false,
                isBeingSent = false,
                isAttachmentsBeingUploaded = false,
                isInline = false
            )
        )
        assertEquals(expected, actual.first())
    }

}
