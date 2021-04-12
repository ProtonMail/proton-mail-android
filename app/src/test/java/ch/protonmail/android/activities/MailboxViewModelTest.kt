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
import androidx.lifecycle.liveData
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.services.MessagesService
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.di.JobEntryPoint
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.jobs.FetchByLocationJob
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.GetConversations
import ch.protonmail.android.mailbox.domain.GetConversationsResult
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.mailbox.presentation.MailboxViewModel
import ch.protonmail.android.mailbox.presentation.model.MailboxUiItem
import ch.protonmail.android.mailbox.presentation.model.MessageData
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.MessageUtils.toContactsAndGroupsString
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.EntryPoints
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.After
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val STARRED_LABEL_ID = "10"

class MailboxViewModelTest : CoroutinesTest {

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

    @RelaxedMockK
    private lateinit var messageServiceScheduler: MessagesService.Scheduler

    @RelaxedMockK
    private lateinit var conversationModeEnabled: ConversationModeEnabled

    @RelaxedMockK
    private lateinit var getConversations: GetConversations

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
            networkConfigurator,
            messageServiceScheduler,
            conversationModeEnabled,
            getConversations
        )

        val jobEntryPoint = mockk<JobEntryPoint>()
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), JobEntryPoint::class.java) } returns jobEntryPoint
        every { jobEntryPoint.userManager() } returns mockk(relaxed = true)
        coEvery { contactsRepository.findAllContactEmails() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        unmockkStatic(EntryPoints::class)
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
        every { conversationModeEnabled(any()) } returns false
        every { messageDetailsRepository.getMessagesByLocationAsync(any()) } returns liveData { emit(messages) }

        // When
        val actual = viewModel.getMailboxItems(
            Constants.MessageLocationType.INBOX,
            "",
            false,
            "",
            false
        ).testObserver()

        // Then
        val expected = MailboxUiItem(
            itemId = "messageId",
            senderName = "senderName9238",
            subject = "subject",
            lastMessageTimeMs = 0,
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
        every { conversationModeEnabled(any()) } returns false
        every { messageDetailsRepository.getMessagesByLocationAsync(any()) } returns liveData { emit(messages) }

        // When
        val actual = viewModel.getMailboxItems(
            Constants.MessageLocationType.INBOX,
            "",
            false,
            "",
            false
        ).testObserver()

        // Then
        val expected = MailboxUiItem(
            itemId = "messageId",
            senderName = contactName,
            subject = "subject",
            lastMessageTimeMs = 0,
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
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }
        every { conversationModeEnabled(any()) } returns false
        every { messageDetailsRepository.getMessagesByLocationAsync(any()) } returns liveData { emit(messages) }

        // When
        val actual = viewModel.getMailboxItems(
            Constants.MessageLocationType.INBOX,
            "",
            false,
            "",
            false
        ).testObserver()

        // Then
        val expected = MailboxUiItem(
            itemId = "messageId",
            senderName = "anySenderEmail@protonmail.ch",
            subject = "subject",
            lastMessageTimeMs = 0,
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
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }
        every { conversationModeEnabled(any()) } returns false
        every { messageDetailsRepository.getMessagesByLocationAsync(any()) } returns liveData { emit(messages) }

        // When
        val actual = viewModel.getMailboxItems(
            Constants.MessageLocationType.INBOX,
            "",
            false,
            "",
            false
        ).testObserver()

        // Then
        val expected = MailboxUiItem(
            itemId = "messageId",
            senderName = "anySenderEmail8437@protonmail.ch",
            subject = "subject",
            lastMessageTimeMs = 0,
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
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }
        every { conversationModeEnabled(any()) } returns false
        every { messageDetailsRepository.getMessagesByLocationAsync(any()) } returns liveData { emit(messages) }

        // When
        val actual = viewModel.getMailboxItems(
            Constants.MessageLocationType.INBOX,
            "",
            false,
            "",
            false
        ).testObserver()

        // Then
        val expected = MailboxUiItem(
            itemId = "messageId",
            senderName = "senderName",
            subject = "subject",
            lastMessageTimeMs = 1617205075000, // Wednesday, March 31, 2021 5:37:55 PM GMT+02:00 in millis
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
    fun getMailboxItemsCallsFetchByLocationForwardingTheGivenParameters() {
        val location = Constants.MessageLocationType.SENT
        val labelId = "labelId923842"
        val includeLabels = true
        val uuid = "9238423bbe2h3423489wssdf"
        val refreshMessages = false

        viewModel.getMailboxItems(
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

    @Test
    fun getMailboxItemsReturnsMailboxItemsLiveDataMappedFromMessageDetailsRepositoryWhenFetchingFirstPage() {
        val message = Message(
            messageId = "messageId9238482",
            sender = MessageSender("senderName", "sender@pm.me"),
            subject = "subject1283"
        )
        coEvery { messageDetailsRepository.getAllMessages() } returns liveData { emit(listOf(message)) }

        val actual = viewModel.getMailboxItems(
            Constants.MessageLocationType.ALL_MAIL,
            "labelId923842",
            true,
            "9238423bbe2h3423489wssdf",
            false
        ).testObserver()

        val expected = listOf(
            fakeMailboxUiData("messageId9238482", "senderName", "subject1283")
        )
        assertEquals(expected, actual.observedValues.first())
    }

    @Test
    fun getMailboxItemsCallsMessageServiceStartFetchMessagesWhenTheRequestIsAboutLoadingPagesGreaterThanTheFirstAndLocationIsNotALabelOrFolder() {
        val location = Constants.MessageLocationType.ARCHIVE
        val labelId = "labelId92323"
        val includeLabels = false
        val uuid = "9238423bbe2h3283742h3hh2bjsd"
        val refreshMessages = true
        // Represents pagination. Only messages older than the given timestamp will be returned
        val timestamp = 123L
        val userId = Id("userId")
        every { userManager.requireCurrentUserId() } returns userId

        viewModel.getMailboxItems(
            location,
            labelId,
            includeLabels,
            uuid,
            refreshMessages,
            timestamp
        )

        verifySequence { messageServiceScheduler.fetchMessagesOlderThanTime(location, userId, timestamp) }
        verify(exactly = 0) { jobManager.addJobInBackground(any()) }
    }

    @Test
    fun getMailboxItemsCallsMessageServiceStartFetchMessagesByLabelWhenTheRequestIsAboutLoadingPagesGreaterThanTheFirstAndLocationIsALabelOrFolder() {
        val location = Constants.MessageLocationType.LABEL_FOLDER
        val labelId = "folderIdi2384"
        val includeLabels = false
        val uuid = "9238h82388sdfa8sdf8asd3hh2bjsd"
        val refreshMessages = false
        // Represents pagination. Only messages older than the given timestamp will be returned
        val earliestTime = 1323L
        val userId = Id("userId1")
        every { userManager.requireCurrentUserId() } returns userId

        viewModel.getMailboxItems(
            location,
            labelId,
            includeLabels,
            uuid,
            refreshMessages,
            earliestTime
        )

        verifySequence {
            messageServiceScheduler.fetchMessagesOlderThanTimeByLabel(
                location, userId, earliestTime, labelId
            )
        }
    }

    @Test
    fun getMailboxItemsReturnsMailboxItemsLiveDataMappedFromMessageDetailsRepositoryWhenFetchingSubsequentPages() {
        val message = Message(
            messageId = "messageId92384823",
            sender = MessageSender("senderName1", "sender@pm.me"),
            subject = "subject12834"
        )
        val location = Constants.MessageLocationType.LABEL_FOLDER
        val labelId = "folderIdi2384"
        val includeLabels = false
        val uuid = "9238h82388sdfa8sdf8asd3hh2283"
        val refreshMessages = false
        // Represents pagination. Only messages older than the given timestamp will be returned
        val earliestTime = 1323L
        coEvery { messageDetailsRepository.getMessagesByLabelIdAsync(labelId) } returns liveData {
            emit(listOf(message))
        }

        val actual = viewModel.getMailboxItems(
            location,
            labelId,
            includeLabels,
            uuid,
            refreshMessages,
            earliestTime
        ).testObserver()

        val expected = listOf(
            fakeMailboxUiData("messageId92384823", "senderName1", "subject12834")
        )
        assertEquals(expected, actual.observedValues.first())
    }

    @Test
    fun getMailboxItemsCallsGetConversationsUseCaseWhenConversationModeIsEnabled() = runBlockingTest {
        val location = Constants.MessageLocationType.INBOX
        val labelId = "labelId923842"
        val includeLabels = true
        val uuid = "9238423bbe2h3423489wssdf"
        val refreshMessages = false
        every { conversationModeEnabled(location) } returns true

        viewModel.getMailboxItems(
            location,
            labelId,
            includeLabels,
            uuid,
            refreshMessages,
            123L
        )

        coVerifySequence { getConversations(location) }
        verify { messageServiceScheduler wasNot Called }
    }

    @Test
    fun getMailboxItemsReturnsMailboxItemsMappedFromConversationsWhenGetConversationsUseCaseSucceeds() =
        runBlockingTest {
            val location = Constants.MessageLocationType.INBOX
            val senders = listOf(
                Correspondent("firstSender", "firstsender@protonmail.com")
            )
            val conversation = Conversation(
                "conversationId124",
                "subject2345",
                senders,
                emptyList(),
                4,
                0,
                2,
                823764623,
                "senderAddressId",
                listOf(),
                null,
                0
            )
            val successResult = GetConversationsResult.Success(listOf(conversation))
            every { conversationModeEnabled(location) } returns true
            coEvery { getConversations(location) } returns flowOf(successResult)

            val actual = viewModel.getMailboxItems(
                location,
                "labelId923842",
                true,
                "9238423bbe2h3423489wssdf",
                false,
            ).testObserver()

            val expected = listOf(
                MailboxUiItem(
                    "conversationId124",
                    "firstSender",
                    "subject2345",
                    lastMessageTimeMs = 0,
                    hasAttachments = true,
                    isStarred = false,
                    isRead = true,
                    expirationTime = 823764623,
                    messagesCount = 4,
                    messageData = null,
                    isDeleted = false,
                    labelIds = emptyList(),
                    recipients = ""
                )
            )
            assertEquals(expected, actual.observedValues.first())
        }

    @Test
    fun getMailboxItemsMapsConversationsSendersUsingContactNameOrSenderNameOrEmailInThisPreferenceOrder() =
        runBlockingTest {
            val location = Constants.MessageLocationType.INBOX
            val senders = listOf(
                Correspondent("firstSender", "firstsender@protonmail.com"),
                Correspondent("secondSender", "anotherSender@protonmail.com"),
                Correspondent("", "thirdsender@pm.me"),
            )
            val recipients = listOf(
                Correspondent("recipient", "recipient@protonmail.com"),
                Correspondent("recipient1", "recipient1@pm.ch")
            )
            val conversation = Conversation(
                "conversationId",
                "subject",
                senders,
                recipients,
                2,
                1,
                2,
                123423423,
                "senderAddressId",
                listOf(),
                null,
                0
            )
            val successResult = GetConversationsResult.Success(listOf(conversation))
            every { conversationModeEnabled(location) } returns true
            coEvery { getConversations(location) } returns flowOf(successResult)
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )

            val actual = viewModel.getMailboxItems(
                location,
                "labelId923842",
                true,
                "9238423bbe2h3423489wssdf",
                false,
            ).testObserver()

            val expected = listOf(
                MailboxUiItem(
                    "conversationId",
                    "firstContactName, secondSender, thirdsender@pm.me",
                    "subject",
                    lastMessageTimeMs = 0,
                    hasAttachments = true,
                    isStarred = false,
                    isRead = false,
                    expirationTime = 123423423,
                    messagesCount = 2,
                    messageData = null,
                    isDeleted = false,
                    labelIds = emptyList(),
                    recipients = "recipient, recipient1"
                )
            )
            assertEquals(expected, actual.observedValues.first())
        }

    @Test
    fun getMailboxItemsMapsConversationAsStarredIfLabelIdsContainsStarredLabelId() =
        runBlockingTest {
            val location = Constants.MessageLocationType.INBOX
            val conversation = Conversation(
                "conversationId9238",
                "subject9237472",
                emptyList(),
                emptyList(),
                2,
                1,
                0,
                0,
                "senderAddressId",
                listOf(STARRED_LABEL_ID, "randomLabelId"),
                null,
                1617982191
            )
            val successResult = GetConversationsResult.Success(listOf(conversation))
            every { conversationModeEnabled(location) } returns true
            coEvery { getConversations(location) } returns flowOf(successResult)
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )

            val actual = viewModel.getMailboxItems(
                location,
                "labelId923842",
                true,
                "9238423bbe2h3423489wssdf",
                false,
            ).testObserver()

            val expected = listOf(
                MailboxUiItem(
                    "conversationId9238",
                    "",
                    "subject9237472",
                    lastMessageTimeMs = 1617982191,
                    hasAttachments = false,
                    isStarred = true,
                    isRead = false,
                    expirationTime = 0,
                    messagesCount = 2,
                    messageData = null,
                    isDeleted = false,
                    labelIds = listOf(STARRED_LABEL_ID, "randomLabelId"),
                    recipients = ""
                )
            )
            assertEquals(expected, actual.observedValues.first())
        }

   
    private fun fakeMailboxUiData(
        itemId: String,
        senderName: String,
        subject: String
    ) = MailboxUiItem(
        itemId,
        senderName,
        subject,
        0,
        hasAttachments = false,
        isStarred = false,
        isRead = true,
        expirationTime = 0,
        messagesCount = 0,
        messageData = MessageData(
            Constants.MessageLocationType.INVALID.messageLocationTypeValue,
            isReplied = false,
            isRepliedAll = false,
            isForwarded = false,
            isInline = false
        ),
        isDeleted = false,
        labelIds = emptyList(),
        recipients = ""
    )

}
