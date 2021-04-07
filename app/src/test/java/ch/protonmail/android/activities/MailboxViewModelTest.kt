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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.di.JobEntryPoint
import ch.protonmail.android.jobs.FetchByLocationJob
import ch.protonmail.android.mailbox.presentation.MailboxUiItem
import ch.protonmail.android.mailbox.presentation.MailboxViewModel
import ch.protonmail.android.mailbox.presentation.MessageData
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.testAndroid.rx.TrampolineScheduler
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.MessageUtils.toContactsAndGroupsString
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.EntryPoints
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class MailboxViewModelTest : CoroutinesTest {

    @get:Rule
    val trampolineSchedulerRule = TrampolineScheduler()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var contactsRepository: ContactsRepository

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
            contactsRepository,
            verifyConnection,
            networkConfigurator
        )
    }

    @Test
    fun messagesToMailboxMapsSenderNameToMessageSenderNameWhenSenderEmailDoesNotExistInContacts() {
        // Given
        val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
        val messages = listOf(
            Message().apply {
                messageId = "messageId"
                sender = MessageSender("senderName9238", "anySenderEmail@pm.me")
                subject = "subject"
            }
        )
        coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
            listOf(ContactEmail("contactId", "anotherContact@pm.me", "anotherContactName"))
        )
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }

        // When
        val actual = viewModel.messagesToMailboxItems(messages).testObserver()

        // Then
        val expected = MailboxUiItem(
            itemId = "messageId",
            senderName = "senderName9238",
            subject = "subject",
            timeMs = 0,
            hasAttachments = false,
            isStarred = false,
            isRead = true,
            expirationTime = 0,
            messagesCount = 0,
            isDeleted = false,
            labelIds = emptyList(),
            recipients = "",
            messageData = MessageData(
                location = Constants.MessageLocationType.INVALID.messageLocationTypeValue,
                isReplied = false,
                isRepliedAll = false,
                isForwarded = false,
                isInline = false
            )
        )
        val actualMailboxItems = actual.observedValues.first()
        assertEquals(expected, actualMailboxItems?.first())
    }

    @Test
    fun messagesToMailboxMapsSenderNameToContactNameWhenSenderEmailExistsInContactsList() {
        // Given
        val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
        val contactName = "contactNameTest"
        val senderEmailAddress = "sender@email.pm"
        val messages = listOf(
            Message().apply {
                messageId = "messageId"
                sender = MessageSender("anySenderName", senderEmailAddress)
                subject = "subject"
            }
        )
        coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
            listOf(ContactEmail("contactId", senderEmailAddress, contactName))
        )
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }

        // When
        val actual = viewModel.messagesToMailboxItems(messages).testObserver()

        // Then
        val expected = MailboxUiItem(
            itemId = "messageId",
            senderName = contactName,
            subject = "subject",
            timeMs = 0,
            hasAttachments = false,
            isStarred = false,
            isRead = true,
            expirationTime = 0,
            messagesCount = 0,
            isDeleted = false,
            labelIds = emptyList(),
            recipients = "",
            messageData = MessageData(
                location = Constants.MessageLocationType.INVALID.messageLocationTypeValue,
                isReplied = false,
                isRepliedAll = false,
                isForwarded = false,
                isInline = false
            )
        )
        val actualMailboxItems = actual.observedValues.first()
        assertEquals(expected, actualMailboxItems?.first())
        coVerify { contactsRepository.findAllContactEmails() }
    }

    @Test
    fun messagesToMailboxMapsSenderNameToMessageSenderEmailWhenSenderEmailDoesNotExistInContactsAndSenderNameIsNull() {
        // Given
        val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
        val messages = listOf(
            Message().apply {
                messageId = "messageId"
                sender = MessageSender(null, "anySenderEmail@protonmail.ch")
                subject = "subject"
            }
        )
        coEvery { contactsRepository.findAllContactEmails() } returns flowOf(emptyList())
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }

        // When
        val actual = viewModel.messagesToMailboxItems(messages).testObserver()

        // Then
        val expected = MailboxUiItem(
            itemId = "messageId",
            senderName = "anySenderEmail@protonmail.ch",
            subject = "subject",
            timeMs = 0,
            hasAttachments = false,
            isStarred = false,
            isRead = true,
            expirationTime = 0,
            messagesCount = 0,
            isDeleted = false,
            labelIds = emptyList(),
            recipients = "",
            messageData = MessageData(
                location = Constants.MessageLocationType.INVALID.messageLocationTypeValue,
                isReplied = false,
                isRepliedAll = false,
                isForwarded = false,
                isInline = false
            )
        )
        val actualMailboxItems = actual.observedValues.first()
        assertEquals(expected, actualMailboxItems?.first())
    }

    @Test
    fun messagesToMailboxMapsSenderNameToMessageSenderEmailWhenSenderEmailDoesNotExistInContactsAndSenderNameIsEmpty() {
        // Given
        val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
        val messages = listOf(
            Message().apply {
                messageId = "messageId"
                sender = MessageSender("", "anySenderEmail8437@protonmail.ch")
                subject = "subject"
            }
        )
        coEvery { contactsRepository.findAllContactEmails() } returns flowOf(emptyList())
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }

        // When
        val actual = viewModel.messagesToMailboxItems(messages).testObserver()

        // Then
        val expected = MailboxUiItem(
            itemId = "messageId",
            senderName = "anySenderEmail8437@protonmail.ch",
            subject = "subject",
            timeMs = 0,
            hasAttachments = false,
            isStarred = false,
            isRead = true,
            expirationTime = 0,
            messagesCount = 0,
            isDeleted = false,
            labelIds = emptyList(),
            recipients = "",
            messageData = MessageData(
                location = Constants.MessageLocationType.INVALID.messageLocationTypeValue,
                isReplied = false,
                isRepliedAll = false,
                isForwarded = false,
                isInline = false
            )
        )
        val actualMailboxItems = actual.observedValues.first()
        assertEquals(expected, actualMailboxItems?.first())
    }

    @Test
    fun messagesToMailboxMapsAllFieldsOfMailboxUiItemFromMessageCorrectly() {
        // Given
        val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
        val messages = listOf(
            Message().apply {
                messageId = "messageId"
                sender = MessageSender("senderName", "senderEmail@pm.ch")
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
                isInline = false
            }
        )
        coEvery { contactsRepository.findAllContactEmails() } returns flowOf(emptyList())
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }

        // When
        val actual = viewModel.messagesToMailboxItems(messages).testObserver()

        // Then
        val expected = MailboxUiItem(
            itemId = "messageId",
            senderName = "senderName",
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
                isInline = false
            )
        )
        val actualMailboxItems = actual.observedValues.first()
        assertEquals(expected, actualMailboxItems?.first())
    }

    @Test
    fun fetchMessagesCallsFetchByLocationForwardingTheGivenParameters() {
        val location = Constants.MessageLocationType.SENT
        val labelId = "labelId923842"
        val includeLabels = true
        val uuid = "9238423bbe2h3423489wssdf"
        val refreshMessages = false
        val jobEntryPoint = mockk<JobEntryPoint>()
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), JobEntryPoint::class.java) } returns jobEntryPoint
        every { jobEntryPoint.userManager() } returns mockk(relaxed = true)

        viewModel.fetchMessages(
            location,
            labelId,
            includeLabels,
            uuid,
            refreshMessages
        )

        val actual = slot<FetchByLocationJob>()
        verify { jobManager.addJobInBackground(capture(actual)) }
        assertEquals(location, actual.captured.location)
        assertEquals(labelId, actual.captured.labelId)
        assertEquals(includeLabels, actual.captured.includeLabels)
        assertEquals(uuid, actual.captured.uuid)
        assertEquals(false, actual.captured.refreshMessages)
        unmockkStatic(EntryPoints::class)
    }
}
